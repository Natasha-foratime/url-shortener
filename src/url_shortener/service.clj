(ns url-shortener.service
  (:require [url-shortener.db :as db]
            [url-shortener.validation :as validation]))

(def alphabet
  "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")

(defn generate-code []
  (apply str
         (repeatedly 6
           #(nth alphabet
                 (rand-int (count alphabet))))))

(defn create-short-url! [database original-url]
  (if-not (validation/valid-url? original-url)
    {:status :invalid-url}
    (loop [attempt 0]
      (if (>= attempt 5)
        {:status :generation-failed}
        (let [code (generate-code)
              result (try
                       (db/create-url! database code original-url)
                       :created
                       (catch java.sql.SQLException e
                         (if (db/get-url database code)
                           :collision
                           (throw e))))]
          (if (= result :collision)
            (recur (inc attempt))
            {:status :created
             :short-code code}))))))
(defn get-original-url [database short-code]
  (if-not (validation/valid-code? short-code)
    {:status :not-found}
    (if-let [record (db/get-url database short-code)]
      {:status :ok
       :original-url (:original_url record)}
      {:status :not-found})))

(defn update-short-url! [database short-code original-url]
  (cond
    (not (validation/valid-code? short-code))
    {:status :not-found}

    (not (validation/valid-url? original-url))
    {:status :invalid-url}

    :else
    (let [result (db/update-url! database short-code original-url)]
      (if (pos? (:next.jdbc/update-count result))
        {:status :ok}
        {:status :not-found}))))

(defn delete-short-url! [database short-code]
  (if-not (validation/valid-code? short-code)
    {:status :not-found}
    (let [result (db/delete-url! database short-code)]
      (if (pos? (:next.jdbc/update-count result))
        {:status :ok}
        {:status :not-found}))))
