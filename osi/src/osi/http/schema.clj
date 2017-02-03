(ns osi.http.schema
  (:require [schema.core :as s]
            [schema.coerce :as coerce]
            [osi.db :refer (sanitize)]
            [clj-time.format :as f]
            [clj-time.coerce :as c]))

(def json-matchers
  {s/Inst (comp c/to-date f/parse)
   s/Num #(if (string? %)
            (float (read-string %))
            %)
   s/Int #(if (string? %)
            (read-string %)
            %)})

(defn req-matcher [schema]
  (or (json-matchers schema)
      (coerce/json-coercion-matcher schema)))

(defn parse-req [schema req]
  ((coerce/coercer schema req-matcher)
   (sanitize req)))
