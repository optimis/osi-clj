(ns osi.http.client
  (:require [osi.http.util :refer [->transit <-transit]]
            [ring.util.response :as r]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))

(defn- content-type [resp type]
  (r/content-type resp (str "application/" type)))

(defn- header [resp h-map]
  (update-in resp [:headers] #(merge h-map %)))

(defn req
  [body & {:keys [type headers] :or {type "json" headers {}}}]
  (-> {:body body}
      (content-type type)
      (header headers)))

(defn resp
  [body & {:keys [status type headers]
           :or {status 200 type "json" headers {}}}]
  (-> (r/response body)
      (r/status status)
      (header headers)
      (content-type type)))

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


