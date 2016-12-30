(ns osi.http.util
  (:require [clojure.walk :refer [keywordize-keys]]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [ring.util.response :as r]
            [osi.db :as db])
  (:import [java.io ByteArrayOutputStream]))

(defn ->transit [obj]
  (let [out (ByteArrayOutputStream.)]
    (trans/write (trans/writer out :json) obj)
    (.reset out)
    (.toString out "UTF-8")))

(defn <-transit [obj]
  (trans/read (trans/reader obj :json)))

(defn ->clj-map [str]
  (-> str json/parse-string keywordize-keys))

(defn rby-compat [obj]
  (transform-keys (comp hyphen->underscore name)
                  (db/rm-ns obj)))

(defn js-compat [obj]
  "Format data for js & rby"
  (-> obj rby-compat json/generate-string))

(defn content-type [resp type]
  (r/content-type resp (str "application/" type)))

(defn header [resp h-map]
  (update-in resp [:headers] #(merge h-map %)))
