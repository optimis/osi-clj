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

(defn sh-cmd [cmd]
  (let [proc (apply ll/proc cmd)
        exit-code (ll/exit-code proc)]
    (ll/stream-to-out proc :out)
    (ll/stream-to-out proc :err)
    proc))

(defn dckr [cmd opts]
  (sh-cmd (concat ["docker"] dckr-opts [cmd] opts)))

(defn ubr-jar []
  (sh-cmd ["lein" "with-profile" (env :name) "uberjar" :env {"LEIN_SNAPSHOTS_IN_RELEASE" "y"}]))

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

(defn deploy
  ([app envars]
   (deploy app envars "alt"))
  ([app envvars ver]
   (let [app-name (str app "-" ver)
         app-ver (str app ":" ver)
         dtr-str (str "dtr.optimispt.com/optimisdev/" app-ver)]
     (ubr-jar)
     (dckr "login" ["-u" (env :dtr-usr) "-p" (env :dtr-pwd) "dtr.optimispt.com"])
     (dckr "build" ["-t" app "."])
     (dckr "tag" [app dtr-str])
     (dckr "push" [dtr-str])
     (dckr "rm" ["-f" app-name])
     (dckr "rmi" [app-ver])
     (dckr "rmi" [dtr-str])
     (dckr "pull" [dtr-str])
     (dckr "run" (flatten ["--name" app-name "-d" (envvars-vec envvars) ntwrk ports links dtr-str])))))
