(ns osi.db
  (:require [clojure.walk :refer (postwalk)]
            [datomic.api :as d]))

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
