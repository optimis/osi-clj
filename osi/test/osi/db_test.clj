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

(defn- teardown []
  (when (and (= "mem" (env :datomic-storage))
             (db-exists? "test"))
    (d/delete-database db-uri)))

(defn- setup []
  (teardown)
  (d/create-database db-uri)
  @(tx schema))

(defn fx [f]
  (setup)
  (f)
  (teardown))

(use-fixtures :once fx)

(defn attrs []
  {:uuid (UUID/randomUUID)
   :name (str "Foo Test" (rand))})

(defn create-foo []
  @(tx [(mke (attrs))]))

(deftest schema-test
  (is schema))

(deftest q-test
  (let [txed @(tx [(mke (attrs))])]
    (is (not (empty? (q '[:find ?u
                          :where [?e :foo/uuid ?u]]))))))

(deftest qf-test
  (let [txed @(tx [(mke (attrs))])]
    (is (instance? java.util.UUID
                   (first (qf '[:find ?u
                                :where [?e :foo/uuid ?u]]))))))

(deftest qff-test
  (let [txed @(tx [(mke (attrs))])]
    (is (instance? java.util.UUID
                   (qff '[:find ?u
                          :where [?e :foo/uuid ?u]])))))

(deftest pull-test
  (let [txed @(tx [(mke (attrs))])]
    (is (pull (:db/id txed)))))

(deftest pull-many-test
  (let [txed @(tx [(mke (attrs))])]
    (is (pull-many [(:db/id txed)]))))

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
    (let [txed @(tx [(assoc (mke (attrs))
                            :foo/foos [(mke (attrs))])])]
      (is (map? txed))
      (is (pos? (:db/id txed))))))

(deftest rm-test
  (let [foo (:db/id (create-foo))]
    (is (ent foo))
    (rm foo)
    (is (empty? (ent foo)))))
