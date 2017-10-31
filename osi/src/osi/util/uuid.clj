(ns osi.util.uuid
  (:require [clojure.string :refer [replace]])
  (:import java.util.UUID))

(defn- str->uuid [data]
  (UUID/fromString
   (replace data
            #"(\w{8})(\w{4})(\w{4})(\w{4})(\w{12})"
            "$1-$2-$3-$4-$5")))

(defn bytes->uuid [bytes]
  (str->uuid
   (apply str
          (for [b (take 16 bytes)]
            (format "%02x" b)))))
