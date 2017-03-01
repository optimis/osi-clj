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
  :attrs
  [uuid :uuid]
  [name :string :unique-identity])

(deftest db-uri-test
  (is (.contains (db-uri) "test")))

(create-database (db-uri))
@(transact (db-conn)
           (s/generate-schema
            [(s/schema test
                       (s/fields [uuid :uuid]))]))

(deftest transact-test
  (is @(transact (db-conn)
                 [{:db/id (tempid :db.part/user)
                   :test/uuid (UUID/randomUUID)}])))

(deftest tx-test
  (is (:db/id (tx :test {:uuid (UUID/randomUUID)}))))

(deftest pull-test
  (is (pull (:db/id (tx :test {:uuid (UUID/randomUUID)})))))
