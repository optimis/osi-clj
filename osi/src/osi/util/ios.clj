(ns osi.util.ios)

(defn apple-app-site-association [ios-prefix]
  (let [appID (str ios-prefix ".com.osi.optimumMe")
        oldAppId "75YPV78L8R.com.optimiscorp.optimumme.rc"]
    {:activitycontinuation {:apps [appID oldAppId]}
     :applinks {:apps []
                :details [{:appID appID
                           :paths ["/signup/*"]},
                          {:appId oldAppId
                           :paths ["/signup/*"]}]}}))
