(ns osi.deploy
  (:require [clojure.string :refer (upper-case join)]
            [environ.core :refer (env)]
            [wharf.core :refer (transform-keys hyphen->underscore capitalize)]
            [me.raynes.conch :refer (programs)]
            [me.raynes.conch.low-level :as ll]))

(def dckr-crt-path
  (str (env :home) "/Documents/Bundles/"
       (env :dckr-crt-home) "/"))

(def dckr-opts
  [(str "--host=" (env :dckr-hst)) "--tls"
   (str "--tlscacert=" dckr-crt-path "ca.pem")
   (str "--tlscert=" dckr-crt-path "cert.pem")
   (str "--tlskey=" dckr-crt-path "key.pem")])

(defn sh-cmd [cmd]
  (let [proc (apply ll/proc cmd)
        exit-code (ll/exit-code proc)]
    (ll/stream-to-out proc :out)
    (ll/stream-to-out proc :err)
    (if (not (= 0 exit-code))
      (throw (ex-info (str "cmd failed: " (pr-str cmd))
                      {:err exit-code}))
      proc)))

(defn dckr
  ([cmd opts]
   (dckr [] cmd opts))
  ([host-opts cmd opts]
   (let [host-opts (if (= :remote host-opts)
                     dckr-opts host-opts)]
     (sh-cmd (concat ["docker"] host-opts [cmd] opts)))))

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

(defn deploy [app envvars]
  (let [ver "latest"
        app-ver (str app ":" ver)
        dtr-str (str "dtr.optimispt.com/optimisdev/" app-ver)]
    (ubr-jar)
    (dckr "login" ["-u" (env :dtr-usr) "-p" (env :dtr-pwd) "dtr.optimispt.com"])
    (dckr "build" ["-t" app "."])
    (dckr "tag" [app-ver dtr-str])
    (dckr "push" [dtr-str])
    (dckr "rm" ["-f" app])
    (dckr "rmi" [app-ver])
    (dckr "rmi" [dtr-str])
    (dckr :remote "pull" [dtr-str])
    (dckr :remote "run" (flatten ["--name" app "-d" (envvars-vec envvars) ntwrk ports links dtr-str]))))
