(ns osi.math-test
  (:require [clojure.test :refer :all]
            [osi.math :refer :all]))

(deftest fuzzy=-test
  (is (fuzzy= 0.7874 0.7874)))
