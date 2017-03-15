(ns osi.db-test
  (:require [environ.core :refer [env]]
            [osi.logger :as log]
            [osi.db :refer [defdb defent db-exists?]]
            [datomic.api :as d]
            [clojure.test :refer :all])
  (:import java.util.UUID))

(log/start!)
(defdb test)

;(d/create-database "datomic:mem://test")

(defent foo
  :db "test"
  :schema [uuid :uuid]
          [name :string :unique-identity])

(deftest db-uri-test
  (is (db-exists? "test"))
  (is (= "foo" ent-name))
  (is schema)
  (is @(tx schema))
  (is @(d/transact db-conn
                   [{:db/id (d/tempid :db.part/user)
                     :foo/uuid (UUID/randomUUID)}])))
