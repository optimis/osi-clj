(ns osi.handler
  (:require [ring.util.response :as r]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [osi.db :as db])
  (:import [java.io ByteArrayOutputStream]))

(defn ->transit [obj]
  (let [out (ByteArrayOutputStream.)]
    (trans/write (trans/writer out :json) obj)
    (.toString out "UTF-8")))

(defn resp
  [body & {:keys [status type]
           :or {status 200 type "json"}}]
  (-> (r/response body)
      (r/status status)
      (r/content-type (str "application/" type))))

(defn api-call
  [{:keys [status body]}]
  (let [query (trans/read (trans/reader body :json))]
    (resp (->transit (into {} query))
          :type "transit+json")))

(defn ->js&rby [json]
  "Format data for js & rby"
  (json/generate-string
   (transform-keys (comp hyphen->underscore name)
                   (db/rm-ns json))))
