(ns app.handlers
  (:require [promesa.core :as p]
            [app.api :as api]
            [app.firebase :as firebase]))

(defn handle-shorten-url
  "Handler for shortening a URL.
   Parameters:
   - state: Current local component state map
   - set-state: Function to update component state"
  [state set-state]
  (set-state assoc :loading? true)
  (-> (api/fetch-slug (:url state) (:custom-slug state))
      (p/then #(set-state assoc :slug (:slug %) :error (:error %)))
      (p/finally #(set-state assoc :loading? false))
      (p/catch (fn [err]
                 (.error js/console "Error in API call:" err)
                 (set-state assoc :error (or (-> err .-responseText js/JSON.parse :error)
                                             "An unknown error occurred"))))))

(defn toggle-dropdown
  "Handler to toggle the dropdown state.
   Parameters:
   - state: Current local component state map
   - set-state: Function to update component state"
  [state set-state]
  (set-state assoc :dropdown-open? (not (:dropdown-open? state))))

(defn sign-out-user
  "Handler for signing out the user.
   Parameters:
   - set-state: Function to update component state"
  [set-state]
  (firebase/sign-out-with-callback!
   #(set-state assoc :dropdown-open? false)
   #(.error js/console "Sign out failed:" %)))
