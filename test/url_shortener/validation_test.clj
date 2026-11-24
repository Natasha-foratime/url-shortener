(ns url-shortener.validation-test
  (:require [clojure.test :refer [deftest is testing]]
            [url-shortener.validation :as validation]))

(deftest valid-url-test
  (testing "Valid HTTP and HTTPS URLs"
    (is (true? (validation/valid-url? "https://example.com")))
    (is (true? (validation/valid-url? "http://example.com/path")))
    (is (true? (validation/valid-url? "https://example.com?q=1")))))

(deftest invalid-url-test
  (testing "Invalid URLs are rejected"
    (is (false? (validation/valid-url? "")))
    (is (false? (validation/valid-url? nil)))
    (is (false? (validation/valid-url? "example.com")))
    (is (false? (validation/valid-url? "ftp://example.com")))
    (is (false? (validation/valid-url? "https://")))
    (is (false? (validation/valid-url? "https://user:pass@example.com")))))

(deftest valid-code-test
  (testing "Valid short codes"
    (is (true? (validation/valid-code? "abc123")))
    (is (true? (validation/valid-code? "ABCD")))
    (is (true? (validation/valid-code? "a12345678901")))))

(deftest invalid-code-test
  (testing "Invalid short codes"
    (is (false? (validation/valid-code? nil)))
    (is (false? (validation/valid-code? "abc")))
    (is (false? (validation/valid-code? "abcdefghijklmn")))
    (is (false? (validation/valid-code? "abc-123")))
    (is (false? (validation/valid-code? "abc 123")))))