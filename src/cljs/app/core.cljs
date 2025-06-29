(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]))

;; define components using the `defnc` macro
;; (defnc greeting
;;   "A component which greets a user."
;;   [{:keys [name]}]
;;   ;; use helix.dom to create DOM elements
;;   (d/div "Hello, " (d/strong name) "!"))

;; (defnc app []
;;   (let [[state set-state] (hooks/use-state {:name "Helix User"})]
;;     (d/div
;;      (d/h1 "Welcome!")
;;       ;; create elements out of components
;;       ($ greeting {:name (:name state)})
;;       (d/input {:value (:name state)
;;                 :on-change #(set-state assoc :name (.. % -target -value))}))))
;; fetch-slugh
;; fetch-slugh (fn [])

(defnc app []
  (let [[state set-state] (hooks/use-state {:url ""})]
    (d/div
     (d/input {:value (:url state)
               :on-change #(set-state assoc :url (.. % -target -value))})
      (d/button "Shorten URL")
     )))

;; start your app with your favorite React renderer
;; (defonce root (rdom/createRoot (js/document.getElementById "app")))

(defn ^:export init []
  (let [root (rdom/createRoot (js/document.getElementById "app"))]
  (.render root ($ app))
  (.log js/console "howzit")
  (js/alert "hello")
  ))
