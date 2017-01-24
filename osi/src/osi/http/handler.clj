(ns osi.http.handler
  (:require [ring.util.response :as r]
            [ring.middleware.params :refer (wrap-params)]
            [ring.middleware.keyword-params :refer (wrap-keyword-params)]
            [ring.middleware.reload :refer (wrap-reload)]
            [ring.middleware.json :refer (wrap-json-response wrap-json-params)]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [ring.logger :refer (wrap-with-logger)]
            [clojure.tools.logging :refer [info]]
            [clojure.walk :refer (postwalk keywordize-keys)]
            [new-reliquary.ring :refer [wrap-newrelic-transaction]]
            [compojure.handler :refer (site)]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [org.httpkit.client :as http]
            [schema.core :refer [validate]]
            [schema.utils :refer :all]
            [osi.http.schema :refer [parse-req]]
            [osi.http.util :refer (->transit <-transit ->json <-json
                                             <-rby-compat ->rby-compat
                                             header content-type)]
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

(defn gen-err-map [err]
  (postwalk (fn [obj]
              (if (= schema.utils.ValidationError (type obj))
                (str (validation-error-explain obj))
                obj))
            (ex-data err)))

(defmacro w-err-hdlrs [bod]
  `(try ~bod
        (catch clojure.lang.ExceptionInfo exc#
          (resp (gen-err-map exc#) :status 422))
        (catch Exception exc#
          (prn exc#)                    ; TODO: use log
          (resp (str exc#) :status 422))))

(defmacro route [name req-xtractr schema & bod]
  `(defn ~name [req#]
     (w-err-hdlrs
      (let [~'obj (parse-req ~schema (~req-xtractr req#))]
        (when (error? ~'obj)
          (throw (ex-info "Schema err" ~'obj)))
        (resp (do ~@bod) :status 201)))))

(defmacro post [name schema & bod]
  `(route ~name :params ~schema ~@bod))

(defmacro get [name schema & bod]
  `(route ~name :params ~schema ~@bod))

(defmacro del [name schema & bod]
  `(route ~name :params ~schema ~@bod))

(defn rby-resp [resp]
  (if (coll? (:body resp))
    (update-in resp [:body] ->rby-compat)
    resp)) 

(defn wrap-rby-resp [hndlr]
  (fn [req]
    (rby-resp (hndlr req))))

(defn rby-params-req [req]
  (update-in req [:params] <-rby-compat))

(defn wrap-rby-params [hndlr]
  (fn [req]
    (hndlr (rby-params-req req))))

(defn hdlr [routes]
  (-> routes
      (wrap-with-logger)
      (wrap-newrelic-transaction)
      (wrap-transit-params)
      (wrap-keyword-params)
      (wrap-rby-params)
      (wrap-params)
      (wrap-json-params)
      site
      (wrap-reload)))
