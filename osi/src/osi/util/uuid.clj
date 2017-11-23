(ns osi.util.uuid
  (:require [clojure.string :refer [replace]])
  (:import
   [java.nio ByteBuffer]
   [java.util UUID]))

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

(defn bytes-to-uuid [^bytes b-a]
  (let [byte-buffer (ByteBuffer/wrap b-a)
        most-significant-bits (-> byte-buffer (.getLong 0))
        least-significant-bits (-> byte-buffer (.getLong 8))]
    (new UUID most-significant-bits least-significant-bits)))

(defn uuid-to-bytes [uuid]
  (-> (ByteBuffer/allocate 16)
      (.putLong (.getMostSignificantBits uuid))
      (.putLong (.getLeastSignificantBits uuid))
      (.array)))

(defn uuidstr->bytes [str]
  (-> str UUID/fromString uuid-to-bytes))
