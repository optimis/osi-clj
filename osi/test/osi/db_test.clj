(ns osi.db-test
  (:require [environ.core :refer [env]]
            [osi.logger :as log]
            [osi.db :refer [defent]]
            [datomic.api :as d]
            [clojure.test :refer :all])
  (:import java.util.UUID))

(log/start!)
(d/create-database "datomic:mem://test")

(defent foo
  :db "test"
  :schema [uuid :uuid]
          [name :string :unique-identity])

(deftest db-uri-test
  (testing "ent-name"
    (is (= "foo" ent-name)))
  (testing "db-uri"
    (is (.contains (db-uri) "test")))
  (testing "schema"
    (is schema))
  (testing "transact schema"
    (is @(tx schema)))
  (testing "transact data"
    (is @(d/transact (db-conn)
                     [{:db/id (d/tempid :db.part/user)
                       :foo/uuid (UUID/randomUUID)}]))))
