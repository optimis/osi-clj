(ns osi.util.ios)

(defn apple-app-site-association [ios-prefix]
  (let [appID (str ios-prefix ".com.osi.optimumMe")]
    {:activitycontinuation {:apps [appID]}
     :applinks {:apps []
                :details [{:appID appID
                           :paths ["*"]}]}}))
