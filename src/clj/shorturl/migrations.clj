(ns shorturl.migrations
  (:require [shorturl.db :refer [execute-query]]
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
      [:created_at :timestamp [:default :current_timestamp]]
      [:user_id :integer [:references :users]]]})))

(defn drop-shortened-urls-table!
  "Drops the shortened_urls table if it exists.

   This will completely remove the table and all of its data.
   USE WITH CAUTION!

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
   USE WITH CAUTION!

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

;; Update the run-migrations! function to run the new migration
(defn run-migrations!
  "Runs all migrations to set up the database schema.
   This should be called during application initialization."
  []
  (create-users-table!)
  (create-shortened-urls-table!))

(comment
  ;; (drop-shortened-urls-table!)
  ;; (drop-users-table!)
  ;; TODO need better message when dropping table

  (run-migrations!)
  (create-users-table!)

  ;; (defn add-user-id-to-shortened-urls-table!
  ;;   "Adds a user_id column to the shortened_urls table as a foreign key reference to users(id)."
  ;;   []
  ;;   (execute-query
  ;;    (sql/format
  ;;     {:alter-table :shortened_urls
  ;;      :add-column [:user_id :integer [:references :users]]})))

  ;; (add-user-id-to-shortened-urls-table!)
  )
