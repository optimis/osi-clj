(defproject osi "1.9.2"
  :description "osi core library"
  :url "https://github.com/optimis/osi-clj.git"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:env/dat_usr]
                                   :password [:env/dat_passwd]}
                 "private" {:sign-releases false
                            :url "s3p://osi-leiningen/releases/"
                            :username :env/aws_access_key
                            :passphrase :env/aws_secret_key}}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/math.combinatorics "0.0.7"]
                 [environ "1.1.0"]
                 [mysql/mysql-connector-java "6.0.6"]
                 [com.datomic/datomic-pro "0.9.5544"
                  :exclusions [joda-time]]
                 [datomic-schema "1.3.0"]
                 [http-kit "2.2.0"]
                 [com.cognitect/transit-clj "0.8.297"]
                 [ring/ring "1.5.1"]
                 [ring-middleware-format "0.7.2"]
                 [spootnik/unilog "0.7.19"]
                 [ring-logger "0.7.7"]
                 [compojure "1.5.2"]
                 [cheshire "5.7.0"]
                 [prismatic/schema "1.1.3"]
                 [yleisradio/new-reliquary "1.0.0"]
                 [me.raynes/conch "0.8.0"]
                 [wharf "0.2.0-SNAPSHOT"]]
  :plugins [[lein-ancient "0.6.10"]
            [lein-cloverage "1.0.9"]
            [lein-environ "1.1.0"]
            [s3-wagon-private "1.2.0"]
            [com.carouselapps/jar-copier "0.2.0"]
            [com.andrewmcveigh/lein-auto-release "0.1.10"]]
  :scm {:dir ".."}
  :release-tasks [["auto-release" "checkout" "master"]
                  ["auto-release" "merge" "develop"]
                  ["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["ancient" "upgrade" ":interactive"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v"]
                  ["vcs" "push"]
                  ["auto-release" "checkout" "develop"]
                  ["auto-release" "merge" "master"]
                  ["change" "version"
                   "leiningen.release/bump-version"]
                  ["ancient" "upgrade" ":interactive" ":allow-snapshots"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :uberjar-name "standalone.jar"
  :prep-tasks ["javac" "compile" "jar-copier"]
  :jar-copier {:java-agents true
               :destination "resources/jars"}
  :java-agents [[com.newrelic.agent.java/newrelic-agent "3.33.0"]])
