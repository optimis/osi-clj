(ns osi.clj)

(defmacro aif [test consq else]
  `(let [~'it ~test]
     (if ~'it ~consq ~else)))

(defmacro ret [var val & bdy]
  `(let [~var ~val] ~@bdy ~var))
