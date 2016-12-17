(ns osi.http.client
  (:require [ring.util.response :as r]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))

(defn- content-type [type]
  (r/content-type (str "application/" type)))

(defn req
  [body & {:keys [type] :or {type "json"}}]
  (-> {:body body}
      (content-type type)))

(defn resp
  [body & {:keys [status type]
           :or {status 200 type "json"}}]
  (-> (r/response body)
      (r/status status)
      (content-type type)))

;;; TODO: templatize
(defn get
  ([uri] (get uri {}))
  ([uri params]
   (let [headers {:headers {"Content-Type" "application/json"
                            "Access-Token" (System/getenv "OCP_ACCESS_TOKEN")}}
         res (http/get uri (merge headers params))]
     (-> @res :body json/parse-string keywordize-keys)))) ; abstract json parsing

;;; TODO: Ask why we have no auth here
(defn post [uri body]
  (let [res (http/post uri (req body))]
    (-> @res :body json/parse-string keywordize-keys)))

(def content-uri
  (or (System/getenv "CONTENT_URI") "http://localhost:4000"))

;;; TODO: wrap transit
(defn pull [query]
  (let [{:keys [status body] :as request}
        @(http/post (str content-uri "/api")
                    {:as :stream
                     :headers {"Content-Type" "application/transit+json"}
                     :body (->transit query)})]
    (<-transit body)))
