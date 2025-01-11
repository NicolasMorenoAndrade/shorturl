(ns app.core)


(defn ^:export init []
  (.log js/console "howzit")
  (js/alert "hello"))
