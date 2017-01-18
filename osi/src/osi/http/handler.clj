(ns osi.http.handler
  (:require [ring.util.response :as r]
            [ring.middleware.params :refer (wrap-params)]
            [ring.middleware.reload :refer (wrap-reload)]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [ring.logger :refer (wrap-with-logger)]
            [new-reliquary.ring :refer [wrap-newrelic-transaction]]
            [compojure.handler :refer (site)]
            [cognitect.transit :as trans]
            [cheshire.core :as json]
            [wharf.core :refer [transform-keys hyphen->underscore]]
            [org.httpkit.client :as http]
            [schema.core :refer [validate]]
            [osi.http.schema :refer [parse-req]]
            [osi.http.util :refer (->transit <-transit ->json <-json
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
  (-> (r/response (if (= type "json") (->json body)
                      body))
      (r/status status)
      (header headers)
      (content-type type)))

(defn req-body [req]
  (-> req :body slurp <-json))

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

(defmacro w-err-hdlrs [bod]
  `(try ~bod 
        (catch clojure.lang.ExceptionInfo exp#
          (resp "Invalid data" :status 422))
        (catch Exception exc#
          (resp "Post failed" :status 422))))

(defmacro route [name req-xtractr schema & bod]
  `(defn ~name [req#]
     (w-err-hdlrs
      (let [~'obj (->> (~req-xtractr req#)
                       (parse-req ~schema) (validate ~schema))]
        (resp (->json (do ~@bod)) :status 201)))))

(defmacro post [name schema & bod]
  `(route ~name (comp <-json slurp :body) ~schema ~@bod))

(defmacro get [name schema & bod]
  `(route ~name :params ~schema ~@bod))

(defn hdlr [routes]
  (-> routes
      (wrap-with-logger)
      (wrap-newrelic-transaction)
      (wrap-transit-params)
      (wrap-params)
      site
      (wrap-reload)))
