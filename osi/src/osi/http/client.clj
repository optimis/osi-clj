(ns osi.http.client
  (:require [osi.http.util :refer [->transit <-transit ->clj-map content-type header]]
            [org.httpkit.client :as http]))

(defn req
  [body & {:keys [type headers] :or {type "json" headers {}}}]
  (-> {:body body}
      (content-type type)
      (header headers)))

(defn errs? [{:keys [errors] :as resp}]
  errors)

(defn resp-body [resp]
  (if (errs? resp) {}
      (-> resp :body ->clj-map)))

;;; TODO: not sure if needed
(defn parse-errs [resp]
  (if (errs? resp) (assoc resp :status 422)
      resp))

(def content-uri
  (or (System/getenv "CONTENT_URI") "http://localhost:4000"))

(defn pull [req]
  (http/post (str content-uri "/api") req))

(defn pull-body [resp]
  (-> @resp resp-body <-transit))


