(ns osi.clj)

(defmacro aif {:style/indent :defn}
  [test consq & else]
  `(let [~'it ~test]
     (if ~'it ~consq ~@else)))

(defmacro ret {:style/indent :defn} [var val & bdy]
  `(let [~var ~val] ~@bdy ~var))

(defn group-by* [fs coll]
  (if-let [f (first fs)]
    (into {} (map (fn [[k vs]]
                    [k (group-by* (next fs) vs)])
                  (group-by f coll)))
    coll))

(defn get-some [pred coll]
  (cond (empty? coll) nil
        (pred (first coll)) (first coll)
        :else (recur pred (rest coll))))

(def find get-some)

(defn split-every [n coll]
  (cond (empty? coll) coll
        (< (count coll) n) `(~coll)
        :else (let [[head tail] (split-at n coll)]
                (cons head (split-every n tail)))))

(defmacro limit
  ([coll num offset]
   `(take ~num (drop ~offset ~coll)))
  ([coll num]
   `(limit ~coll ~num 0)))

(defn- deep-merge* [& maps]
  (let [f (fn [old new]
            (if (and (map? old) (map? new))
              (merge-with deep-merge* old new)
              new))]
    (if (every? map? maps)
      (apply merge-with f maps)
      (last maps))))

(defn deep-merge [& maps]
  (let [maps (filter identity maps)]
    (assert (every? map? maps))
    (apply merge-with deep-merge* maps)))
