(ns osi.logger
  (:require [environ.core :refer (env)]
            [unilog.config :refer (start-logging!)]))

(defonce default-cfg
  {:level "info"
   :console false})

(defn start!
  ([] (start! default-cfg))
  ([cfg] (start-logging! cfg)))
