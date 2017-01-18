(ns osi.http.schema
  (:require [schema.core :as s]
            [schema.coerce :as coerce]
            [clj-time.format :as f]
            [clj-time.coerce :as c]))

(def json-matchers {s/Inst (comp c/to-date f/parse)
                    s/Num #(if (string? %)
                             (float (read-string %))
                             (float %))})

(defn req-matcher [schema]
  (or (coerce/json-coercion-matcher schema)
      (json-matchers schema)))

(defn parse-req [schema req]
  ((coerce/coercer schema req-matcher) req))
