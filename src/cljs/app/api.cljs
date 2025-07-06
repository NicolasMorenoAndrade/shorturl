(ns app.api
  (:require [promesa.core :as p]))

(defn fetch-slug
  "Creates a shortened URL by calling the backend API.
   Returns a promise that resolves to the API response data."
  [url]
  (p/let [response (js/fetch "/api/redirect/"
                            (clj->js {:headers {:Content-Type "application/json"}
                                     :method "POST"
                                     :body (js/JSON.stringify #js {:url url})}))
          json-data (.json response)
          data (js->clj json-data :keywordize-keys true)]
    data))
