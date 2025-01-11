
(ns shorturl.db
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [shorturl.env :refer [env]]))

(def db-spec
  {:dbtype "postgresql"
   :dbname (env :DBNAME)
   :host (env :HOST)
   :user (env :SHORTURL_DB_USER)
   :password (env :SHORTURL_DB_PASSWORD)
   :ssl (env :SSL)
   :sslmode (env :SSLMODE)})

(def ds (jdbc/get-datasource db-spec))

(defn query [q]
  (jdbc/execute! ds q))

(defn get-url [slug]
  (-> (query (-> (h/select :*)
                 (h/from :shortened_urls)
                 (h/where [:= :short_code slug])
                 (sql/format)))
      first
      :shortened_urls/original_url))

(defn insert-url-redirection! [url slug]
  (query (-> (h/insert-into :shortened_urls)
             (h/columns :original_url :short_code)
             (h/values [[url slug]])
             (sql/format))))

(defn remove-by-slug! [slug]
  (query (-> (h/delete-from :shortened_urls)
             (h/where [:= :short_code slug])
             (sql/format))))

(defn remove-by-url! [url]
  (query (-> (h/delete-from :shortened_urls)
             (h/where [:= :original_url url])
             (sql/format))))

(comment
;; Test connection
(jdbc/execute! ds ["SELECT 1"])

(jdbc/execute! ds ["CREATE TABLE IF NOT EXISTS shortened_urls (
                    id SERIAL PRIMARY KEY,
                    original_url TEXT NOT NULL,
                    short_code VARCHAR(10) UNIQUE NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                  )"])

(jdbc/execute! ds ["SELECT * FROM shortened_urls"])


(jdbc/execute! ds (sql/format {:select [:*]
                               :from [:shortened_urls]}))

(->
 (h/select :a :b :c)
 (h/from :foo)
 (h/where [:= :foo.a "baz"])
 (sql/format))

(jdbc/execute! ds
 (->
 (h/select :*)
 (h/from :shortened_urls)
 (sql/format)))

(query  (->
 (h/select :*)
 (h/from :shortened_urls)
 (sql/format)))


(query (-> (h/insert-into :shortened_urls)
           (h/columns :original_url :short_code)
           (h/values
            [["https://github.com/seancorfield/honeysql" "abc"]])
           (sql/format)))

(query (-> (h/insert-into :shortened_urls)
           (h/columns :original_url :short_code)
           (h/values
            [["https://www.youtube.com/watch?v=V-dBmuRsW6w&t=546s" "shorturlFE"]])
           (sql/format)))

(get-url "shorturlFE")

(query (-> (h/select :*)
           (h/from :shortened_urls)
           (h/where [:= :short_code "abc"])
           (sql/format)))

(insert-url-redirection! "https://clojure.org/releases/downloads" "clj")

;; (remove-by-slug! "shorturlFE")
(remove-by-url! "https://github.com/seancorfield/honeysql")
)
