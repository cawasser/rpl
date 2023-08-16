(ns mvs.helpers
  (:require [mvs.read-models :refer :all]))


(defn make-resource
  "utility to build :resource/definition (spec) compliant structures quickly and programmatically"

  [id [start-time end-time]]

  {:resource/id id :resource/time-frame (into [] (range start-time end-time))})


(defn associated-sales-request [order-id]
  (->> @order->sales-request-view
    vals
    (filter #(= order-id (:order/id %)))
    first))


(defn associated-order [sales-request-id]
  (->> @order->sales-request-view
    vals
    (filter #(= sales-request-id (:sales/request-id %)))
    first))


(defn malformed [service-name expected-spec & structure]
  (println (str service-name " ******** MALFORMED ********  " expected-spec " // " (first structure))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments


; endregion
