(ns osi.http.util
  (:require [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [osi.db :as db])
  (:import [java.io ByteArrayOutputStream]))

(defn ->transit [obj]
  (let [out (ByteArrayOutputStream.)]
    (trans/write (trans/writer out :json) obj)
    (.toString out "UTF-8")))

(defn <-transit [obj]
  (trans/read (trans/reader obj :json)))

(defn js-compat [json]
  "Format data for js & rby"
  (json/generate-string
   (transform-keys (comp hyphen->underscore name)
                   (db/rm-ns json))))
