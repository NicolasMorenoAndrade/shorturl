(ns app.hooks
  (:require [helix.hooks :as hooks]))

(defn use-click-outside
  "Hook that calls the callback when a click occurs outside the referenced elements.
   Takes a vector of refs and a callback function to execute on outside click."
  [refs callback]
  (hooks/use-effect
   [callback]
   (let [handle-click (fn [event]
                        (when (every? #(and % (not (.contains (.-current %) (.-target event)))) refs)
                          (callback)))]
     (.addEventListener js/document "mousedown" handle-click)
     #(.removeEventListener js/document "mousedown" handle-click))))

;; You could later add other hooks here:
;; (defn use-auth-state [...] ...)
;; (defn use-form-validation [...] ...)
;; etc.
