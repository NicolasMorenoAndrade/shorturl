(ns app.api
  (:require [promesa.core :as p]))

(defn fetch-slug
  "Creates a shortened URL by calling the backend API.
   Returns a promise that resolves to the API response data."
  ([url slug]
   (if (= "" slug)
     (p/let [response (js/fetch "/api/redirect/"
                                (clj->js {:headers {:Content-Type "application/json"}
                                          :method "POST"
                                          :body (js/JSON.stringify #js {:url url})}))
             json-data (.json response)
             data (js->clj json-data :keywordize-keys true)]
       data)
     (p/let [response (js/fetch "/api/redirect/"
                                (clj->js {:headers {:Content-Type "application/json"}
                                          :method "POST"
                                          :body (js/JSON.stringify #js {:url url :slug slug})}))
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
                             (clj->js {:headers {:Content-Type "application/json"}
                                       :method "POST"
                                       :body (js/JSON.stringify
                                              #js {:firebase-uid (:uid firebase-user)
                                                   :email (:email firebase-user)
                                                   :display-name (:display-name firebase-user)})}))
          json-data (.json response)
          data (js->clj json-data :keywordize-keys true)]
    data))
