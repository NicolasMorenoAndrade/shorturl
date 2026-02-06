(ns app.api
  (:require [promesa.core :as p]))
;; ============================================================================
;; CSRF Token Handling
;; ============================================================================

(defn get-csrf-token
  "Reads CSRF token from meta tag injected by server"
  []
  (when-let [meta-tag (js/document.querySelector "meta[name='csrf-token']")]
    (.getAttribute meta-tag "content")))


;; ============================================================================
;; API Functions
;; ============================================================================


(defn fetch-slug
  "Creates a shortened URL by calling the backend API.
   Returns a promise that resolves to the API response data."
  ([url slug]
   (let [body (if (= "" slug)
                #js {:url url}
                #js {:url url :slug slug})]
     (p/let [response (js/fetch "/api/redirect/"
                                (clj->js {:headers {:Content-Type "application/json"
                                                    :X-CSRF-Token (get-csrf-token)}
                                          :method "POST"
                                          :credentials "same-origin"  ; IMPORTANT
                                          :body (js/JSON.stringify body)}))
             json-data (.json response)
             data (js->clj json-data :keywordize-keys true)]
       data))))

(defn verify-firebase-user
  "Verifies a Firebase user with the backend API.

   Makes a POST request to /api/user/verify with the user's Firebase data.
   Returns a promise that resolves to the verified user data from the database.

   Parameters:
   - firebase-user: Map containing Firebase authentication data (:uid, :email, :display-name)"
  [firebase-user]
  (p/let [response (js/fetch "/api/user/verify"
                             (clj->js {:headers {:Content-Type "application/json"
                                                 :X-CSRF-Token (get-csrf-token)}
                                       :method "POST"
                                       :body (js/JSON.stringify
                                              #js {:firebase-uid (:uid firebase-user)
                                                   :email (:email firebase-user)
                                                   :display-name (:display-name firebase-user)})}))
          json-data (.json response)
          data (js->clj json-data :keywordize-keys true)]
    data))

(defn fetch-user-slugs
  "Fetches all shortened URLs for the authenticated user.
   Returns a promise that resolves to a list of slug objects.

   Each slug object contains:
   - :slug - the short URL identifier
   - :original_url - the destination URL
   - :created_at - timestamp of creation"
  []
  (p/let [response (js/fetch "/api/user/slugs"
                             (clj->js {:method "GET"
                                       :credentials "same-origin"}))
          json-data (.json response)
          data (js->clj json-data :keywordize-keys true)]
    data))

(defn delete-slug
  "Deletes a shortened URL by its slug.

   Requires authentication and ownership.
   Returns a promise that resolves when deletion is complete.

   Parameters:
   - slug: The short URL identifier to delete"
  [slug]
  (p/let [response (js/fetch (str "/api/redirect/" slug "/")
                             (clj->js {:method "DELETE"
                                       :credentials "same-origin"
                                       :headers {:X-CSRF-Token (get-csrf-token)}}))
          json-data (.json response)
          data (js->clj json-data :keywordize-keys true)]
    data))
