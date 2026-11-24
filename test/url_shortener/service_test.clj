(ns url-shortener.service-test
  (:require [clojure.test :refer [deftest is testing]]
            [url-shortener.db :as db]
            [url-shortener.service :as service]))

(defn with-test-db [f]
  (let [file (java.io.File/createTempFile
              "url-shortener-service-" ".sqlite")
        database {:dbtype "sqlite"
                  :dbname (.getAbsolutePath file)}]
    (try
      (db/init-db! database)
      (f database)
      (finally
        (.delete file)))))

(deftest create-short-url-test
  (with-test-db
    (fn [database]
      (testing "Valid URL is shortened and stored"
        (let [result (service/create-short-url!
                      database "https://example.com")
              code (:short-code result)]
          (is (= :created (:status result)))
          (is (string? code))
          (is (= 6 (count code)))
          (is (= "https://example.com"
                 (:original_url (db/get-url database code)))))))))

(deftest invalid-url-test
  (with-test-db
    (fn [database]
      (testing "Invalid URL is rejected"
        (is (= {:status :invalid-url}
               (service/create-short-url! database "not-a-url")))
        (is (= {:status :invalid-url}
               (service/create-short-url! database nil)))))))

(deftest collision-retry-test
  (with-test-db
    (fn [database]
      (testing "A collision triggers another attempt"
        (db/create-url! database "abc123" "https://existing.com")
        (let [codes (atom ["abc123" "xyz789"])]
          (with-redefs [service/generate-code
                        (fn []
                          (let [code (first @codes)]
                            (swap! codes rest)
                            code))]
            (let [result (service/create-short-url!
                          database "https://example.com")]
              (is (= :created (:status result)))
              (is (= "xyz789" (:short-code result)))
              (is (= "https://example.com"
                     (:original_url
                      (db/get-url database "xyz789")))))))))))

(deftest collision-limit-test
  (with-test-db
    (fn [database]
      (testing "Generation stops after five collisions"
        (db/create-url! database "abc123" "https://existing.com")
        (with-redefs [service/generate-code (constantly "abc123")]
          (is (= {:status :generation-failed}
                 (service/create-short-url!
                  database "https://example.com"))))))))
(deftest get-original-url-test
  (with-test-db
    (fn [database]
      (testing "Get an existing URL"
        (db/create-url! database "abc123" "https://example.com")
        (is (= {:status :ok
                :original-url "https://example.com"}
               (service/get-original-url database "abc123"))))
      (testing "Get a missing URL"
        (is (= {:status :not-found}
               (service/get-original-url database "missing")))))))

(deftest update-short-url-test
  (with-test-db
    (fn [database]
      (db/create-url! database "abc123" "https://example.com")
      (testing "Update an existing URL"
        (is (= {:status :ok}
               (service/update-short-url!
                database "abc123" "https://openai.com")))
        (is (= "https://openai.com"
               (:original_url (db/get-url database "abc123")))))
      (testing "Reject an invalid URL"
        (is (= {:status :invalid-url}
               (service/update-short-url!
                database "abc123" "invalid"))))
      (testing "Update a missing URL"
        (is (= {:status :not-found}
               (service/update-short-url!
                database "missing" "https://example.com")))))))

(deftest delete-short-url-test
  (with-test-db
    (fn [database]
      (db/create-url! database "abc123" "https://example.com")
      (testing "Delete an existing URL"
        (is (= {:status :ok}
               (service/delete-short-url! database "abc123")))
        (is (nil? (db/get-url database "abc123"))))
      (testing "Delete a missing URL"
        (is (= {:status :not-found}
               (service/delete-short-url! database "abc123")))))))

(deftest invalid-short-code-test
  (with-test-db
    (fn [database]
      (testing "Invalid short codes are rejected"
        (is (= {:status :not-found}
               (service/get-original-url database "a!")))
        (is (= {:status :not-found}
               (service/delete-short-url! database "a!")))
        (is (= {:status :not-found}
               (service/update-short-url!
                database "a!" "https://example.com")))))))
