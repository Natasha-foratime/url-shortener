(ns url-shortener.core-test
  (:require [clojure.test :refer [deftest testing is]]))

(deftest environment-test
  (testing "Clojure test environment works"
    (is (= 2 (+ 1 1)))))