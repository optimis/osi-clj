(ns osi.db
  (:require [clojure.walk :refer (postwalk)]
            [environ.core :refer (env)]
            [datomic.api :as d]))

(defn db-uri
  ([] (db-uri (env :datomic-db)))
  ([db]
   (let [uri
         (case (env :datomic-storage)
           "sql" "datomic:sql://%s?jdbc:mysql://%s/datomic?serverTimezone=PST&user=datomic&password=datomic"
           "mem" "datomic:mem://%s"
           "datomic:dev://datomicdb:4334/%s")
         ip (env :datomic-storage-ip)]
     (format uri db ip))))

(defn db-conn [db-name]
  (fn [] (d/connect (db-uri db-name))))

(defn db [] (d/db (d/connect (db-uri))))

(defn delete-db []
  (d/delete-database (db-uri)))

(defn create-db []
  (d/create-database (db-uri)))

(defn tmp-usrid []
  (d/tempid :db.part/user))

(defn tmp-txid []
  (d/tempid :db.part/tx))

(defn rm-db-ids [map]
  (postwalk #(if (map? %) (dissoc % :db/id) %)
            map))

(defn rm-ns [map]
  "rm namespaces from map keys."
  (postwalk #(if (keyword? %)
               (keyword (name %)) %)
            map))

(defn sanitize [tx]
  "Removes any keys with nil values."
  (into {} (filter (comp not nil? val)) tx))

(defn quantity-tx [quantity]
  "Returns a datomix tx for a quantity."
  {:db/id (d/tempid :db.part/user)
   :quantity/name (:name quantity)})

(defn unit-tx [unit quantity]
  "Returns a datomix tx for a unit."
  (if unit
    {:db/id (d/tempid :db.part/user)
     :unit/name (:name unit)
     :unit/plural (:plural unit)
     :unit/description (:description unit)
     :unit/symbol (:symbol unit)
     :unit/system (:system unit)
     :unit/quantity (quantity-tx quantity)}))
