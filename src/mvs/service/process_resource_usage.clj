(ns mvs.service.process-resource-usage
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :as rm :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs :refer :all]
            [clj-uuid :as uuid]))

(def last-event (atom []))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; helpers
;

(defn- update-ktable
  "the idea here is ot keep track of all the :resource/ids that report
  metrics, count them , and report 'usage' as the percentage of the
  purchased resources that have reported.

  For example, if the order is for 5 resources and 3 of them hav reported
  at least once, the SLA will be 0.60 or 60%"

  [event]

  (rm/resource-usage-view event))


(defn- target [order-resources]
  (reduce (fn [acc {t :resource/time-frames}]
            (+ acc (count t)))
    0
    (:agreement/resources order-resources)))


(defn- compute-sla-compliance [order-resources resources-reporting]
  (println "compute-sla-compliance" resources-reporting)

  (let [usage          (or (count resources-reporting) 0.0)
        resource-count (target order-resources)
        target         (max resource-count 1.0)]

    (println "compute-sla-compliance (b)" usage target "//" resource-count)

    (double (/ usage target))))

; endregion


(defn process-resource-usage
  "we will compute 'usage' as the percent of the resources the customer purchased
  vs. those that have reported a measurement event at least once.

  uses resource-usage-view to track the :resource/ids that have reported to-date"

  [[event-key {resource-id :resource/id
               order-id    :order/id
               customer-id :customer/id
               :as         measurement}
    :as event]]

  (println "process-resource-usage" event-key " // " measurement)
  (println "process-resource-usage (b)" order-id)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do
      (let [order-resources (-> (rm/state)
                              rm/order->sales-request
                              (get order-id))
            _               (rm/resource-usage-view [event-key
                                                     (assoc measurement
                                                       :usage/function ["SLA" (partial compute-sla-compliance order-resources)]
                                                       :order-resources order-resources)])
            usage           {:customer/usage
                             (compute-sla-compliance        ; notice the implicit JOIN here
                               (get-in (rm/resource-usage (rm/state)) [customer-id order-id])
                               (get (rm/order->sales-request (rm/state)) order-id))}
            usage-event     [event-key (merge measurement usage)]]

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
    (def order-view (atom (rm/order->sales-request (rm/state))))
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


(comment
  (def measurement {:resource/id           #uuid"fe3f5d81-5116-11ee-9a65-f0935daac663",
                    :order/id              #uuid"faec3ae4-5116-11ee-9a65-f0935daac663",
                    :agreement/id          #uuid"fe3c9e60-5116-11ee-9a65-f0935daac663",
                    :measurement/attribute :googoo/metric,
                    :measurement/value     19,
                    :customer/id           #uuid"faec3ae0-5116-11ee-9a65-f0935daac663",
                    :sales/request-id      #uuid"fe35c090-5116-11ee-9a65-f0935daac663",
                    :order/needs           [0 1],
                    :measurement/id
                    #uuid"16e9ba80-511a-11ee-9a65-f0935daac663"})

  (compute-usage                                            ; notice the implicit JOIN here
    (rm/resource-usage (rm/state))
    (rm/order->sales-request (rm/state))
    measurement)


  ())

; endregion
