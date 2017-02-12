(ns osi.http.handler-test
  (:require [clojure.test :refer :all]
            [clojure.walk :refer (keywordize-keys)]
            [environ.core :refer (env)]
            [compojure.core :refer :all]
            [org.httpkit.client :as http]
            [osi.http.client :refer (req)]
            [osi.http.handler :refer (hdlr) :as hdlr]
            [osi.test :refer :all]
            [schema.core :as s]
            [cheshire.core :as json]))

(defn homepage [request]
  (req "ok"))

(hdlr/post build {:foo s/Str} params)

(defroutes app-routes
  (GET "/site" [] homepage)
  (POST "/build" [] build))

(defn app []
  (hdlr app-routes))

(with-app app 5678)

(http-test wrap-restful-format-test
  (testing "JSON params and response"
    (let [prms {:foo "bar"}]
      (is (= prms
             (-> (:body @(post "/build" prms))
                 json/parse-string
                 keywordize-keys))))))
