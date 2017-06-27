(ns osi.http.handler
  (:require [ring.util.response :as r]
            [ring.middleware.params :refer (wrap-params)]
            [ring.middleware.keyword-params :refer (wrap-keyword-params)]
            [ring.middleware.format :refer (wrap-restful-format)]
            [ring.middleware.reload :refer (wrap-reload)]
            [ring.middleware.honeybadger :refer [send-exception! wrap-honeybadger]]
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
            [new-reliquary.ring :refer (wrap-newrelic-transaction)]
            [environ.core :refer [env]]))

(def uri (or (System/getenv "OAUTH_URI")
             "https://cas-staging.optimispt.com"))

(def token (System/getenv "OCP_ACCESS_TOKEN"))

(def hb-config
  {:api-key (env :hb-key)
   :env (env :name)})

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

(defmacro w-err-hdlrs [req bod]
  `(try ~bod
        (catch clojure.lang.ExceptionInfo exc#
          (resp (gen-err-map exc#) :status 422))
        (catch Exception exc#
          (prn exc#)
          (resp (str exc#) :status 422))))

(defmacro w-parsed-req [req req-xtractr schema & bod]
  `(w-err-hdlrs ~req
    (let [~'params (parse-req ~schema (~req-xtractr ~req))]
      (when (error? ~'params)
        (throw (ex-info "Schema err" ~'params)))
      (do ~@bod))))

(defmacro route [name req-xtractr schema status & bod]
  `(defn ~name [~'req]
     (w-parsed-req ~'req ~req-xtractr ~schema
      (resp (do ~@bod) :status ~status))))

;;; TODO: generate these
(defmacro post {:style/indent :defn} [name schema & bod]
  `(route ~name :params ~schema 201 ~@bod))

(defmacro get {:style/indent :defn} [name schema & bod]
  `(route ~name :params ~schema 200 ~@bod))

(defmacro put {:style/indent :defn} [name schema & bod]
  `(route ~name :params ~schema 200 ~@bod))

(defmacro del {:style/indent :defn} [name schema & bod]
  `(route ~name :params ~schema 200 ~@bod))

(defmacro patch {:style/indent :defn} [name schema & bod]
  `(route ~name :params ~schema 200 ~@bod))

(defmacro proxy [route name schema & bod]
  `(defn ~name [~'req]
     (w-parsed-req ~'req :params ~schema
      (let [{:keys [~'body ~'status ~'headers]} @(do ~@bod)]
        (resp ~'body :status ~'status
              :type (:content-type ~'headers))))))

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

;; TODO: add wrap session
(defn hdlr [routes]
  (-> routes
      (wrap-with-logger)
      (wrap-honeybadger hb-config)
      (wrap-newrelic-transaction)
      (wrap-rby-params)
      (wrap-rby-resp)
      (wrap-restful-format
       :formats [:json :transit-json])
      site
      (wrap-reload)))
