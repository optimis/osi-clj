(ns osi.clj)

(defmacro aif {:style/indent :defn}
  [test consq & else]
  `(let [~'it ~test]
     (if ~'it ~consq ~else)))

(defmacro ret [var val & bdy]
  `(let [~var ~val] ~@bdy ~var))

(defn group-by* [fs coll]
  (if-let [f (first fs)]
    (into {} (map (fn [[k vs]]
                    [k (group-by* (next fs) vs)])
                  (group-by f coll)))
    coll))
