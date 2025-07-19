(ns shorturl.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [shorturl.db :as db]
            [shorturl.slug :refer [generate-slug]]
            [shorturl.migrations :as migrations]
            [clojure.java.io :as io])
  (:gen-class))

(defn redirect
  "Handles redirection requests for shortened URLs.
   Looks up the slug in the database and redirects to the original URL if found.
   Returns a 404 if the slug doesn't exist or 400 if no slug is provided."
  [req]
  (if-let [slug (get-in req [:path-params :slug])]
    (if-let [url (db/get-url slug)]
      (r/redirect url 307)
      (r/status (r/response {:error "URL not found"}) 404))
    (r/status (r/response {:error "Slug is required"}) 400)))

(defn create-redirect
  "Creates a new shortened URL.
   Takes a URL from the request body, generates a unique slug,
   stores the mapping in the database, and returns the slug.
   Returns a 400 error if no URL is provided or if it's empty."
  [req]
  (let [url (get-in req [:body-params :url])
        slug (get-in req [:body-params :slug])]
    (cond
      (nil? url)
      (r/status (r/response {:error "URL is required"}) 400)

      (empty? url)
      (r/status (r/response {:error "URL cannot be empty"}) 400)

      slug
      (try
        (db/insert-url-redirection! url slug)
        (r/response {:slug slug :url url})
        (catch Exception e
          (r/status (r/response {:error (.getMessage e)}) 500)))

      :else
      (try
        (let [slug (generate-slug)]
          (db/insert-url-redirection! url slug)
          (r/response {:slug slug :url url}))
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
      ["redirect/" {:post create-redirect}]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["" {:handler (fn [req] {:body (serve-index) :status 200})}]]
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})))

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
