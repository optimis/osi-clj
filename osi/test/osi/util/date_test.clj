(ns osi.util.date-test
  (:require [clojure.test :refer :all]
            [osi.util.date :refer :all]))

(deftest grp-by-wk-and-date-test
  (let [col [{:date #inst "2017-01-01"}
             {:date #inst "2017-01-02"}
             {:date #inst "2017-01-08"}
             {:date #inst "2017-01-16"}]]
    (is (= [2 1 1]
           (map #(count (last %))
                (grp-by-wk-and-date col :date
                                    "2017-01-01" "2017-02-01"))))))
