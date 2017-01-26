(ns osi.test
  (:require [org.httpkit.server :refer [run-server]]))

(defmacro w-srvr [app port & body]
  `(let [stop-srvr# (run-server ~app {:port ~port})]
     ~@body
     (stop-srvr#)))
