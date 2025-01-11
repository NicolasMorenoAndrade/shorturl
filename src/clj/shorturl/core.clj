(ns shorturl.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [ring.util.response :as r]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [shorturl.db :as db]
            [shorturl.slug :refer [generate-slug]])
  (:gen-class))


(defn redirect [req]
  (if-let [slug (get-in req [:path-params :short_code])]
    (if-let [url (db/get-url slug)]
      (r/redirect url 307)
      (r/status (r/response {:error "URL not found"}) 404))
    (r/status (r/response {:error "Slug is required"}) 400)))


(defn create-redirect [req]
  (if-let [url (get-in req [:body-params :url])]
    (try
      (let [slug (generate-slug)]
        (db/insert-url-redirection! url slug)
        (r/response {:slug slug :url url}))
      (catch Exception e
        (r/status (r/response {:error (.getMessage e)}) 500)))
    (r/status (r/response {:error "URL is required"}) 400)))


(def app
  (ring/ring-handler
   (ring/router
    [
     "/"
     [":short_code/" redirect ]
     ["" {:handler (fn [req] {:body (str "req: " req) :status 200})}]
     ["api/"
      ["redirect/" {:post create-redirect}]]
     ]
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty #'app {:port 3001
                               :join? false}))

(comment

 (def server (start))

 (.stop server)
 )
