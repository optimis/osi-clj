(ns osi.http.client
  (:require [osi.http.util :refer [->transit <-transit content-type header]]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))

(defn req
  [body & {:keys [type headers] :or {type "json" headers {}}}]
  (-> {:body body}
      (content-type type)
      (header headers)))

(defn errors? [{:keys [errors] :as resp}]
  errors)

(defn resp-body [resp]
  (if (errors? resp) {}
      (-> resp
          :body json/parse-string keywordize-keys)))

;;; TODO: not sure if needed
(defn parse-errs [resp]
  (if (errors? resp) (assoc resp :status 422)
      resp))

(def content-uri
  (or (System/getenv "CONTENT_URI") "http://localhost:4000"))

(defn pull [req]
  (http/post (str content-uri "/api") req))

(defn pull-body [resp]
  (-> @resp resp-body <-transit))


