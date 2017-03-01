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

(defent test-ent (env :datomic-db))

(deftest ent-test
  (is (= (osi-db/db-uri (env :datomic-db)) (db-uri))))

(deftest db-test
  (is (create-database (db-uri))))

(deftest schema-test
  (is @(transact (db-conn)
                 (s/generate-schema
                  [(s/schema test
                             (s/fields [uuid :uuid]))]))))

(deftest transact-test
  (is @(transact (db-conn)
                 [{:db/id (tempid :db.part/user)
                   :test/uuid (UUID/randomUUID)}])))

(deftest tx-test
  (is (:db/id (tx :test {:uuid (UUID/randomUUID)}))))

(deftest pull-test
  (is (pull (:db/id (tx :test {:uuid (UUID/randomUUID)})))))
