(ns osi.deploy
  (:require [clojure.string :refer (upper-case join)]
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
    (ll/stream-to-out p :out)
    (ll/stream-to-out p :err)))

(defn ubr-jar []
  (let [p (ll/proc "lein" "with-profile" (env :name) "uberjar")]
    (ll/stream-to-out p :out)
    (ll/stream-to-out p :err)))

(defn envvars-vec [envvars]
  (->> envvars
       (select-keys env)
       (transform-keys (comp hyphen->underscore name))
       (transform-keys upper-case)
       (map (fn [[k v]] ["-e" (str k "=" v)]))))

(def ntwrk ["--net" (env :dckr-ntwrk)])

(def ports
  ["-p" (env :dckr-ports)])

(def links
  ["--link" (env :dckr-links)])

(defn deploy [app envvars]
  (let [ver "latest"
        app-ver (str app ":" ver)
        dtr-str (str "dtr.optimispt.com/optimisdev/" app-ver)]
    (ubr-jar)
    (dckr "rm" ["-f" app])
    (dckr "rmi" [app-ver])
    (dckr "rmi" [dtr-str])
    (dckr "build" ["-t" app "."])
    (dckr "tag" [app-ver dtr-str])
    (dckr "push" [dtr-str])
    (dckr "pull" [dtr-str])
    (dckr "run" (flatten ["--name" app "-d" (envvars-vec envvars) ntwrk ports links dtr-str]))))
