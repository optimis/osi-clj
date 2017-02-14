(ns osi.pay
  (require [osi.clj :refer :all]))

(defprotocol PaymentService
  (open [x])
  (close [x])
  (charge [x])
  (charged? [x]))

(defn def-pay-process [pay-service]
  (fn [amount to pay-id]
    (ret payment (pay-service amount to pay-id)
         (dosync
          (open payment)
          (charge payment)))))
