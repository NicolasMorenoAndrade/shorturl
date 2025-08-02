(ns shorturl.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [shorturl.db :as db]
            [shorturl.slug :refer [generate-slug sanitize-custom-slug sanitize-url]]
            [shorturl.migrations :as migrations]
            [clojure.java.io :as io])
  (:gen-class))

(defn verify-user
  "Verifies a Firebase-authenticated user and ensures they exist in the database.

   When a user authenticates with Firebase, this endpoint:
   1. Checks if they exist in the database by Firebase UID
   2. If not, creates a new user record with their Firebase profile data
   3. If they exist, updates their last login timestamp

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
                (r/response existing-user))
            ;; New user - create record
            (do (db/create-user! firebase-uid email display-name)
                (r/response {:firebase_uid firebase-uid
                             :email email
                             :display_name display-name}))))
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
  "Creates a new shortened URL.
   Takes a URL from the request body, generates a unique slug,
   stores the mapping in the database, and returns the slug.
   Returns a 400 error if no URL is provided, is empty, or is invalid.
   Custom slugs must be at least 6 characters long."
  [req]
  (let [url (get-in req [:body-params :url])
        custom-slug (get-in req [:body-params :slug])
        sanitized-url (when url (sanitize-url url))
        sanitized-slug (when custom-slug (sanitize-custom-slug custom-slug))]
    (cond
      (nil? url)
      (r/status (r/response {:error "URL is required"}) 400)

      (empty? url)
      (r/status (r/response {:error "URL cannot be empty"}) 400)

      ;; URL is invalid
      (nil? sanitized-url)
      (r/status (r/response {:error "Invalid URL format or potentially unsafe URL"}) 400)

      ;; Custom slug provided but invalid
      (and custom-slug (nil? sanitized-slug))
      (r/status (r/response
                 {:error (str "slug must be at least 6 characters long and contain only "
                              "letters, numbers, underscores and dashes.")}) 400)
      sanitized-slug
      (try
        (db/insert-url-redirection! sanitized-url sanitized-slug)
        (r/response {:slug sanitized-slug :url sanitized-url})
        (catch Exception e
          (r/status (r/response {:error (.getMessage e)}) 500)))

      :else
      (try
        (let [slug (generate-slug)]
          (db/insert-url-redirection! sanitized-url slug)
          (r/response {:slug slug :url sanitized-url}))
        (catch Exception e
          (r/status (r/response {:error (.getMessage e)}) 500))))))

(defn serve-index
  "Serves the main application HTML page from resources."
  []
  (slurp (io/resource "public/index.html")))

(def app
  "The main Ring handler for the application.

   Routes:
   - /:slug/ - Redirects to the original URL for the given slug
   - /api/redirect/ - POST endpoint to create a new shortened URL
   - /assets/* - Serves static assets from resources/public/assets
   - / - Serves the main application HTML page

   Middleware:
   - muuntaja for request/response format negotiation and parsing"
  (ring/ring-handler
   (ring/router
    ["/"
     [":slug/" redirect]
     ["api/"
      ["redirect/" {:post create-redirect}]
      ["user/verify" {:post verify-user}]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["" {:handler (fn [req] {:body (serve-index) :status 200})}]]
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})
   (ring/create-default-handler
    {:not-found (fn [_] {:status 404, :body "Not found"})})))

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
  (create-redirect {:body-params {:url "https://www.youtube.com/watch?v=0mrguRPgCzI&t=1936s" :slug "manualslug"}}))
