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

;;; TODO: templatize
(defn get
  ([uri] (get uri {}))
  ([uri params]
   (let [headers {:headers {"Content-Type" "application/json"
                            "Access-Token" (System/getenv "OCP_ACCESS_TOKEN")}}
         resp (http/get uri (merge headers params))
         {:keys [errors] :as resp} (-> @resp :body json/parse-string
                                       keywordize-keys)] ; abstract json parsing
     (if errors (assoc resp :status 422)
         resp))))

;;; TODO: Ask why we have no auth here
(defn post [uri req]
  (let [resp (http/post uri req)
        {:keys [errors] :as resp} (-> @resp :body json/parse-string
                                     keywordize-keys)]
    (if errors (assoc resp :status 422)
        resp)))

(defn delete [uri]
  @(http/delete uri))

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
