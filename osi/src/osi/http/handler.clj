(ns osi.http.handler
  (:require [ring.util.response :as r]
            [ring.middleware.params :refer (wrap-params)]
            [ring.middleware.reload :refer (wrap-reload)]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [ring.logger :refer (wrap-with-logger)]
            [compojure.handler :refer (site)]
            [osi.http.handler :refer (resp)]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [org.httpkit.client :as http]
            [osi.http.util :refer (->transit <-transit header content-type)]
            [new-reliquary.ring :refer (wrap-newrelic-transaction)]))

(def uri (or (System/getenv "OAUTH_URI")
             "https://cas-staging.optimispt.com"))

(def token (System/getenv "OCP_ACCESS_TOKEN"))

(defn profile-request [access-token]
  @(http/get (str uri "/oauth2.0/profile?access_token=" access-token)))

(defn resp
  [body & {:keys [status type headers]
           :or {status 200 type "json" headers {}}}]
  (-> (r/response body)
      (r/status status)
      (header headers)
      (content-type type)))

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

(defn hdlr [routes]
  (-> routes
      (wrap-with-logger)
      (wrap-newrelic-transaction)
      (wrap-transit-params)
      (wrap-params)
      site
      (wrap-reload)))
