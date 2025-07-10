(ns shorturl.env
  (:require [clojure.edn]))

(def envvars
  "Application environment variables loaded from env.edn file.

   Contains configuration settings as a map that can be accessed
   using keywords. These settings can be overridden by system
   environment variables."
  (clojure.edn/read-string (slurp "env.edn")))

(defn env
  "Retrieves an environment variable value by key.

   First looks for the key in the envvars map loaded from env.edn.
   If not found there, falls back to checking system environment
   variables, converting the key to a string name.

   Parameters:
   - k: A keyword representing the environment variable name

   Returns:
   - The value of the environment variable if found, otherwise nil"
  [k]
  (or (k envvars)
      (System/getenv (name k))))

(comment

  (System/getenv "SHORTURL_DB_PASSWORD")
  (env :DBTYPE)
  (env :SHORTURL_DB_PASSWORD))
