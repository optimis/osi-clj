(ns osi.http.util
  (:require [clojure.string :refer [replace]]
            [clojure.walk :refer [keywordize-keys]]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys
                                underscore->hyphen
                                hyphen->underscore
                                hyphen->lower-camel]]
            [ring.util.response :as r]
            [osi.db :as db]
            [clojure.string :as str])
  (:import [java.io ByteArrayOutputStream]))

(defn ->transit [obj]
  (let [out (ByteArrayOutputStream.)]
    (trans/write (trans/writer out :json) obj)
    (.reset out)
    (.toString out "UTF-8")))

(defn <-transit [obj]
  (trans/read (trans/reader obj :json)))


(defn ->js-compat [obj]
  (transform-keys #(if (keyword? %)
                     (-> % name
                         hyphen->underscore
                         (str/replace "?" "")
                         keyword)
                     %)
                  obj))

(defn <-js-compat [obj]
  (transform-keys #(if (keyword? %)
                     (-> % name
                         underscore->hyphen
                         keyword)
                     %)
                  obj))

(defn ->rby-compat [obj]
  (transform-keys #(if (keyword? %)
                     (-> % name
                         hyphen->underscore)
                     (str %))
                  (db/rm-ns obj)))

(defn <-rby-compat [obj]
  (transform-keys (comp keyword underscore->hyphen name)
                  obj))

(defn <-json [str]
  (-> str json/parse-string <-rby-compat keywordize-keys))

(defn ->json [obj]
  (-> obj ->rby-compat json/generate-string))

(defn content-type [resp type]
  (r/content-type resp (str "application/" type)))

(defn header [resp h-map]
  (update-in resp [:headers] #(merge h-map %)))
