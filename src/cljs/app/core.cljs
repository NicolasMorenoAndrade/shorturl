(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            [promesa.core :as p]))


(defnc app []
  (let [[state set-state] (hooks/use-state {:slug nil :url ""})
        fetch-slug
        (fn [] (p/let [response (js/fetch "/api/redirect/"
                                          (clj->js {:headers {:Content-Type "application/json"}
                                                    :method "POST"
                                                    :body (js/JSON.stringify #js {:url (:url state)})}))
                       json-data (.json response)
                       data (js->clj json-data :keywordize-keys true)]
                 (set-state assoc :slug (:slug data))))
        redirect-link
        (str (.-origin js/window.location) "/" (:slug state) "/")
        ]

    (d/div
     (if (:slug state)
       (d/div (d/a {:href redirect-link} redirect-link))
       (d/div
        (d/input {:value (:url state)
                  :on-change #(set-state assoc :url (.. % -target -value))})
        (d/button {:on-click #(fetch-slug)} "Shorten URL"))))))


(defn ^:export init []
  (let [root (rdom/createRoot (js/document.getElementById "app"))]
    (.render root ($ app))
    (.log js/console "URL shortener app initialized")))
