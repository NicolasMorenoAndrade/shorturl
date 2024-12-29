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
  ;; {:body "hello2" :status 200}
  (let [slug (get-in req [:path-params :short_code])
        url (db/get-url slug)]
    (if url
      ;; (r/response url)
      (r/redirect url 307)
      (r/not-found "Not found"))))


(defn create-redirect [req]
  (let [url (get-in req [:body-params :url])
        slug (generate-slug)]
    (db/insert-url-redirection! url slug)
    (r/response (str "create slug " slug))
    )
  )


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
