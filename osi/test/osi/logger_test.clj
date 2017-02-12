(ns osi.logger-test
  (:require [osi.logger :refer :all]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]))

(deftest start-test
  (start!)
  (is (nil? (log/debug "message"))))
