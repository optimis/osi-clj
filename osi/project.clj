(defproject osi "0.1.0-SNAPSHOT"
  :description "osi core library"
  :url "https://github.com/optimis"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:env/dat_usr]
                                   :password [:env/dat_psk]}
                 "private" {:sign-releases false
                            :url "s3p://osi-leiningen/releases/"
                            :username :env/lein_repo_usr
                            :passphrase :env/lein_repo_psk}}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-pro "0.9.5394" :exclusions [joda-time]]
                 [ring/ring "1.5.0"]
                 [com.cognitect/transit-clj "0.8.288"]
                 [cheshire "5.6.3"]
                 [wharf "0.2.0-SNAPSHOT"]])
