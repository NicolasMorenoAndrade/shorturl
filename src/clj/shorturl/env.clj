(ns shorturl.env)

(def envvars (clojure.edn/read-string (slurp "env.edn")))

(defn env [k]
  (or (k envvars)
      (System/getenv (name k))))


(comment

  (System/getenv "SHORTURL_DB_PASSWORD")
  (env :DBTYPE)
  )
