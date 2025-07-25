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

;; [[:id :serial [:primary-key]]
;;       [:original_url :text [:not nil]]
;;       [:slug [:varchar 10] [:not nil] [:unique]]
;;  [:created_at :timestamp [:default :current_timestamp]]]

(defn create-users-table!
  "Creates the users table if it doesn't exist"
  []
  (execute-query
   (sql/format
    {:create-table [:users :if-not-exists]
     :with-columns
     [[:id :serial [:primary-key]]
      [:email :text [:not nil] [:unique]]
      [:password_hash :text [:not nil]]
      [:created_at :timestamp [:default :current-timestamp]]]})))

(defn run-migrations!
  "Runs all migrations to set up the database schema.
   This should be called during application initialization."
  []
  (create-shortened-urls-table!))

(comment
  ;; (drop-shortened-urls-table!)
  ;; TODO need better message when dropping table
  (run-migrations!)
  (create-users-table!))
