(ns shorturl.slug)

(def charset "ABCDEFGHJKLMONPQRSTUVWXYZ")

(defn generate-slug
  "Generates a slug of length 4 by default.
  Keep the slug generation simple"
  ([]
   (generate-slug 4))
  ([slug-length]
   (->> (repeatedly #(rand-nth charset))
        (take slug-length)
        (apply str))))

(comment
  (.nextInt (java.security.SecureRandom.))
  (rand-nth charset)
  (repeatedly #(rand-nth charset))
  (take 4 (repeatedly #(rand-nth charset)))
  (apply str (take 4 (repeatedly #(rand-nth charset))))
  (generate-slug))
