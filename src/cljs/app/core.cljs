(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            [promesa.core :as p]))


(defnc app []
  (let [[state set-state] (hooks/use-state {:slug nil
                                            :url ""})
        [hovered set-hovered] (hooks/use-state false)
        fetch-slug (fn []
                     (p/let [response (js/fetch "/api/redirect/"
                                                (clj->js {:headers {:Content-Type "application/json"}
                                                          :method "POST"
                                                          :body (js/JSON.stringify #js {:url (:url state)})}))
                             json-data (.json response)
                             data (js->clj json-data :keywordize-keys true)]
                       (set-state assoc :slug (:slug data))))]

    (d/div
     (if (:slug state)
       (d/div (str () (:slug state)))
       (d/div
        (d/input {:value (:url state)
               :on-change #(set-state assoc :url (.. % -target -value))})
      ;; (d/button "Shorten URL")
      (d/button {
           :style {:background-color (if hovered "#AAAAAA" "#C0C0C0")
                   :margin-left "10px"
                   :transition "background-color 0.3s"
                   :color "white"}
          :on-mouse-enter #(set-hovered true)
          :on-mouse-leave #(set-hovered false)
          :on-click #(fetch-slug)
                 }
          "Shorten URL")
        )
     ))))


(defn ^:export init []
  (let [root (rdom/createRoot (js/document.getElementById "app"))]
    (.render root ($ app))
    (.log js/console "URL shortener app initialized")))
