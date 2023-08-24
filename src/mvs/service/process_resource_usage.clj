(ns mvs.service.process-resource-usage
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :refer :all]
            [mvs.specs :refer :all]
            [clj-uuid :as uuid]))

(def last-event (atom []))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; helpers
;

(defn- update-ktable
  "manage state materialization (reduce/fold update events over time) into a
  'ktable' (currently just an atom) and we'll track usage of resources "
  [ktable {resource-id :resource/id
           customer-id :customer/id
           order-id    :order/id
           :as         measurement}]

  (swap! ktable assoc-in [customer-id order-id]
    (conj (or (get-in @ktable [customer-id order-id]) #{}) resource-id)))


(defn- get-sla [ktable order-id]
  (let [agreement (get-in @ktable [order-id :agreement/resources])]
    (reduce (fn [acc {t :resource/time-frames}]
              (+ acc (count t)))
      0
      agreement)))


(defn- compute-usage [reports orders {customer-id :customer/id
                                      order-id    :order/id}]
  (let [usage (or (count (get-in @reports [customer-id order-id])) 0.0)
        sla   (or (get-sla orders order-id) 1.0)]

    {:customer/usage (double (/ usage sla))}))

; endregion


(defn process-resource-usage
  "we will compute 'usage' as the percent of the resources the customer purchased
  vs. those that have reported a measurement event at least once.

  uses a local Ktable (atom) to track the :resource/ids that have reported to-date"

  [_ _ _ [event-key {resource-id :resource/id
                     order-id    :order/id
                     customer-id :customer/id
                     :as         measurement}]]

  (println "process-resource-usage" event-key " // " measurement)
  (println "process-resource-usage (b)" order-id)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do
      (let [_           (update-ktable resource-usage-view measurement)
            usage       (compute-usage resource-usage-view
                          order->sales-request-view measurement)
            usage-event [event-key (merge measurement usage)]]

        ; what do we do with the result?
        ;
        ;  1) publish an event (pass the buck)
        ;  2) update a ktable, so it can be viewed in a dashboard
        ;
        (publish! usage-topic usage-event)))

    (malformed "process-resource-usage" :resource/measurement measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;


(comment
  (do
    (def usage-view (atom {}))
    (def order-view (atom @order->sales-request-view))
    (def customer-id #uuid"6d9bc4e0-3a4a-11ee-8473-e65ce679c38d")
    (def order-id #uuid"75f888c0-3ac3-11ee-8473-e65ce679c38d")
    (def resource-id "resource-1")

    (def agreement (get @order-view order-id)))

  ; track first time we see each resource-id
  (swap! usage-view assoc-in [customer-id order-id]
    (conj (get-in @usage-view [customer-id order-id]) resource-id))

  (swap! usage-view assoc-in [customer-id order-id]
    (conj (get-in @usage-view [customer-id order-id]) "resource-2"))


  ; new we need to figure out how many resources we owe the customer for the order
  (let [agreement (get @order-view order-id)]
    (reduce (fn [acc {t :resource/time-frames}]
              (+ acc (count t)))
      0
      (:agreement/resources agreement)))


  (do
    (def usage (or (count (get-in @usage-view [customer-id order-id])) 0.0))
    (def sla (or (get-sla order-view order-id) 1.0)))

  {:customer/usage (double (/ usage sla))}


  (do
    (def usage (or (count (get-in @usage-view [customer-id order-id])) 0.0))
    (def sla (or (get-sla order->sales-request-view order-id) 1.0)))
  ())

; endregion
