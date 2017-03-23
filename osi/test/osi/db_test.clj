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

(defn attrs []
  {:uuid (UUID/randomUUID)
   :name (str "Foo Test" (rand))})

(defn create-foo []
  @(tx [(mke (attrs))]))

(d/create-database db-uri)

@(tx schema)

(deftest tx-test
  (testing "single ent"
    (is (pos? (:db/id @(tx [(mke (attrs))])))))
  (testing "double ent"
    (is (every? pos? (map :db/id @(tx [(mke (attrs)) (mke (attrs))]))))))

(deftest rm-test
  (let [foo (:db/id (create-foo))]
    (is (find foo))
    (rm foo)
    (is (empty? (find foo)))))
