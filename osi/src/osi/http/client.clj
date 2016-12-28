(ns osi.http.client
  (:require [environ.core :refer (env)]
            [osi.http.util :refer [->transit <-transit ->clj-map content-type header]]
            [org.httpkit.client :as http]))

(def content-uri (env :content-uri))

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

(defn pull [req]
  (http/post (str content-uri "/api") req))

(defn pull-body [resp]
  (-> @resp resp-body <-transit))


