(ns osi.db-test
  (:require [environ.core :refer [env]]
            [osi.db :refer [defent] :as osi-db]
            [datomic-schema.schema :as s]
            [datomic.api :refer [delete-database
                                 create-database
                                 tempid
                                 transact]]
            [clojure.test :refer :all])
  (:import java.util.UUID))

(defent test-ent
  :db "test"
  :schema [uuid :uuid]
          [name :string :unique-identity])

(deftest db-uri-test
  (testing "db-uri"
    (is (.contains (db-uri) "test")))
  (testing "create-database"
    (is (create-database (db-uri))))
  (testing "transact schema"
    (is @(transact (db-conn) schema)))
  (testing "transact"
    (is @(transact (db-conn)
                   [{:db/id (tempid :db.part/user)
                     :test/uuid (UUID/randomUUID)}]))))
