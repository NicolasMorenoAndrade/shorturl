(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            [promesa.core :as p]
            [app.api :as api]
            [app.styles :refer [styles]]
            [app.firebase :as firebase]
            [app.hooks :refer [use-click-outside]]))

(defnc app []
  (let [[state set-state] (hooks/use-state {:user {:display-name "" :email ""} :authenticated? false
                                            :slug nil :url "" :custom-slug ""
                                            :loading? false :dropdown-open? false
                                            :error nil})
        handle-shorten-url
        (fn []
          (set-state assoc :loading? true)
          (-> (api/fetch-slug (:url state) (:custom-slug state))
              (p/then #(set-state assoc :slug (:slug %) :error (:error %)))
              (p/finally #(set-state assoc :loading? false))
              ))
        redirect-link
        (str (.-origin js/window.location) "/" (:slug state) "/")
        auth-button-ref (hooks/use-ref nil)
        dropdown-ref (hooks/use-ref nil)]

    ;; declare hooks
    ;; Set up auth listener on component mount
    (hooks/use-effect
     []  ;; Empty dependency array = run once on mount
     (firebase/set-user! set-state))

    (use-click-outside
     [auth-button-ref dropdown-ref]
     #(when (:dropdown-open? state)
        (set-state assoc :dropdown-open? false)))

    (hooks/use-effect
     [(:loading? state)]
     (.log js/console (str ":loading? state (value): " (:loading? state))))

    (d/div {:class-name (get styles :container)}
           (d/div {:class-name (get styles :card)}
                  (d/div {:class-name "relative"}
                         (d/div {:class-name (get-in styles [:auth :container])}
                                (d/div
                                 (d/button {:class-name (get-in styles [:auth (if (:dropdown-open? state)
                                                                                :user-icon-clicked
                                                                                :user-icon-unclicked)])
                                            :ref auth-button-ref
                                            :on-click #(set-state assoc :dropdown-open?
                                                                  (not (:dropdown-open? state)))}
                                           (-> (get-in state [:user :display-name] "User")
                                               first
                                               str)))

                                (when (:dropdown-open? state)
                                  (d/div {:class-name (get-in styles [:auth :dropdown])
                                          :ref dropdown-ref}

                                         ;; User info
                                         (d/div {:class-name (get-in styles [:auth :user-info])}
                                                (if (:authenticated? state)
                                                  (d/div (d/p {:class-name (get-in styles [:auth :welcome])}
                                                              (str (get-in state [:user :display-name])))
                                                         (d/p {:class-name (get-in styles [:auth :email])}
                                                              (str (get-in state [:user :email]))))
                                                  (d/p "Not signed-in")))
                                         (d/div
                                          (if (not (:authenticated? state))
                                           ;; Sign in button for logged-out user
                                            (d/button {:class-name (get-in styles [:auth :dropdown-item])
                                                       :on-click #(firebase/google-sign-in)}
                                                      "Sign in")
                                            (d/button {:class-name (get-in styles [:auth :dropdown-item])
                                                       :on-click
                                                       (fn [] (firebase/sign-out-with-callback!
                                                               #(set-state assoc :dropdown-open? false)
                                                               #(.error js/console "Sign out failed:" %)))}
                                                      "Sign out")))))))

                  (d/h1 {:class-name (get styles :title)}
                        "URL Shortener")

                  (if (:slug state)
                    ;; Result display section
                    (d/div {:class-name (get-in styles [:result-section :container])}
                           (d/p {:class-name (get-in styles [:result-section :label])}
                                "Your shortened URL:")
                           (d/a {:href redirect-link
                                 :class-name (get-in styles [:result-section :link])}
                                redirect-link)
                           (d/button {:class-name (get-in styles [:result-section :button])
                                      :on-click #(set-state {:slug nil :url "" :custom-slug ""})}
                                     "Create Another Link"))

                    ;; Form section
                    (d/form {:on-submit (fn [e]
                                          (.preventDefault e) (if (empty? (:url state))
                                                                (js/alert "URL cannot be empty")
                                                                (handle-shorten-url)))
                             :class-name (get-in styles [:form :container])}

                            (d/div
                             (d/label {:for "url-input"
                                       :class-name (get-in styles [:form :label])}
                                      "URL to Shorten")
                             (d/input {:id "url-input"
                                       :value (:url state)
                                       :disabled (:loading? state)
                                       :on-change #(set-state assoc :url (.. % -target -value))
                                       :class-name (get-in styles [:form :input])
                                       :placeholder "https://example.com/long/path"}))

                            (d/div
                             (d/label {:for "slug-input"
                                       :class-name (get-in styles [:form :label])}
                                      "Custom Slug (Optional)")
                             (d/input {:id "slug-input"
                                       :value (:custom-slug state)
                                       :disabled (:loading? state)
                                       :on-change #(set-state assoc :custom-slug (.. % -target -value))
                                       :class-name (get-in styles [:form :input])
                                       :placeholder "e.g., my-link"}))

                            (d/button {:type "submit"
                                       :disabled (:loading? state)
                                       :class-name (str (get-in styles [:form :button :base]) " "
                                                        (if (:loading? state)
                                                          (get-in styles [:form :button :disabled])
                                                          (get-in styles [:form :button :enabled])))}
                                      (if (:loading? state)
                                        (d/div {:class-name (get-in styles [:loading :container])}
                                               (d/span {:class-name (get-in styles [:loading :spinner])})
                                               "Shortening...")
                                        "Shorten URL"))
                      (when (:error state)
                        (d/p {:class-name (get styles :error)}
               (:error state)))
                            ))
                  (d/div {:class-name (get-in styles [:auth :container])})))))

(defn ^:export init
  "Initializes the URL shortener application.

   Creates ($) a React root in the 'app' DOM element and renders
   the main application component. This function is exported
   and called from JavaScript when the page loads.

   Returns:
   - nil, but has the side effect of rendering the application"
  []
  (let [root (rdom/createRoot (js/document.getElementById "app"))]
    (.render root ($ app))
    (.log js/console "URL shortener app initialized")))
