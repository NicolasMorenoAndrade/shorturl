(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            [promesa.core :as p]
            [app.api :as api]))

(defnc app []
  (let [[state set-state] (hooks/use-state {:slug nil :url "" :custom-slug "" :loading? false})
        handle-shorten-url
        (fn []
          (set-state assoc :loading? true)
          ;; (.log js/console (str "state " (:loading? state)))
          (-> (api/fetch-slug (:url state) (:custom-slug state))
              (p/then #(set-state assoc :slug (:slug %)))
              (p/finally #(set-state assoc :loading? false))))
        redirect-link
        (str (.-origin js/window.location) "/" (:slug state) "/")]

    (hooks/use-effect
     ;; an example of the usage of use-effect. Will run on every change of (:loading state),
     [(:loading? state)]
     (.log js/console (str "loading state changed to: " (:loading? state))))
    (d/div {:class-name "bg-red-100 grid place-items-center h-screen p-4"}
           (d/div {:class-name "bg-white rounded-lg shadow-md p-8 w-full max-w-md"}
                  (d/h1 {:class-name "text-2xl font-bold text-red-800 mb-6 text-center"}
                        "URL Shortener")

                  (if (:slug state)
                    (d/div {:class-name "text-center"}
                           (d/p {:class-name "mb-3 text-gray-600"} "Your shortened URL:")
                           (d/a {:href redirect-link
                                 :class-name "text-red-500 hover:text-red-600 font-medium text-lg break-all"}
                                redirect-link)
                           (d/button {:class-name "mt-6 w-full bg-red-600 hover:bg-red-700 text-white py-2 px-4 rounded transition-colors duration-200"
                                      :on-click #(set-state {:slug nil :url "" :custom-slug ""})}
                                     "Create Another Link"))

                    (d/form {:on-submit (fn [e] (.preventDefault e) (handle-shorten-url))
                             :class-name "space-y-4"}

                            (d/div
                             (d/label {:for "url-input"
                                       :class-name "block text-sm font-medium text-gray-700 mb-1"}
                                      "URL to Shorten")
                             (d/input {:id "url-input"
                                       :value (:url state)
                                       :disabled (:loading? state)
                                       :on-change #(set-state assoc :url (.. % -target -value))
                                       :class-name "w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                       :placeholder "https://example.com/long/path"}))

                            (d/div
                             (d/label {:for "slug-input"
                                       :class-name "block text-sm font-medium text-gray-700 mb-1"}
                                      "Custom Slug (Optional)")
                             (d/input {:id "slug-input"
                                       :value (:custom-slug state)
                                       :disabled (:loading? state)
                                       :on-change #(set-state assoc :custom-slug (.. % -target -value))
                                       :class-name "w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                                       :placeholder "e.g., my-link"}))

                            (d/button {:type "submit"
                                       :disabled (:loading? state)
                                       :class-name (str "w-full bg-red-600 "
                                                        (if (:loading? state) "opacity-70 cursor-not-allowed" "hover:bg-red-700 cursor-pointer") " text-white py-2 px-4 rounded-md transition-colors duration-200 font-medium mt-2")}
                                      (if (:loading? state)
                                        (d/div {:class-name "flex items-center justify-center"} (d/span {:class-name "animate-spin mr-2 h-4 w-4 border-t-2 border-b-2 border-white rounded-full"}) "Shortening...")
                                        "Shorten URL"))))))))

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
