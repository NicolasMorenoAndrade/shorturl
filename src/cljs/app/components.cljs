(ns app.components
  (:require [helix.core :refer [defnc ]]
            [helix.dom :as d]
            [app.styles :refer [styles]]
            [app.firebase :as firebase]
            [app.handlers :refer [toggle-dropdown sign-out-user handle-shorten-url]]))

(defnc FormComponent [{:keys [state set-state]}]
  (d/form {:on-submit (fn [e]
                        (.preventDefault e)
                        (if (empty? (:url state))
                          (js/alert "URL cannot be empty")
                          (handle-shorten-url state set-state)))
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
                 (:error state)))))

(defnc AuthComponent [{:keys [state set-state auth-button-ref dropdown-ref]}]
  (d/div
   {:class-name (get-in styles [:auth :container])}
   (d/button
    {:class-name (str (get-in styles [:auth :user-icon :base]) " "
                      (if (:dropdown-open? state)
                        (get-in styles [:auth :user-icon :clicked])
                        (get-in styles [:auth :user-icon :unclicked])))

     :ref auth-button-ref
     :on-click #(toggle-dropdown state set-state)}
    (-> (get-in state [:user :display-name] "User")
        first
        str))

   (when (:dropdown-open? state)
     (d/div
      {:class-name (get-in styles [:auth :dropdown])
       :ref dropdown-ref}
      (d/div {:class-name (get-in styles [:auth :user-info])}
             (if (:authenticated? state)
               (d/div (d/p {:class-name (get-in styles [:auth :welcome])}
                           (str (get-in state [:user :display-name])))
                      (d/p {:class-name (get-in styles [:auth :email])}
                           (str (get-in state [:user :email]))))
               (d/p "Not signed-in")))
      (d/div
       (if (not (:authenticated? state))
         (d/button {:class-name (get-in styles [:auth :dropdown-item])
                    :on-click #(firebase/google-sign-in)}
                   "Sign in")
         (d/button {:class-name (get-in styles [:auth :dropdown-item])
                    :on-click #(sign-out-user set-state)}
                   "Sign out")))))))

(defnc ResultSection [{:keys [state set-state]}]
  (let [redirect-link (str (.-origin js/window.location) "/" (:slug state) "/")]
    (d/div {:class-name (get-in styles [:result-section :container])}
           (d/p {:class-name (get-in styles [:result-section :label])}
                "Your shortened URL:")
           (d/a {:href redirect-link
                 :class-name (get-in styles [:result-section :link])}
                redirect-link)
           (d/button {:class-name (get-in styles [:result-section :button])
                      :on-click #(set-state {:slug nil :url "" :custom-slug ""})}
                     "Create Another Link"))))
