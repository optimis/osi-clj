(ns osi.util.date
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.periodic :as tp]
            [clj-time.format :as f]))

(defn- <-str [t]
  (f/parse (f/formatters :date) t))

(defn- within? [start end t]
  (t/within? (t/interval start end) t))

(defn- within-wk? [wk date]
  (within? wk (t/plus wk (t/weeks 1))
           (c/to-date-time date)))

(defn- wk-idxs-in-range [itm date-key range]
  (map-indexed
   (fn [idx wk]
     (when (within-wk? wk (get itm date-key))
       (+ 1 idx)))
   range))

(defn- assoc-wk [itm date-key range]
  (assoc itm :wk
         (wk-idx-in-range itm date-key range)))

(defn time-range [start end step]
  (let [inf-range (tp/periodic-seq start step)]
    (take-while #(within? start end %) inf-range)))

(defn wk-idx-in-range [itm date-key range]
  (first
   (filter (comp not nil?)
           (wk-idxs-in-range itm date-key range))))

(defn grp-by-wk [col date-key strt end]
  (let [range (time-range (<-str strt) (<-str end)
                          (t/weeks 1))]
    (->> (map #(assoc-wk % date-key range) col)
         (group-by :wk))))

(defn grp-by-wk-and-date [col date-key strt end]
  (->> (grp-by-wk col date-key strt end)
       (map (fn [[wk grp]]
              [wk (group-by date-key grp)]))))
