(ns osi.db
  (:require [clojure.walk :refer (postwalk)]
            [environ.core :refer (env)]
            [datomic.api :as d]))

(defn db-uri
  ([] (db-uri "assess"))
  ([db]
   (let [uri
         (case (env :datomic-storage)
           "sql"
           "datomic:sql://%s?jdbc:mysql://%s/datomic?serverTimezone=PST&user=datomic&password=datomic"
           "datomic:dev://datomicdb:4334/%s")
         ip (env :datomic-storage-ip)]
     (format uri db ip))))

(defn conn []
  (d/connect (db-uri)))

(defn tmp-usrid []
  (d/tempid :db.part/user))

(defn tmp-txid []
  (d/tempid :db.part/tx))

(defn transact [tx]
  "Transacts mutations (tx) over a datomic connection."
  @(d/transact (conn) tx))

;; TODO: unclear what this is doing
(defn entity [id]
  (d/entity (d/db (conn)) id))

(defn pull
  "Default behavior pulls all attributes of the entity.
   Optionally takes a pull expression."
  ([id]
   (d/pull (d/db (conn)) '[*] id))
  ([id exp]
   (d/pull (d/db (conn)) exp id)))

(defn pull-many [exp col]
  (d/pull-many (d/db (conn)) exp col))

(defn q
  "Executes a datomic query using the latest db value.
   Optionally takes query arguments and a pull expression."
  ([q]
   (d/q q (d/db (conn))))
  ([q args]
   (d/q q (d/db (conn)) args))
  ([q args exp]
   (d/q q (d/db (conn)) args exp)))

(defn hq
  "Executes a datomic query using the history db value.
  Optionally takes query arguments and a pull expression."
  ([q]
   (d/q q (d/history (d/db (conn)))))
  ([q args]
   (d/q q (d/history (d/db (conn))) args))
  ([q args exp]
   (d/q q (d/history (d/db (conn))) args exp)))

(defn rm-db-ids [map]
  (postwalk #(if (map? %) (dissoc % :db/id) %)
            map))

(defn rm-ns [map]
  "rm namespaces from map keys."
  (postwalk #(if (keyword? %)
               (keyword (name %)) %)
            (rm-db-ids map)))

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
