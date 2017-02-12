(ns osi.logger
  (:require [unilog.config :refer (start-logging!)]))

(defn start! [& cfg]
  (let [default-cfg {:level "info"
                     :console true}]
    (start-logging!
     (merge default-cfg
            (:logging cfg)))))
