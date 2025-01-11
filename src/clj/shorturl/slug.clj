(ns shorturl.slug)

(def charset "ABCDEFGHJKLMONPQRSTUVWXYZ")

(defn generate-slug []
  (->> (repeatedly #(rand-nth charset))
       (take 4)
       (apply str))
  )

(comment
  (rand-nth charset)
  (repeatedly #(rand-nth charset))
  (take 4 (repeatedly #(rand-nth charset)))
  (apply str (take 4 (repeatedly #(rand-nth charset))))
  (generate-slug)
  )
