(ns osi.deploy-test
  (:require [clojure.test :refer :all]
            [osi.deploy :refer :all]
            [me.raynes.conch.low-level :as ll]))

(deftest ubr-jar-test
  (is (= 0 (ll/exit-code (ubr-jar)))))

(deftest npm-test
  (is (npm-init!)))
