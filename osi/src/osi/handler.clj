(ns osi.handler
  (:require [ring.util.response :as r]
            [cognitect.transit :as trans])
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
