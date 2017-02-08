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
            (req {} :params qry)))

(defn post [uri body]
  (http/post (str (env :uri) uri)
             (req body)))

(defn del [uri body]
  (http/delete (str (env :uri) uri)
               (req body)))

(def status {get 200 post 201 del 200})

(defn- most [inputs]
  (disj inputs (first inputs)))

(defn- merge-inputs [i1 i2s]
  (map #(into () (union (into #{} %) i1))
       i2s))

(defn req-test [http-fn status]
  (fn [& body]
    (let [req @(http-fn
                (if (nil? body) {}
                    (into {} (first body))))]
      (is (= status (:status req)))
      req)))

(defn req-passes? [http-type http-fn]
  (req-test http-fn (status http-type)))

(defn req-fails? [http-type http-fn]
  (req-test http-fn 422))

(defn- test-inputs [inputs test]
  (doall (for [input-seq inputs]
           (test input-seq))))

(defn with-app [app port]
  (defmacro http-test {:style/indent :defn} [name & body]
    (if app
      `(deftest ~name []
         (w-srvr (~app) ~port
                 (try ~@body
                      (catch Exception exc#
                        (prn exc#)))))
      `(deftest ~name []
         ~@body))))

(defn resp-status-test
  ([http-type fn] (resp-status-test http-type fn {}))
  ([http-type fn req] (resp-status-test http-type fn req {}))
  ([http-type fn reqs ops]
   (let [[reqs ops] (map #(into #{} %) [reqs ops])]
     (let [bad-inputs (set/subsets (union ops (most reqs)))]
       (when (> 1 (count bad-inputs))
         (test-inputs bad-inputs
                      (req-fails? http-type fn)))
       (test-inputs (merge-inputs reqs (set/subsets ops))
                    (req-passes? http-type fn))))))

(defn hdlr-tst
  ([http-fn uri] (hdlr-tst http-fn uri {}))
  ([http-fn uri reqs] (hdlr-tst http-fn uri reqs {}))
  ([http-fn uri reqs ops]
   (resp-status-test http-fn #(http-fn uri %) reqs ops)))
