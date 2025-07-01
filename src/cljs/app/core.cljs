(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            [promesa.core :as p]))

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
  (let [[state set-state] (hooks/use-state {:url ""})
        [hovered set-hovered] (hooks/use-state false)
        fetch-slug (fn []
                     (p/let [response (js/fetch "https://httpbin.org/uuid")
                             json-data (.json response)]
                       (println json-data)))]

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
     )))
;; :on-click #(js/alert "Button clicked!")
;; ;; start your app with your favorite React renderer
;; ;; (defonce root (rdom/createRoot (js/document.getElementById "app")))

;; (defn ^:export init []
;;   (let [root (rdom/createRoot (js/document.getElementById "app"))]
;;     (.render root ($ app))
;;     (.log js/console "howzit")
;;     (js/alert "hello")
;;     )
;;   )

;; Function to send the URL to the backend and get a shortened slug
;; (defn fetch-slug [url set-state]
;;   (-> (js/fetch "/api/redirect/"
;;                 (clj->js {:method "POST"
;;                           :headers {"Content-Type" "application/json"}
;;                           :body (js/JSON.stringify #js {:url url})}))
;;       (.then (fn [response] (.json response)))
;;       (.then (fn [data]
;;                (let [result (js->clj data :keywordize-keys true)]
;;                  (set-state assoc :shortened-slug (:slug result)))))
;;       (.catch (fn [error]
;;                 (js/console.error "Error:" error)
;;                 (set-state assoc :error "Failed to shorten URL")))))

;; (defnc app []
;;   (let [[state set-state] (hooks/use-state {:url "" :shortened-slug nil :error nil})
;;         [hovered set-hovered] (hooks/use-state false)]

;;     (d/div
;;      (d/h1 "URL Shortener")

;;      ;; Input and button row
;;      (d/div {:style {:display "flex" :margin-bottom "20px"}}
;;        (d/input {:value (:url state)
;;                  :placeholder "Enter a URL to shorten"
;;                  :style {:flex "1" :padding "8px"}
;;                  :on-change #(set-state assoc :url (.. % -target -value))})
;;        (d/button {:style {:background-color (if hovered "#4a7bff" "#5c8aff")
;;                          :margin-left "10px"
;;                          :padding "8px 16px"
;;                          :border "none"
;;                          :border-radius "4px"
;;                          :transition "background-color 0.3s"
;;                          :color "white"
;;                          :cursor "pointer"}
;;                   :on-mouse-enter #(set-hovered true)
;;                   :on-mouse-leave #(set-hovered false)
;;                   :on-click #(fetch-slug (:url state) set-state)}
;;                  "Shorten URL"))

;;      ;; Results area
;;      (when (:shortened-slug state)
;;        (d/div {:style {:margin-top "20px" :padding "15px" :background "#f0f0f0" :border-radius "4px"}}
;;          (d/p "Your shortened URL:")
;;          (d/a {:href (str "/" (:shortened-slug state) "/")
;;                :target "_blank"
;;                :style {:font-weight "bold"}}
;;               (str (.. js/window -location -origin) "/" (:shortened-slug state) "/"))))

;;      ;; Error display
;;      (when (:error state)
;;        (d/div {:style {:margin-top "20px" :color "red"}}
;;          (d/p (:error state)))))))

(defn ^:export init []
  (let [root (rdom/createRoot (js/document.getElementById "app"))]
    (.render root ($ app))
    (.log js/console "URL shortener app initialized")))
