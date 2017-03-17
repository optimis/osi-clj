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
          [name :string :unique-identity])

(prn schema)

(defn create-foo []
  (let [attrs {:uuid (UUID/randomUUID)
               :name "Foo Test"}]
    @(tx [(mke attrs)])))

(d/create-database db-uri)
@(tx schema)

(deftest rm-test
  (let [foo (:db/id (first (create-foo)))]
    (is (find foo))
    (rm foo)
    (is (empty? (find foo)))))
