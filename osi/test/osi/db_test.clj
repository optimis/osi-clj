(ns osi.db-test
  (:require [environ.core :refer [env]]
            [osi.logger :as log]
            [osi.db :refer [defdb defent db-exists?]]
            [datomic.api :as d]
            [clojure.test :refer :all])
  (:import java.util.UUID))

(defent foo
  :db test
  :schema [uuid :uuid]
          [name :string :unique-identity]
          [foos :ref :many])

(defn attrs []
  {:uuid (UUID/randomUUID)
   :name (str "Foo Test" (rand))})

(defn create-foo []
  @(tx [(mke (attrs))]))

(d/create-database db-uri)

@(tx schema)

(deftest tx-test
  (testing "single ent"
    (let [txed @(tx [(mke (attrs))])]
      (is (map? txed))
      (is (pos? (:db/id txed)))))
  (testing "double ent"
    (let [txed @(tx [(mke (attrs)) (mke (attrs))])]
      (is (= 2 (count txed)))
      (is (every? pos? (map :db/id txed)))))
  (testing "nested ents"
    (let [txed @(tx [(assoc (mke (attrs)) :foo/foos [(mke (attrs))])])]
      (is (map? txed))
      (is (pos? (:db/id txed))))))

(deftest rm-test
  (let [foo (:db/id (create-foo))]
    (is (find foo))
    (rm foo)
    (is (empty? (find foo)))))
