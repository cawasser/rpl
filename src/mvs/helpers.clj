(ns mvs.helpers
  (:require [mvs.read-models :refer :all]
            [mvs.read-model.state :as state]
            [mvs.read-model.order-sales-request-view :as v]
            [mvs.specs :refer :all]
            [clojure.spec.alpha :as spec]))


(def malformed-history (atom []))


(defn make-resource
  "utility to build :resource/definition (spec) compliant structures quickly and programmatically"

  [id [start-time end-time]]

  {:resource/id id :resource/time-frame (into [] (range start-time end-time))})


(defn associated-sales-request [order-id]
  (->> (v/order->sales-request (state/db))
    vals
    (filter #(= order-id (:order/id %)))
    first))


(defn associated-order [sales-request-id]
  (->> (v/order->sales-request (state/db))
    vals
    (filter #(= sales-request-id (:sales/request-id %)))
    first))


(defn malformed [service-name expected-spec structure]
  (swap! malformed-history
    conj {:service-name  service-name
          :expected-spec expected-spec
          :structure     structure})

  (println (str service-name " ******** MALFORMED ********  "
             expected-spec
             " // " (spec/explain-data expected-spec structure))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments


(comment
  (malformed "dummy" :resource/measurement {})

  (spec/explain-data :resource/measurement {})

  ())


; checking for malformed events
(comment
  @malformed-history
  (def resource-id #uuid"f6b50fc6-50e8-11ee-bfe7-91e057b1b221")

  (-> (mvs.read-models/state)
    mvs.read-models/resource-states
    (get resource-id))

  (-> (mvs.read-models/state)
    mvs.read-models/resource-states
    (get #uuid"72f564a6-50ed-11ee-bfe7-91e057b1b221"))

  ())

; endregion
