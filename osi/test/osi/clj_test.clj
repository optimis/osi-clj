(ns osi.clj-test
  (:require [osi.clj :refer :all]
            [clojure.test :refer :all]))

(deftest aif-test
  (testing "when no else"
    (is (aif (nil? nil) it)))
  (testing "when else"
    (is (aif (nil? nil) it 2))))
