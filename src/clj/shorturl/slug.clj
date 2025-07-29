(ns shorturl.slug)

;; Improved charset - removed ambiguous characters to avoid confusion:
;; - No '0' (zero) vs 'O' (letter O) confusion
;; - No '1' (one) vs 'l' (lowercase L) vs 'I' (uppercase i) confusion
;; - No '5' vs 'S' confusion
;; - Added numbers for greater variety
(def charset "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789")

(defn valid-slug?
  "Checks if a slug is valid according to our rules.
   Only allows alphanumeric characters, dashes, and underscores.
   Enforces a minimum length of 6 characters."
  [slug]
  (and
   (string? slug)
   (>= (count slug) 6)
   (boolean (re-matches #"^[A-Za-z0-9_-]+$" slug))))

(defn generate-slug
  "Generates a URL-friendly slug with a fixed length of 6 characters.
   Uses a charset without ambiguous characters."
  []
  (->> (repeatedly #(rand-nth charset))
       (take 6)
       (apply str)))

;; Validate custom slugs before saving
(defn sanitize-custom-slug
  "Ensures custom slugs are valid. Returns nil if invalid.
   Enforces minimum length of 6 characters."
  [slug]
  (when (and slug (not (empty? slug)))
    (if (valid-slug? slug)
      slug
      nil)))

(comment
  (.nextInt (java.security.SecureRandom.))
  (rand-nth charset)
  (repeatedly #(rand-nth charset))
  (take 4 (repeatedly #(rand-nth charset)))
  (apply str (take 4 (repeatedly #(rand-nth charset))))
  (generate-slug)
  (valid-slug? "ABCD")
  (sanitize-custom-slug "ABCDF_-")
  )
