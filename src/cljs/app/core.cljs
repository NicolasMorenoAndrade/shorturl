(ns app.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            ["react-dom/client" :as rdom]
            [app.styles :refer [styles]]
            [app.firebase :as firebase]
            [app.hooks :refer [use-click-outside]]
            [app.components :refer [AuthComponent ResultSection FormComponent]]))

(defnc app []
  (let [[state set-state] (hooks/use-state {:user {:display-name "" :email ""} :authenticated? false
                                            :slug nil :url "" :custom-slug ""
                                            :loading? false :dropdown-open? false
                                            :error nil})
        auth-button-ref (hooks/use-ref nil)
        dropdown-ref (hooks/use-ref nil)]

    ;; Initialize any necessary hooks or effects
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

    ;; Now using separate components for clarity
    (d/div {:class-name (get styles :container)}
      (d/div {:class-name (get styles :card)}
        (d/div {:class-name "relative"}
          ($ AuthComponent {:state state :set-state set-state
                            :auth-button-ref auth-button-ref :dropdown-ref dropdown-ref}))

        (d/h1 {:class-name (get styles :title)} "URL Shortener")
        (if (:slug state)
          ($ ResultSection {:state state :set-state set-state})
          ($ FormComponent {:state state :set-state set-state}))))))

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
