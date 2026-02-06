(ns shorturl.migrations
  (:require [shorturl.db :refer [execute-query]]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]))

(defn drop-all-tables!
  "Drops all application tables in the correct order.

   WARNING: This will completely remove all tables and their data.
   USE ONLY IN DEVELOPMENT!"
  []
  (println "Dropping all tables...")
  ;; Drop in reverse dependency order
  (execute-query ["DROP TABLE IF EXISTS shortened_urls CASCADE"])
  (execute-query ["DROP TABLE IF EXISTS users CASCADE"])
  (println "All tables dropped."))
(defn create-sessions-table!
  "Creates the sessions table for persistent session storage."
  []
  (execute-query
   ["CREATE TABLE IF NOT EXISTS sessions (
       id TEXT PRIMARY KEY,
       data TEXT NOT NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
     )"])

  ;; Index for cleanup queries
  (execute-query
   ["CREATE INDEX IF NOT EXISTS idx_sessions_updated_at
     ON sessions(updated_at)"]))

(defn drop-all-tables!
  "Drops all application tables in the correct order."
  []
  (println "Dropping all tables...")
  (execute-query ["DROP TABLE IF EXISTS sessions CASCADE"])  ;; Add this line
  (execute-query ["DROP TABLE IF EXISTS shortened_urls CASCADE"])
  (execute-query ["DROP TABLE IF EXISTS users CASCADE"])
  (println "All tables dropped."))


(defn create-users-table!
  "Creates the users table if it doesn't exist."
  []
  (execute-query
   ["CREATE TABLE IF NOT EXISTS users (
       id SERIAL PRIMARY KEY,
       firebase_uid TEXT UNIQUE NOT NULL,
       email TEXT UNIQUE NOT NULL,
       display_name TEXT,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       last_login TIMESTAMP
     )"]))

(defn create-shortened-urls-table!
  "Creates the shortened_urls table if it doesn't exist."
  []
  (execute-query
   ["CREATE TABLE IF NOT EXISTS shortened_urls (
       id SERIAL PRIMARY KEY,
       original_url TEXT NOT NULL,
       slug VARCHAR(20) UNIQUE NOT NULL,
       user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
     )"]))

;; (defn run-migrations!
;;   "Runs all migrations to set up the database schema.
;;    This should be called during application initialization."
;;   []
;;   (println "Running database migrations...")
;;   (create-users-table!)
;;   (create-shortened-urls-table!)
;;   (println "Migrations complete."))

(defn run-migrations!
  "Runs all migrations to set up the database schema."
  []
  (println "Running database migrations...")
  (create-users-table!)
  (create-shortened-urls-table!)
  (create-sessions-table!)  ;; Add this line
  (println "Migrations complete."))

(comment
  ;; Development helpers

  ;; Reset entire database (DESTRUCTIVE!)
  ;; (drop-all-tables!)

  (run-migrations!)

  ;; Verify the schema
  (execute-query
   (sql/format
    {:select [:table_name :column_name :data_type :is_nullable]
     :from [:information_schema.columns]
     :where [:and
             [:in :table_name ["users" "shortened_urls" "sessions"]]
             [:= :table_schema "public"]]
     :order-by [:table_name :ordinal_position]}))

  )
