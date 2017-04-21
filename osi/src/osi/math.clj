(ns osi.math)

(defn fuzzy=
  ([x y] (fuzzy= 0.01 x y))
  ([tolerance x y]
   (let [diff (Math/abs (- x y))]
     (< diff tolerance))))
