(ns shorturl.slug
  (:require [clojure.string :as str])
  (:import [org.apache.commons.validator.routines UrlValidator DomainValidator]))

;; - No '0' (zero) vs 'O' (letter O) confusion
;; - No '1' (one) vs 'l' (lowercase L) vs 'I' (uppercase i) confusion
;; - No '5' vs 'S' confusion
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

(defn url-contains-dangerous-pattern?
  "Checks for potentially dangerous URL patterns."
  [url]
  (boolean
   (some #(str/includes? (str/lower-case url) %)
         ["javascript:" "data:" "vbscript:" "file:" "about:" "<%"])))

(defn sanitize-url [url]
  (when (and url (not (empty? url)))
    (let [trimmed (str/trim url)
          with-protocol (if (str/starts-with? trimmed "http")
                          trimmed
                          (str "https://" trimmed))
          url-validator (UrlValidator. (into-array String ["http" "https"]))
          domain-validator (DomainValidator/getInstance true)]
      (when (.isValid url-validator with-protocol)
        ;; Extract domain from URL
        (try
          (let [uri (java.net.URI. with-protocol)
                host (.getHost uri)]
            (if (and host (not (str/blank? host))
                     (.isValid domain-validator host)
                     (not (url-contains-dangerous-pattern? with-protocol)))
              with-protocol
              nil))
          (catch Exception _
            nil))))))

(comment
  (.nextInt (java.security.SecureRandom.))
  (rand-nth charset)
  (repeatedly #(rand-nth charset))
  (take 4 (repeatedly #(rand-nth charset)))
  (apply str (take 4 (repeatedly #(rand-nth charset))))
  (generate-slug)
  (valid-slug? "ABCD")
  (sanitize-custom-slug "ABCDF_-")
  (sanitize-url "https://www.hitoiki.co")
  (sanitize-url "file:")
  (sanitize-url "hjkjkjkjk")
  (sanitize-url "hitoiki.pe"))
