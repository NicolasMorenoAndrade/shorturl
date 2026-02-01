(ns shorturl.migrations
  (:require [shorturl.db :refer [execute-query
                                 add-user-id-column-to-shortened-urls!]]
            [honey.sql :as sql]))

(defn create-shortened-urls-table!
  "Creates the shortened_urls table if it doesn't exist.

   This table stores URL redirections with columns for:
   - id: auto-incrementing primary key
   - original_url: the destination URL
   - slug: the unique slug/identifier for the short URL
   - created_at: timestamp when the record was created

   Returns:
   - The result of the create table operation"
  []
  (execute-query
   (sql/format
    {:create-table [:shortened_urls :if-not-exists]
     :with-columns
     [[:id :serial [:primary-key]]
      [:original_url :text [:not nil]]
      [:slug [:varchar 10] [:not nil] [:unique]]
      [:created_at :timestamp [:default :current_timestamp]]]})))

(defn drop-shortened-urls-table!
  "Drops the shortened_urls table if it exists.

   This will completely remove the table and all of its data.
   USE WITH CAUTION!, especially in production environments.

   Parameters:
   - cascade: (optional) If true, drops dependent objects like constraints. Default: false

   Returns:
   - The result of the drop table operation"
  ([]
   (execute-query
    (sql/format
     {:drop-table [:if-exists :shortened_urls]}))))

(defn create-users-table!
  "Creates the users table if it doesn't exist.

   This table stores user information with columns for:
   - id: auto-incrementing primary key
   - firebase_uid: unique identifier from Firebase Authentication
   - email: user's email address
   - display_name: user's display name from Firebase profile
   - created_at: timestamp when the user record was created
   - last_login: timestamp of the user's most recent login

   Returns:
   - The result of the create table operation"
  []
  (execute-query
   (sql/format
    {:create-table [:users :if-not-exists]
     :with-columns
     [[:id :serial [:primary-key]]
      [:firebase_uid :text [:not nil] [:unique]]
      [:email :text [:not nil] [:unique]]
      [:display_name :text]
      [:created_at :timestamp [:default :current_timestamp]]
      [:last_login :timestamp]]})))

(defn drop-users-table!
  "Drops the users table if it exists.

   This will completely remove the table and all of its data.
   USE WITH CAUTION!, especially in production environments.

   Parameters:
   - cascade: (optional) If true, drops dependent objects like constraints. Default: false

   Returns:
   - The result of the drop table operation"
  ([]
   (execute-query
    (sql/format
     {:drop-table [:if-exists :users]})))

  ([cascade]
   (execute-query
    (sql/format
     {:drop-table [:if-exists :users]
      :cascade cascade}))))

(defn run-migrations!
  "Runs all migrations to set up the database schema.
   This should be called during application initialization."
  []
  (create-shortened-urls-table!)
  (create-users-table!)
  (add-user-id-column-to-shortened-urls!));

(comment
  ;; (drop-shortened-urls-table!)
  ;; (drop-users-table!)
  ;; TODO need better message when dropping table

  (run-migrations!)
  (create-users-table!)


  ;; Verify the schema
  (require '[shorturl.db :as db])

  (db/execute-query
   (honey.sql/format
    {:select [:column_name :data_type :is_nullable]
     :from [:information_schema.columns]
     :where [:and
             [:= :table_name "shortened_urls"]
             [:= :table_schema "public"]]}))

  )
