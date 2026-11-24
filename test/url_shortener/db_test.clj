(ns url-shortener.db-test
  (:require [clojure.test :refer [deftest is testing]]
            [url-shortener.db :as db]))

(defn with-test-db [f]
  (let [file (java.io.File/createTempFile
              "url-shortener-test-" ".sqlite")
        test-db {:dbtype "sqlite"
                 :dbname (.getAbsolutePath file)}]
    (try
      (db/init-db! test-db)
      (f test-db)
      (finally
        (.delete file)))))

(deftest create-and-read-test
  (with-test-db
    (fn [test-db]
      (testing "Create and retrieve URL"
        (db/create-url! test-db "abc123" "https://example.com")
        (let [result (db/get-url test-db "abc123")]
          (is (= "abc123" (:short_code result)))
          (is (= "https://example.com"
                 (:original_url result))))))))

(deftest update-test
  (with-test-db
    (fn [test-db]
      (testing "Update existing URL"
        (db/create-url! test-db "abc123" "https://example.com")
        (let [result (db/update-url!
                      test-db "abc123" "https://openai.com")]
          (is (= 1 (:next.jdbc/update-count result))))
        (is (= "https://openai.com"
               (:original_url (db/get-url test-db "abc123"))))))))

(deftest delete-test
  (with-test-db
    (fn [test-db]
      (testing "Delete existing URL"
        (db/create-url! test-db "abc123" "https://example.com")
        (let [result (db/delete-url! test-db "abc123")]
          (is (= 1 (:next.jdbc/update-count result))))
        (is (nil? (db/get-url test-db "abc123")))))))

(deftest missing-url-test
  (with-test-db
    (fn [test-db]
      (testing "Unknown short code returns nil"
        (is (nil? (db/get-url test-db "missing")))))))

(deftest duplicate-code-test
  (with-test-db
    (fn [test-db]
      (testing "Duplicate short codes are rejected"
        (db/create-url! test-db "abc123" "https://example.com")
        (is (thrown? java.sql.SQLException
                     (db/create-url!
                      test-db "abc123" "https://openai.com")))))))