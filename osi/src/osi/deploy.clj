(ns osi.deploy
  (:require [clojure.string :refer (join)]
            [environ.core :refer (env)]
            [wharf.core :refer (transform-keys hyphen->underscore capitalize)]
            [me.raynes.conch :refer (programs)]
            [me.raynes.conch.low-level :as ll]))

(def dckr-crt-path (str (env :home) "/Documents/Bundles/"
                        (env :dckr-crt-home) "/"))

(def dckr-opts [(str "--host=" (env :dckr-hst)) "--tls"
                (str "--tlscacert=" dckr-crt-path "ca.pem")
                (str "--tlscert=" dckr-crt-path "cert.pem")
                (str "--tlskey=" dckr-crt-path "key.pem")])

(defn dckr [cmd opts]
  (let [p (apply ll/proc (concat ["docker"] dckr-opts [cmd] opts))]
    (ll/stream-to-out p :out)))

(defn ubr-jar []
  (ll/stream-to-out (ll/proc "lein" "uberjar") :out))

(defn envvars-str [envvars]
  (->> envvars
       (select-keys env)
       (transform-keys (comp hyphen->underscore name))
       (transform-keys clojure.string/upper-case)
       (map (fn [[k v]] (str "-e " k "=" v)))
       (join " ")))

(defn deploy [app envvars]
  (let [ver "latest"
        app-ver (str app ":" ver)
        dtr-str (str "dtr.optimispt.com/optimisdev/" app-ver)]
    (prn (str "Deploying " app " to " dtr-str))
    (ubr-jar)
    (dckr "build" ["-t" app "."])
    (dckr "tag" [app-ver dtr-str])
    (dckr "push" [dtr-str])
    (dckr "pull" [dtr-str])
    (dckr "rm" ["-f" app])
    (dckr "run" ["--name" app "-d" (envvars-str envvars) "-p" "4004:4004" "--link" "redis:redis" dtr-str])))
