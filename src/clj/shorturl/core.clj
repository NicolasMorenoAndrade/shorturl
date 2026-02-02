(ns shorturl.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.middleware.session :refer [wrap-session]]
            [shorturl.db :as db]
            [shorturl.slug :refer [generate-slug sanitize-custom-slug sanitize-url]]
            [shorturl.migrations :as migrations]
            [clojure.java.io :as io])
  (:gen-class))

(defn verify-user
  "Verifies a Firebase-authenticated user and ensures they exist in the database.
   Also sets the user's firebase_uid in the session for subsequent requests.

   When a user authenticates with Firebase, this endpoint:
   1. Checks if they exist in the database by Firebase UID
   2. If not, creates a new user record with their Firebase profile data
   3. If they exist, updates their last login timestamp
   4. Stores firebase_uid in the session

   Parameters (in request body):
   - firebase-uid: The Firebase Authentication UID
   - email: User's email address
   - display-name: User's display name

   Returns:
   - The user data with 200 status if successful
   - Error with appropriate status code on failure"
  [req]
  (let [firebase-uid (get-in req [:body-params :firebase-uid])
        email (get-in req [:body-params :email])
        display-name (get-in req [:body-params :display-name])]
    (try
      (if (and firebase-uid email)
        (let [existing-user (db/get-user-by-firebase-uid firebase-uid)]
          (if existing-user
            ;; User exists - update last login
            (do (db/update-user-last-login! firebase-uid)
                ;; Set session and return user data
                (-> (r/response existing-user)
                    (assoc :session {:firebase-uid firebase-uid})))
            ;; New user - create record
            (do (db/create-user! firebase-uid email display-name)
                ;; Set session and return user data
                (-> (r/response {:firebase_uid firebase-uid
                                 :email email
                                 :display_name display-name})
                    (assoc :session {:firebase-uid firebase-uid})))))
        ;; Missing required fields
        (r/status (r/response {:error "Firebase UID and email are required"}) 400))
      (catch Exception e
        (r/status (r/response {:error (.getMessage e)}) 500)))))

(defn redirect
  "Handles redirection requests for shortened URLs.
   Looks up the slug in the database and redirects to the original URL if found.
   Returns a 404 if the slug doesn't exist or 400 if no slug is provided."
  [req]
  (if-let [slug (get-in req [:path-params :slug])]
    (try
      (if-let [url (db/get-url slug)]
        (r/redirect url 307)
        (r/status (r/response {:error "URL not found"}) 404))
      (catch Exception e
        (r/status (r/response {:error (.getMessage e)}) 500)))
    (r/status (r/response {:error "Slug is required"}) 400)))

(defn create-redirect
  [req]
  (let [url (get-in req [:body-params :url])
        custom-slug (get-in req [:body-params :slug])
        sanitized-url (when url (sanitize-url url))
        sanitized-slug (when custom-slug (sanitize-custom-slug custom-slug))
        firebase-uid (get-in req [:session :firebase-uid])
        user-id (when firebase-uid (db/get-user-id-by-firebase-uid firebase-uid))
        anon-count (get-in req [:session :anon-url-count] 0)
        anon-limit-reached? (and (nil? firebase-uid) (>= anon-count 1))]

    ;; (println "=== DEBUG ===")
    ;; (println "Session:" (:session req))
    ;; (println "Firebase UID:" firebase-uid)
    ;; (println "Anon count:" anon-count)
    ;; (println "Limit reached?" anon-limit-reached?)

    (cond
      anon-limit-reached?
      (r/status (r/response
                 {:error "Anonymous users limited to 1 URL. Please sign in for unlimited URLs."}) 403)

      (nil? url)
      (r/status (r/response {:error "URL is required"}) 400)

      (empty? url)
      (r/status (r/response {:error "URL cannot be empty"}) 400)

      (nil? sanitized-url)
      (r/status (r/response {:error "Invalid URL format or potentially unsafe URL"}) 400)

      (and custom-slug (nil? sanitized-slug))
      (r/status (r/response
                 {:error "slug must be between 6 and 20 characters..."}) 400)

      ;; Custom slug - UPDATE SESSION HERE TOO
      sanitized-slug
      (try
        (db/insert-url-redirection! sanitized-url sanitized-slug user-id)
        (-> (r/response {:slug sanitized-slug :url sanitized-url})
            (assoc-in [:session :anon-url-count]
                      (if (nil? firebase-uid) (inc anon-count) 0)))  ; ADD THIS
        (catch Exception e
          (r/status (r/response {:error (.getMessage e)}) 500)))

      ;; Auto-generate slug - already has session update
      :else
      (try
        (let [slug (generate-slug)]
          (db/insert-url-redirection! sanitized-url slug user-id)
          (-> (r/response {:slug slug :url sanitized-url})
              (assoc-in [:session :anon-url-count]
                        (if (nil? firebase-uid) (inc anon-count) 0))))
        (catch Exception e
          (r/status (r/response {:error (.getMessage e)}) 500))))))

