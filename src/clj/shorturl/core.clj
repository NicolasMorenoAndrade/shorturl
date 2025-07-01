(ns shorturl.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [shorturl.db :as db]
            [shorturl.slug :refer [generate-slug]]
            [clojure.java.io :as io])
  (:gen-class))


(defn redirect
  "Handles redirection requests for shortened URLs.
   Looks up the slug in the database and redirects to the original URL if found.
   Returns a 404 if the slug doesn't exist or 400 if no slug is provided."
  [req]
  (if-let [slug (get-in req [:path-params :short_code])]
    (if-let [url (db/get-url slug)]
      (r/redirect url 307)
      (r/status (r/response {:error "URL not found"}) 404))
    (r/status (r/response {:error "Slug is required"}) 400)))


(defn create-redirect
  "Creates a new shortened URL.
   Takes a URL from the request body, generates a unique slug,
   stores the mapping in the database, and returns the slug.
   Returns a 400 error if no URL is provided or 500 on database errors."
  [req]
  (if-let [url (get-in req [:body-params :url])]
    (try
      (let [slug (generate-slug)]
        (db/insert-url-redirection! url slug)
        (r/response {:slug slug :url url}))
      (catch Exception e
        (r/status (r/response {:error (.getMessage e)}) 500)))
    (r/status (r/response {:error "URL is required"}) 400)))


(defn index
  "Serves the main application HTML page from resources."
  []
  (slurp (io/resource "public/index.html"))
  )


(def app
  (ring/ring-handler
   (ring/router
    [
     "/"
     [":short_code/" redirect ]
     ["api/"
      ;; ["redirect/" {:post create-redirect}]]
     ["assets/*" (ring/create-resource-handler {:root "public/assets"})]
     ["" {:handler (fn [req] {:body (index) :status 200})}]
     ]
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})))

;; (def app
;;   (ring/ring-handler
;;    (ring/router
;;     ["/"
;;      ["" {:handler (fn [req] {:body "hello" :status 200})}]
;;      ]
;;     {:data {:muuntaja m/instance
;;             :middleware [muuntaja/format-middleware]}}
;;     )
;;    )
;;   )

(defn start []
  (ring-jetty/run-jetty #'app {:port 3001
                               :join? false}))

(comment

  (def server (start))

  (.stop server)

  (index)


)
