(ns osi.util.ios)

(defn- get-details [prefixes]
  (mapv (fn [prefix]
          {:appID prefix
           :paths ["/signup/*"]})
       prefixes))

(defn apple-app-site-association [prefixes]
    {:activitycontinuation {:apps prefixes}
     :applinks {:apps []
                :details (get-details prefixes)}})
