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
                 [http-kit "2.2.0"]
                 [ring/ring "1.5.0"]
                 [ring-logger "0.7.6"]
                 [ring-transit "0.1.6"]
                 [com.cognitect/transit-clj "0.8.288"]
                 [compojure "1.5.1"]
                 [yleisradio/new-reliquary "1.0.0"]
                 [cheshire "5.6.3"]
                 [wharf "0.2.0-SNAPSHOT"]
                 [yleisradio/new-reliquary "1.0.0"]]
  :plugins [[s3-wagon-private "1.2.0"]
            [com.carouselapps/jar-copier "0.2.0"]]
  :prep-tasks ["javac" "compile" "jar-copier"]
  :jar-copier {:java-agents true
               :destination "resources/jars"}
  :java-agents [[com.newrelic.agent.java/newrelic-agent "3.33.0"]])
