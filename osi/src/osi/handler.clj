(ns osi.handler
  (:require [ring.util.response :as r]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [org.httpkit.client :as http]
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

(defn js&rby-compat [json]
  "Format data for js & rby"
  (json/generate-string
   (transform-keys (comp hyphen->underscore name)
                   (db/rm-ns json))))

(def uri (or (System/getenv "OAUTH_URI")
             "https://cas-staging.optimispt.com"))

(defn profile-request [access-token]
  @(http/get (str uri "/oauth2.0/profile?access_token=" access-token)))

;;; TODO: Umang, what is this used for?
(defn not-found [request]
  (prn request)
   "Not Found")

(defn wrap-api-authenticate [handler]
  (fn [request]
    (let [server-name (:server-name request)
          token-request (profile-request (get-in request [:headers "access-token"]))
          {:keys [body]} token-request
          {:keys [id]} (-> body json/parse-string clojure.walk/keywordize-keys)]
      (if (or (= "localhost" server-name) id)
        (handler request)
        (resp "Unauthorized" :status 401)))))
