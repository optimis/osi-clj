(ns osi.clj-test
  (:require [osi.clj :refer :all]
            [clojure.test :refer :all]))

(deftest aif-test
  (testing "when no else"
    (is (aif (nil? nil) it)))
  (testing "when else"
    (is (aif (nil? nil) it 2))))

(deftest ret-test
  (is (= 1 (ret it 1 (inc it)))))

(deftest group-by*-test
  (let [col [{:a 1 :b 1}
             {:a 2 :b 2}
             {:a 3 :b 3}
             {:a 4 :b 4}]]
    (is (group-by* [:a :b] col))))

(deftest get-some-test
  (testing "when empty col"
    (is (nil? (get-some :a []))))
  (testing "when first match"
    (is (= {:a 1} (get-some :a [{:a 1}]))))
  (testing "else"
    (is (= {:a 1} (get-some :a [{:b 1} {:a 1}])))))

(deftest split-every-test
  (testing "when n > count of coll"
    (is (= '([1]) (split-every 10 [1]))))
  (testing "when n < count of coll"
    (is (= '((1 2 3) (4 5)) (split-every 3 [1 2 3 4 5])))))
