(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            [promesa.core :as p]
            [app.api :as api]))

(defnc app []
  (let [[state set-state] (hooks/use-state {:slug nil :url "" :custom-slug ""})
        handle-shorten-url
        (fn []
          (-> (api/fetch-slug (:url state) (:custom-slug state))
              (p/then #(set-state assoc :slug (:slug %)))))
        redirect-link
        (str (.-origin js/window.location) "/" (:slug state) "/")]

    (d/div {:class-name "bg-purple-100 grid place-items-center h-screen"}
           (if (:slug state)
             (d/div (d/a {:href redirect-link
                          :class-name "text-blue-500 hover:text-purple-600"} redirect-link))
             (d/div
              (d/input {:value (:url state)
                        :on-change #(set-state assoc :url (.. % -target -value))
                        :class-name "form-control border border-solid border-gray-600"
                        :placeholder "Enter URL"})
              (d/input {:value (:custom-slug state)
                        :on-change #(set-state assoc :custom-slug (.. % -target -value))
                        :class-name "form-control border border-solid border-gray-600"
                        :placeholder "Enter slug"})
              (d/button {:on-click #(handle-shorten-url)
                         :class-name "border-1 rounded px-4 uppercase"}
                        "Shorten URL"))))))

(defn ^:export init
  "Initializes the URL shortener application.

   Creates a React root in the 'app' DOM element and renders
   the main application component. This function is exported
   and called from JavaScript when the page loads.

   Returns:
   - nil, but has the side effect of rendering the application"
  []
  (let [root (rdom/createRoot (js/document.getElementById "app"))]
    (.render root ($ app))
    (.log js/console "URL shortener app initialized")))
