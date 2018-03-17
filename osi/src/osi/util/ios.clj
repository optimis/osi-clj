(ns osi.util.ios)

(defn- make-prefix [prefix]
  {:appID prefix :paths ["*"]})

(defn- make-details [prefixes]
  (mapv make-prefix prefixes))

(defn apple-app-site-association [prefixes]
  {:activitycontinuation {:apps prefixes}
   :applinks {:apps []
              :details (make-details prefixes)}})
