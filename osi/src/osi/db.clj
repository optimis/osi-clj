(ns osi.db
  (:require [clojure.walk :refer [postwalk]]
            [wharf.core :refer [transform-keys]]
            [environ.core :refer [env]]
            [datomic.api :as d]
            [datomic-schema.schema :as s]
            [wharf.core :as wharf]))

(declare db-exists? db-uri db mke-pull mke-tx)

(defn db-exists? [name]
  (let [dbs (d/get-database-names (db-uri "*"))]
    (and (not (nil? dbs))
         (.contains dbs name))))

(defn resolve-ids [db tmp-ids]
  (map #(d/resolve-tempid (db) tmp-ids %) (keys tmp-ids)))

(defmacro defdb [nm]
  `(do (def ~'db-name (name '~nm))
       (def ~'db-uri (~db-uri ~'db-name))
       (defn ~'db-conn [] (d/connect ~'db-uri))
       (defn ~'db []
         (d/db (~'db-conn)))
       (defn ~'q [q# & inputs#]
         (apply d/q q# (~'db) inputs#))
       (defn ~'mapf [col#]
         (into #{} (pmap first col#)))
       (defn ~'qf [q# & inputs#]
         (~'mapf (apply ~'q q# inputs#)))
       (defn ~'qff [q# & inputs#]
         (ffirst (apply ~'q q# inputs#)))
       (defn ~'find [eid#]
         (~'q '[:find ~'?e :in ~'$ ~'?e :where ~'[?e]] eid#))
       (def ~'pull (mke-pull db))
       (defn ~'pull-many
         ([eids#] (~'pull-many ~'['*] eids#))
         ([pat# eids#] (d/pull-many (~'db) pat# eids#)))
       (defn ~'tx
         ([data#]
          (future
            (if (some vector? data#)
              @(d/transact (~'db-conn) data#)
              (let [data# (map #(merge {:db/id (tmp-usrid)} %) data#)
                    txed# @(d/transact
                            (~'db-conn)
                            data#)
                    tempids# (:tempids txed#)
                    ret# (map (fn [tx#] (update tx# :db/id
                                               #(d/resolve-tempid (~'db) tempids# %)))
                              data#)]
                (if (= 1 (count ret#))
                  (first ret#)
                  ret#)))))
          ([data# annotation#]
           (~'tx (merge data#
                        (merge annotation#
                               {:db/id (tmp-txid)})))))
       (defn ~'rm [eid#]
         @(~'tx [[:db.fn/retractEntity eid#]]))))

(defmacro defschema [nm attrs]
  `(def ~'schema
     (s/generate-schema
      [(s/schema ~nm (s/fields ~@attrs))])))

(defmacro defattr [nm k v]
  (case k
    :db `(defdb ~(first v))
    :schema `(defschema ~nm ~v)))

(defmacro defattrs
  ([nm] nil)
  ([nm [k] v & rst]
   `(do (defattr ~nm ~k ~v)
        (defattrs ~nm ~@rst))))

(defmacro defent [nm & opts]
  `(do (def ~'ent-name (name '~nm))
       (defattrs ~nm ~@(partition-by keyword? opts))
       (defn ~'mke [attrs#]
         (add-ns attrs# ~'ent-name))))

(def db-name (env :datomic-db))

(defn db-uri
  ([] (db-uri db-name))
  ([db-name]
   (let [uri
         (case (env :datomic-storage)
           "sql" "datomic:sql://%s?jdbc:mysql://%s/datomic?serverTimezone=PST&user=datomic&password=datomic"
           "mem" "datomic:mem://%s"
           "datomic:dev://datomicdb:4334/%s")
         ip (env :datomic-storage-ip)]
     (format uri db-name ip))))

(defn db-conn
  ([] (fn [] (d/connect (db-uri))))
  ([db-name]
   (fn [] (d/connect (db-uri db-name)))))

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
  (postwalk #(if (and (map? %) (< 1 (count %))) (dissoc % :db/id) %)
            map))

(defn rm-ns [map]
  (postwalk #(if (keyword? %)
               (keyword (name %)) %)
            map))

(defn add-ns [hash-map ns]
  (into {}
        (map (fn [[k v]]
               {(keyword (str (name ns) "/" (name k))) v})
             hash-map)))

(defn update-keys
  ([map] map)
  ([map key key' & keys]
   (apply update-keys
          (wharf/transform-keys
           #(if (= key %) key' %)
           map)
          keys)))

(defn sanitize [tx]
  "Removes any keys with nil values."
  (into {} (filter (comp not nil? val)) tx))

(defn rm-empty [tx]
  (into {} (filter (comp not empty? val)) tx))

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

(defn mke-tx [db-conn db]
  (defn -tx [ent]
    (let [tx @(d/transact (db-conn)
                          [(assoc ent :db/id "tx")])]
      (d/pull (db) '[*] ((:tempids tx) "tx"))))
  (fn
    ([ent] (if (vector? ent)
             (do @(d/transact (db-conn) [ent])
                 (d/pull (db) '[*] (second ent)))
             (-tx ent)))
    ([ent attrs]
     (if (keyword? ent)
       (-tx (add-ns attrs ent))
       (let [ns (namespace (ffirst (dissoc ent :db/id)))]
         (-tx (merge ent (add-ns attrs ns))))))))

(defn mke-pull [db]
  (fn
    ([eid] (d/pull (db) '[*] eid))
    ([exp eid] (d/pull (db) exp eid))))

(defn mke-ref [db]
  (fn [exp] (->> exp
                (d/pull (db) '[:db/id])
                :db/id)))
