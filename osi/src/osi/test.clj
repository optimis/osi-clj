(ns osi.test
  (:require [clojure.test :refer [deftest is]]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [org.httpkit.client :as http]
            [clojure.set :refer [union]]
            [clojure.math.combinatorics :as set]
            [datomic.api :as d]
            [osi.db :refer [db]]
            [osi.http.client :refer [req]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]))

(defmacro w-srvr [app port & body]
  `(let [stop-srvr# (run-server ~app {:port ~port})]
     (let [res# (do ~@body)]
       (stop-srvr#)
       res#)))

(defn pull-all [kwd]
  (d/q `[:find ?t ?a ?tx
         :where [?a ~kwd ?t ?tx]]
       (db)))

(defn rand-datum [kwd]
  (ffirst (pull-all kwd)))

(defn rand-ref [kwd]
  (-> (pull-all kwd) first second))

(defn get [uri qry]
  (http/get (str (env :uri) uri)
            (req {} :params (into {} qry))))

(defn post [uri req-body]
  (http/post (str (env :uri) uri)
             (req (into {} req-body))))

(defn del [uri req-body]
  (http/delete (str (env :uri) uri)
               (req (into {} req-body))))

(def status {get 200 post 201 del 200})

(defn- most-inputs [inputs]
  (disj inputs (first inputs)))

(defn- merge-inputs [i1 i2s]
  (map #(into () (union (into #{} %) i1))
       i2s))

(defn req-test [http-fn uri status]
  (fn [& body]
    (let [req @(http-fn uri (if (nil? body) {}
                                (first body)))]
      (is (= status (:status req)))
      req)))

(defn req-passes? [http-fn uri]
  (req-test http-fn uri (status http-fn)))

(defn req-fails? [http-fn uri]
  (req-test http-fn uri 422))

(defn- test-inputs [inputs test]
  (doall (for [input-seq inputs]
           (test input-seq))))

(defn with-app [app port]
  (defmacro http-test {:style/indent :defn} [name & body]
    (if app
      `(deftest ~name []
         (w-srvr (~app) ~port ~@body))
      `(deftest ~name []
         ~@body))))

;;; TODO: name change + new abstraction
;;; TODO: assuming we are doing a post
(defn api-status-test
  ([api-fn] (api-status-test api-fn #{}))
  ([api-fn reqs] (api-status-test api-fn reqs #{}))
  ([api-fn reqs ops]
   (let [bad-inputs (set/subsets (union ops (most-inputs reqs)))]
     (test-inputs bad-inputs
                  (fn [item]
                    (is (= 422
                           (:status @(api-fn (into {} item))))))))
   (test-inputs (merge-inputs reqs (set/subsets ops))
                (fn [item]
                  (is (= (status post)
                         (:status @(api-fn (into {} item)))))))))

(defn resp-status-test
  ([http-fn uri] (resp-status-test http-fn uri #{}))
  ([http-fn uri reqs] (resp-status-test http-fn uri reqs #{}))
  ([http-fn uri reqs ops]           ; reqs: required inputs, ops: optional inputs
   (let [bad-inputs (set/subsets (union ops (most-inputs reqs)))]
     (when (> 1 (count bad-inputs))
         (test-inputs bad-inputs
                      (req-fails? http-fn uri))))
   (test-inputs (merge-inputs reqs (set/subsets ops))
                (req-passes? http-fn uri))))
