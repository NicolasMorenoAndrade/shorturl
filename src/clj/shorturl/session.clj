(ns shorturl.session
  (:require [ring.middleware.session.store :refer [SessionStore]]
            [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [shorturl.db :refer [ds execute-query]]))

(defn dev-mode? []
  (= (System/getenv "ENV") "development"))

;; ============================================================================
;; PostgreSQL-backed Session Store
;; ============================================================================

(defrecord PostgresSessionStore [datasource]
  SessionStore

  (read-session [_ key]
    (when key
      (-> (jdbc/execute-one! datasource
            (sql/format
              {:select [:data]
               :from [:sessions]
               :where [:= :id key]}))
          :sessions/data
          read-string)))

  (write-session [_ key data]
    (let [key (or key (str (java.util.UUID/randomUUID)))
          data-str (pr-str data)
          now (java.sql.Timestamp. (System/currentTimeMillis))]
      (jdbc/execute-one! datasource
        (sql/format
          {:insert-into :sessions
           :values [{:id key :data data-str :updated_at now}]
           :on-conflict :id
           :do-update-set {:data data-str
                          :updated_at now}}))
      key))

  (delete-session [_ key]
    (when key
      (jdbc/execute! datasource
        (sql/format
          {:delete-from :sessions
           :where [:= :id key]})))
    nil))

(defn postgres-store
  "Creates a PostgreSQL-backed session store"
  []
  (->PostgresSessionStore ds))

;; ============================================================================
;; Session Configuration
;; ============================================================================

(def session-config
  "Production-ready session configuration"
  {:store (postgres-store)
   :cookie-name "shorturl-session"
   :cookie-attrs {:http-only true      ;; Prevents XSS access to cookie
                  :secure (not (dev-mode?))         ;; HTTPS only (set to false in dev)
                  :same-site :strict   ;; Prevents CSRF attacks
                  :max-age (* 60 60 24 30)} ;; 30 days
   :root "/"})


;; ============================================================================
;; Cleanup
;; ============================================================================

(defn cleanup-old-sessions!
  "Remove sessions older than 30 days.
   Run this periodically (e.g., daily cron job)"
  []
  (execute-query
    ["DELETE FROM sessions
      WHERE updated_at < NOW() - INTERVAL '30 days'"]))

(comment
  ;; Test the session store
  (require '[shorturl.session :as session])

  ;; Cleanup old sessions
  (session/cleanup-old-sessions!)

  ;; View all sessions
  (execute-query (sql/format {:select [:*] :from [:sessions]})))
