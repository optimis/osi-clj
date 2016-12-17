(ns osi.http.handler
  (:require [ring.util.response :as r]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [org.httpkit.client :as http]
            [osi.http.util :refer (->transit)]))


(def uri (or (System/getenv "OAUTH_URI")
             "https://cas-staging.optimispt.com"))

(def token (System/getenv "OCP_ACCESS_TOKEN"))

(defn profile-request [access-token]
  @(http/get (str uri "/oauth2.0/profile?access_token=" access-token)))

(defn api-call
  [{:keys [status body]}]
  (let [query (trans/read (trans/reader body :json))]
    (resp (->transit (into {} query))
          :type "transit+json")))

(defn not-found [request]
  (prn request)
   "Not Found")

(defn wrap-api-authenticate [hdlr]
  (fn [req]
    (let [srvr-nme (:server-name req)
          tok-req (profile-request (get-in req [:headers "access-token"]))
          {:keys [body]} tok-req
          {:keys [id]} (-> body json/parse-string clojure.walk/keywordize-keys)]
      (if (or (= "localhost" srvr-nme) id)
        (hdlr req)
        (resp "Unauthorized" :status 401)))))

(def hdlr [routes]
  (-> routes
      (wrap-with-logger)
      (wrap-newrelic-transaction)
      (wrap-transit-params)
      (wrap-params)
      site
      (wrap-reload)))
