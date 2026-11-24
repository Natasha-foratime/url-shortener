(ns url-shortener.server-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [url-shortener.db :as db]
            [url-shortener.server :as server]))

(defn with-test-handler [f]
  (let [file (java.io.File/createTempFile
              "url-shortener-http-" ".sqlite")
        database {:dbtype "sqlite"
                  :dbname (.getAbsolutePath file)}]
    (try
      (db/init-db! database)
      (f (server/make-handler database))
      (finally
        (.delete file)))))

(defn request
  ([method uri]
   {:request-method method
    :uri uri})
  ([method uri data]
   {:request-method method
    :uri uri
    :body (java.io.StringReader.
           (json/generate-string data))}))

(defn response-data [response]
  (json/parse-string (:body response) true))

(deftest create-url-test
  (with-test-handler
    (fn [handler]
      (testing "POST creates a short URL"
        (let [response (handler
                        (request :post "/api/urls"
                                 {:original_url "https://example.com"}))
              data (response-data response)]
          (is (= 201 (:status response)))
          (is (string? (:short_code data)))
          (is (= 6 (count (:short_code data)))))))))

(deftest invalid-create-test
  (with-test-handler
    (fn [handler]
      (testing "POST rejects an invalid URL"
        (let [response (handler
                        (request :post "/api/urls"
                                 {:original_url "invalid"}))]
          (is (= 400 (:status response))))))))

(deftest get-url-test
  (with-test-handler
    (fn [handler]
      (let [created (handler
                     (request :post "/api/urls"
                              {:original_url "https://example.com"}))
            code (:short_code (response-data created))
            response (handler
                      (request :get (str "/api/urls/" code)))]
        (is (= 200 (:status response)))
        (is (= "https://example.com"
               (:original_url (response-data response))))))))

(deftest update-url-test
  (with-test-handler
    (fn [handler]
      (let [created (handler
                     (request :post "/api/urls"
                              {:original_url "https://example.com"}))
            code (:short_code (response-data created))
            uri (str "/api/urls/" code)
            updated (handler
                     (request :put uri
                              {:original_url "https://openai.com"}))
            retrieved (handler (request :get uri))]
        (is (= 200 (:status updated)))
        (is (= "https://openai.com"
               (:original_url (response-data retrieved))))))))

(deftest delete-url-test
  (with-test-handler
    (fn [handler]
      (let [created (handler
                     (request :post "/api/urls"
                              {:original_url "https://example.com"}))
            code (:short_code (response-data created))
            uri (str "/api/urls/" code)
            deleted (handler (request :delete uri))
            retrieved (handler (request :get uri))]
        (is (= 200 (:status deleted)))
        (is (= 404 (:status retrieved)))))))

(deftest missing-url-test
  (with-test-handler
    (fn [handler]
      (testing "Unknown short code returns 404"
        (is (= 404
               (:status
                (handler (request :get "/api/urls/missing")))))))))

(deftest invalid-json-test
  (with-test-handler
    (fn [handler]
      (testing "Malformed JSON returns 400"
        (is (= 400
               (:status
                (handler
                 {:request-method :post
                  :uri "/api/urls"
                  :body (java.io.StringReader. "{invalid")}))))))))

(deftest unknown-route-test
  (with-test-handler
    (fn [handler]
      (testing "Unknown endpoint returns 404"
        (is (= 404
               (:status
                (handler (request :get "/unknown")))))))))
