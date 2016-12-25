(ns osi.http.wrap
  (:require [osi.http.handler :refer [req-body]]
            [clojure.instant :as inst])
  (:import [org.httpkit.BytesInputStream]))

(defn- cnvtr-hdlr [hdlr keys cnvtr]
  (fn [req]
    (let [req* (update-in req [:params] merge
                          (reduce (fn [p-map key]
                                    (if (contains? p-map key)
                                      (update p-map key cnvtr)
                                      p-map))
                                  (select-keys (req-body req) keys)
                                  keys))]
      (.reset (:body req*))
      (hdlr req*))))

(defn wrap-date-params [keys]
  (fn [hdlr]
    (cnvtr-hdlr hdlr keys #(inst/read-instant-date %))))

(defn wrap-uuid-params [keys]
  (fn [hdlr]
    (cnvtr-hdlr hdlr keys #(java.util.UUID/fromString %))))

