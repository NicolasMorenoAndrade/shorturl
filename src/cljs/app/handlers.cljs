(ns app.handlers
  (:require [promesa.core :as p]
            [app.api :as api]
            [app.firebase :as firebase]))

(defn toggle-dropdown
  "Handler to toggle the dropdown state.
   Parameters:
   - state: Current local component state map
   - set-state: Function to update component state"
  [state set-state]
  (set-state assoc :dropdown-open? (not (:dropdown-open? state))))

(defn sign-out-user
  "Handler for signing out the user.
   Clears all user-specific state including URLs, slugs list, and authentication.

   Parameters:
   - set-state: Function to update component state"
  [set-state]
  (firebase/sign-out-with-callback!
   ;; On successful sign-out, reset all user-related state
   #(set-state (fn [_]
                 {:user nil
                  :authenticated? false
                  :slug nil
                  :url ""
                  :custom-slug ""
                  :loading? false
                  :dropdown-open? false
                  :error nil
                  ;; Clear slugs management state
                  :user-slugs nil
                  :slugs-loading? false
                  :slugs-section-open? false
                  :deleting-slug nil}))
   #(.error js/console "Sign out failed:" %)))

(defn handle-fetch-slugs
  "Fetches the user's shortened URLs from the API.

   Parameters:
   - state: Current local component state map
   - set-state: Function to update component state"
  [state set-state]
  (set-state assoc :slugs-loading? true)
  (-> (api/fetch-user-slugs)
      (p/then (fn [response]
                (set-state assoc :user-slugs response)))
      (p/catch (fn [err]
                 (.error js/console "Error fetching slugs:" err)
                 (set-state assoc :error "Failed to load your URLs")))
      (p/finally (fn []
                   (set-state assoc :slugs-loading? false)))))

(defn toggle-slugs-section
  "Toggles the slugs section open/closed.
   If opening for the first time (user-slugs is nil), fetches the slugs.

   Parameters:
   - state: Current local component state map
   - set-state: Function to update component state"
  [state set-state]
  (let [currently-open? (:slugs-section-open? state)
        user-slugs (:user-slugs state)]
    ;; Toggle the section
    (set-state assoc :slugs-section-open? (not currently-open?))

    ;; If opening for the first time and slugs not loaded, fetch them
    (when (and (not currently-open?)
               (nil? user-slugs))
      (handle-fetch-slugs state set-state))))

(defn handle-delete-slug
  "Deletes a slug after user confirmation.

   Shows a native confirm dialog before deletion.
   Optimistically removes the slug from the UI on success.

   Parameters:
   - slug: The slug to delete
   - state: Current local component state map
   - set-state: Function to update component state"
  [slug state set-state]
  (when (js/confirm (str "Are you sure you want to delete '" slug "'?"))
    (set-state assoc :deleting-slug slug)
    (-> (api/delete-slug slug)
        (p/then (fn [_]
                  ;; Remove the deleted slug from the list
                  (set-state (fn [current-state]
                               (assoc current-state
                                      :user-slugs
                                      (filterv #(not= (:shortened_urls/slug %) slug)
                                               (:user-slugs current-state)))))))
        (p/catch (fn [err]
                   (.error js/console "Error deleting slug:" err)
                   (set-state assoc :error "Failed to delete URL")))
        (p/finally (fn []
                     (set-state assoc :deleting-slug nil))))))

(defn handle-slug-click
  "Opens a shortened URL in a new tab.

   Parameters:
   - slug: The slug to open"
  [slug]
  (let [url (str (.-origin js/window.location) "/" slug "/")]
    (.open js/window url "_blank")))

(defn handle-shorten-url
  "Handler for shortening a URL.
   Updated to optimistically add new slugs to the user's list.

   Parameters:
   - state: Current local component state map
   - set-state: Function to update component state"
  [state set-state]
  (set-state assoc :loading? true)
  (-> (api/fetch-slug (:url state) (:custom-slug state))
      (p/then (fn [response]
                (set-state assoc :slug (:slug response) :error (:error response))

                ;; If user is authenticated AND slugs are loaded, add to list
                (when (and (:authenticated? state)
                           (not (nil? (:user-slugs state))))
                  (let [new-slug {:shortened_urls/slug (:slug response)
                                  :shortened_urls/original_url (:url state)
                                  :shortened_urls/created_at (js/Date.)}]
                    (set-state (fn [current-state]
                                 (update current-state :user-slugs
                                         #(into [new-slug] %))))))))
      (p/finally (fn []
                   (set-state assoc :loading? false)))
      (p/catch (fn [err]
                 (.error js/console "Error in API call:" err)
                 (set-state assoc :error (or (-> err .-responseText js/JSON.parse :error)
                                             "An unknown error occurred"))))))
