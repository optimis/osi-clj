(ns osi.http.client
  (:require [environ.core :refer (env)]
            [osi.http.util :refer [->transit <-transit ->json <-json content-type header]]
            [org.httpkit.client :as http]))

(def content-uri (env :content-uri))

(defn- param [req p-map]
  (update-in req [:query-params] #(merge p-map %)))

(defn req
  [body & {:keys [type headers params]
           :or {type "json" headers {} params {}}}]
  (-> (if (empty? body) {}
          {:body (if (= type "json") (->json body)
                     body)})
      (param params)
      (content-type type)
      (header headers)))

(defn errs? [{:keys [errors] :as resp}]
  errors)

(defn resp-body [resp]
  (if (errs? resp) {}
      (-> resp :body <-json)))

;;; TODO: not sure if needed
(defn parse-errs [resp]
  (if (errs? resp) (assoc resp :status 422)
      resp))

(defn pull [req]
  (http/post (str content-uri "/api") req))

(defn pull-body [resp]
  (-> @resp resp-body <-transit))


