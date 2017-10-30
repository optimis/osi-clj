(ns osi.http.util-test
  (:require [clojure.test :refer :all]
            [osi.http.util :refer :all]))

(deftest ->js-compat-test
  (let [ns-map {:a/b 1}
        dash-map {:a-b 1}]
    (testing "when map contains a namespace"
      (is (= {"aB" 1} (->js-compat ns-map))))
    (testing "when map contains a dash"
      (is (= {"aB" 1} (->js-compat dash-map))))))
