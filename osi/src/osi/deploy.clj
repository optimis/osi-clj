(ns osi.deploy
  (:require [clojure.string :refer
             [split upper-case replace join]]
            [environ.core :refer [env]]
            [wharf.core :refer [transform-keys hyphen->underscore capitalize]]
            [me.raynes.conch :refer [programs]]
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
    proc))

(defn dckr
  ([cmd opts]
   (dckr [] cmd opts))
  ([host-opts cmd opts]
   (let [host-opts (if (= :remote host-opts)
                     dckr-opts host-opts)]
     (sh-cmd (concat ["docker"] host-opts [cmd] opts)))))

(defn ubr-jar []
  (sh-cmd ["lein" "with-profile" (env :name) "uberjar"
           :env {"LEIN_SNAPSHOTS_IN_RELEASE" "y"}])
  (sh-cmd ["cp" "target/standalone.jar" "ops/standalone.jar"]))

(defn envvars-vec [envvars]
  (->> envvars
       (select-keys env)
       (transform-keys (comp hyphen->underscore name))
       (transform-keys upper-case)
       (map (fn [[k v]] ["-e" (str k "=" v)]))))

(def ntwrk ["--net" (env :dckr-ntwrk)])

(def ports
  ["-p" (env :dckr-ports)])

(defn link [link]
  ["--link" link])

(def links
  (map link
       (split (env :dckr-links)
              #" ")))

(defn npm-build! []
  (sh-cmd ["npm" "run" "build" :dir (env :npm)]))

(defn push [app envvars]
  (let [ver "latest"
        app-ver (str app ":" ver)
        dtr-str (str (env :dckr-uri) "/" app-ver)]
    (when (contains? envvars :npm)
      (npm-build!))
    (ubr-jar)
    (dckr "login" ["-u" (env :dtr-usr) "-p" (env :dtr-pwd) "dtr.optimispt.com"])
    (dckr "rmi" [app-ver])
    (dckr "build" ["-t" app "." :dir "ops"])
    (dckr "tag" [app-ver dtr-str])
    (dckr "push" [dtr-str])))

(defn deliver [app envvars]
  (let [ver "latest"
        app-ver (str app ":" ver)
        dtr-str (str (env :dtr-usr) "/" app-ver)]
    (dckr :remote "rm" ["-f" app])
    (dckr "rmi" [dtr-str])
    (dckr :remote "pull" [dtr-str])
    (dckr :remote "run" (flatten ["--name" app "-d" (envvars-vec envvars) ntwrk ports links dtr-str]))))

(defn deploy [app envvars]
  (let [args (drop 1 *command-line-args*)]
    (if (or (empty? args)
            (some #(= "push" %) args))
      (push app envvars))
    (if (or (empty? args)
            (some #(= "deliver" %) args))
      (deliver app envvars))))
