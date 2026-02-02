(ns app.components
  (:require [helix.core :refer [defnc]]
            [helix.dom :as d]
            [app.styles :refer [styles]]
            [app.firebase :as firebase]
            [app.handlers :refer [toggle-dropdown sign-out-user handle-shorten-url
                                  toggle-slugs-section handle-delete-slug handle-slug-click]]))

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
    {:class-name (get-in styles [:auth (if (:dropdown-open? state)
                                         :user-icon-clicked
                                         :user-icon-unclicked)])
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
                      :on-click #(set-state assoc :slug nil :url "" :custom-slug "")}
                     "Create Another Link"))))

(defnc UserSlugsSection [{:keys [state set-state]}]
  ;; Only render if user is authenticated
  (when (:authenticated? state)
    (d/div {:class-name (get-in styles [:slugs-section :container])}

      ;; Collapsible header
      (d/div {:class-name (get-in styles [:slugs-section :header])
              :on-click #(toggle-slugs-section state set-state)}
        (d/h3 {:class-name (get-in styles [:slugs-section :title])}
          (str "Your Short Links"
               (when (:user-slugs state)
                 (str " (" (count (:user-slugs state)) ")"))))
        (d/span {:class-name (get-in styles [:slugs-section :toggle])}
          (if (:slugs-section-open? state) "▼" "▶")))

      ;; Content (only shown when open)
      (when (:slugs-section-open? state)
        (d/div
          (cond
            ;; Loading state
            (:slugs-loading? state)
            (d/div {:class-name (get-in styles [:slugs-section :loading])}
              (d/div {:class-name (get-in styles [:loading :container])}
                (d/span {:class-name (get-in styles [:loading :spinner])})
                "Loading your links..."))

            ;; Empty state
            (empty? (:user-slugs state))
            (d/div {:class-name (get-in styles [:slugs-section :empty])}
              "You haven't created any short links yet.")

            ;; List of slugs
            :else
            (d/div {:class-name (get-in styles [:slugs-section :list])}
              (for [slug-data (:user-slugs state)]
                (let [slug (:shortened_urls/slug slug-data)
                      url (:shortened_urls/original_url slug-data)
                      deleting? (= (:deleting-slug state) slug)]
                  (d/div {:key slug
                          :class-name (get-in styles [:slugs-section :item :container])}

                    (d/div {:class-name "flex justify-between items-start"}
                      ;; Slug and URL
                      (d/div {:class-name "flex-1"}
                        (d/div {:class-name (get-in styles [:slugs-section :item :slug])
                                :on-click #(handle-slug-click slug)}
                          slug)
                        (d/div {:class-name (get-in styles [:slugs-section :item :url])}
                          (str "→ " url)))

                      ;; Delete button
                      (d/button {:class-name (get-in styles [:slugs-section :item :delete-btn])
                                 :disabled deleting?
                                 :on-click #(handle-delete-slug slug state set-state)}
                        (if deleting?
                          (d/div {:class-name (get-in styles [:loading :container])}
                            (d/span {:class-name (get-in styles [:loading :spinner])})
                            "Deleting...")
                          "🗑️ Delete")))))))))))))
