(ns url-shortener.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def db-spec
  {:dbtype "sqlite"
   :dbname "urls.db"})

(defn init-db!
  ([] (init-db! db-spec))
  ([db]
   (jdbc/execute-one!
    db
    ["CREATE TABLE IF NOT EXISTS urls (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        short_code TEXT NOT NULL UNIQUE,
        original_url TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      )"])))

(defn create-url!
  ([short-code original-url]
   (create-url! db-spec short-code original-url))
  ([db short-code original-url]
   (jdbc/execute-one!
    db
    ["INSERT INTO urls (short_code, original_url) VALUES (?, ?)"
     short-code original-url])))

(defn get-url
  ([short-code]
   (get-url db-spec short-code))
  ([db short-code]
   (jdbc/execute-one!
    db
    ["SELECT * FROM urls WHERE short_code = ?" short-code]
    {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-url!
  ([short-code original-url]
   (update-url! db-spec short-code original-url))
  ([db short-code original-url]
   (jdbc/execute-one!
    db
    ["UPDATE urls SET original_url = ? WHERE short_code = ?"
     original-url short-code])))

(defn delete-url!
  ([short-code]
   (delete-url! db-spec short-code))
  ([db short-code]
   (jdbc/execute-one!
    db
    ["DELETE FROM urls WHERE short_code = ?" short-code])))