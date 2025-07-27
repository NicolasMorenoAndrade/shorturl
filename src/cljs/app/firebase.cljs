(ns app.firebase
  (:require
   ["firebase/app" :as firebase]
   ["firebase/auth" :refer [GoogleAuthProvider getAuth signInWithPopup onAuthStateChanged signOut]]))

(defn init []
  ;; initialize firebase app. runs once.
  (when (zero? (alength (firebase/getApps)))
    (println "Initialize firebase")
    (firebase/initializeApp #js
                             {:apiKey "AIzaSyDWD9vnhxblGXvMeY5Yf4AdcMBrOUW52iw",
                              :authDomain "shorturl-project-6431d.firebaseapp.com",
                              :projectId "shorturl-project-6431d",
                              :storageBucket "shorturl-project-6431d.firebasestorage.app",
                              :messagingSenderId "657813974014",
                              :appId "1:657813974014:web:adec6b656897c7c95c2eaf",
                              :measurementId "G-LXB2J3B5B0"})))

(defn google-sign-in []
  (let [provider (GoogleAuthProvider.)
        auth (getAuth)]
    (signInWithPopup auth provider)))

(defn sign-out []
  (signOut (getAuth)))



(defn set-user! [set-state]
  "Updates application state with Firebase user information.

   Parameters:
   - set-state: Function to update state (from React's useState)

   The function sets up an auth state listener that will:
   - Store user information when signed in
   - Clear user when signed out"
  ;; Make sure Firebase is initialized first
  (init)
  (let [auth (getAuth)]
    (onAuthStateChanged
     auth
     (fn [user]
       (if user
         ;; User is signed in
         (let [user-data {:uid (.-uid user)
                          :email (.-email user)
                          :display-name (.-displayName user)}]
           (set-state (fn [current-state]
                        (assoc current-state
                               :user user-data
                               :authenticated? true))))

         ;; User is signed out
         (set-state (fn [current-state]
                      (assoc current-state
                             :user nil
                             :authenticated? false))))))))

(comment
  (println firebase)
  (firebase/getApps)
  (init)
  (println GoogleAuthProvider)
  (google-sign-in))