(defn get-user-slugs
  "Returns all shortened URLs created by the authenticated user.

   Requires authentication (firebase_uid in session).
   Returns URLs ordered by creation date (newest first).

   Returns:
   - 401 if not authenticated
   - List of slug objects with :slug, :original_url, :created_at"
  [req]
  (if-let [firebase-uid (get-in req [:session :firebase-uid])]
    (try
      (if-let [user-id (db/get-user-id-by-firebase-uid firebase-uid)]
        (let [slugs (db/get-user-slugs user-id)]
          (r/response slugs))
        (r/status (r/response {:error "User not found"}) 404))
      (catch Exception e
        (r/status (r/response {:error (.getMessage e)}) 500)))
    (r/status (r/response {:error "Authentication required"}) 401)))

(defn delete-redirect
  "Deletes a shortened URL by slug.

   Requires authentication and ownership verification.
   Only the user who created the URL can delete it.

   Parameters:
   - slug: The short URL slug to delete (from path)

   Returns:
   - 401 if not authenticated
   - 403 if slug doesn't exist or doesn't belong to user
   - 200 on successful deletion"
  [req]
  (let [slug (get-in req [:path-params :slug])]
    (if-let [firebase-uid (get-in req [:session :firebase-uid])]
      (try
        (if-let [user-id (db/get-user-id-by-firebase-uid firebase-uid)]
          (if (db/slug-belongs-to-user? slug user-id)
            (do
              (db/delete-slug! slug)
              (r/response {:message "Slug deleted successfully"}))
            (r/status (r/response {:error "Slug not found or access denied"}) 403))
          (r/status (r/response {:error "User not found"}) 404))
        (catch Exception e
          (r/status (r/response {:error (.getMessage e)}) 500)))
      (r/status (r/response {:error "Authentication required"}) 401))))

(defn serve-index
  "Serves the main application HTML page from resources."
  []
  (slurp (io/resource "public/index.html")))

(def app
  "The main Ring handler for the application.

   Routes:
   - /:slug/ - Redirects to the original URL for the given slug
   - /api/redirect/ - POST endpoint to create a new shortened URL
   - /api/redirect/:slug/ - DELETE endpoint to delete a shortened URL
   - /api/user/verify - POST endpoint to verify Firebase user and set session
   - /api/user/slugs - GET endpoint to fetch user's shortened URLs
   - /assets/* - Serves static assets from resources/public/assets
   - / - Serves the main application HTML page

   Middleware:
   - muuntaja for request/response format negotiation and parsing
   - wrap-session for session management"
  (-> (ring/ring-handler
       (ring/router
        ["/"
         [":slug/" redirect]
         ["api/"
          ["redirect/"
           ["" {:post create-redirect}]
           [":slug/" {:delete delete-redirect}]]
          ["user/"
           ["verify" {:post verify-user}]
           ["slugs" {:get get-user-slugs}]]]
         ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
         ["" {:handler (fn [req] {:body (serve-index) :status 200})}]]
        {:data {:muuntaja m/instance
                :middleware [muuntaja/format-middleware]}})
       (ring/create-default-handler
        {:not-found (fn [_] {:status 404, :body "Not found"})}))
      wrap-session))


(defonce server
  ^{:doc "Atom holding the Jetty server instance. Will be nil when server is not running."}
  (atom nil))

(defn start-server!
  "Starts the web server on the specified port.

   Before starting the server, runs database migrations to ensure
   the schema is up to date. Uses the global app handler with var-quote
   to allow for REPL-based development without restarts.

   Parameters:
   - port: The port number to listen on

   Returns:
   - Stores the server instance in the server atom and returns it"
  [port]
  (println "Starting server on port" port)
  (migrations/run-migrations!)
  (reset! server (ring-jetty/run-jetty #'app {:port port :join? false})))

(defn stop-server!
  "Stops the running web server if it exists.

   Checks if the server atom contains a server instance,
   stops it, and resets the atom to nil.

   Returns:
   - nil"
  []
  (when @server
    (println "Stopping server...")
    (.stop @server)
    (reset! server nil)))

(defn restart-server!
  "Restarts the web server on the specified port.

   Stops the server if it's running, then starts it again
   on the given port.
If no port is passed, restarts using port 3001

   Parameters:
   - port: The port number to listen on

   Returns:
   - The new server instance"
  ([]
   (restart-server! 3001))

  ([port]
   (stop-server!)
   (start-server! port)))

(defn -main
  "Application entry point.

   Starts the web server on the specified port, or defaults to 3001
   if no port is specified.

   Parameters:
   - args: Command line arguments, where the first argument is the optional port

   Returns:
   - The server instance"
  [& args]
  (let [port (if (seq args)
               (Integer/parseInt (first args))
               3001)]
    (start-server! port)))

(comment

  (start-server! 3001)
  (stop-server!)
  (restart-server!)
  (serve-index)
  (create-redirect {:body-params
                    {:url "https://www.youtube.com/watch?v=0mrguRPgCzI&t=1936s"
                     :slug "manualslug"}})
  )
