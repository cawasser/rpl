(ns mvs.service.process-customer-order
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-customer-order
  "we:

   1) assign an ACME :sale/request-id to this request
   2) enrich with the actual resources associated with the chosen services

   and pass it along

   we also associate the :order-id with the :sales/request-id generated here, so we
   can find all the relevant data as we need
   "
  [_ _ _ [event-key order]]

  (println "process-customer-order" (:order/id order))

  (reset! last-event order)

  (if (spec/valid? :customer/order order)
    ; region ; handle :customer/order
    (let [request-id (uuid/v1)
          resources  (->> order
                       :order/needs
                       (mapcat (fn [service-id]
                                 (:service/elements
                                   (first
                                     (filter #(= (:service/id %) service-id) @service-catalog-view)))))
                       (into []))]

      (println "process-customer-order (b) " (:order/needs order) " // " resources)

      (if (not-empty resources)
        (do
          ; 1) store the mapping from the :order/id to the :sales/request-id
          (swap! order->sales-request-view assoc
            (:order/id order) (assoc order :sales/request-id request-id))

          ; 2) publish the :sales/request or send the customer some kind of Error

          (publish! sales-request-topic [{:sales/request-id request-id
                                          :order/id         (:order/id order)
                                          :customer/id      (:customer/id order)}
                                         {:sales/request-id request-id
                                          :request/status   :request/submitted
                                          :order/id         (:order/id order)
                                          :customer/id      (:customer/id order)
                                          :order/needs      (:order/needs order)
                                          :sales/resources  resources}]))

        (println "process-customer-order !!!!!! Error Error Error !!!!!! "
          (:order/id order) " // " (:order/needs order))))
    ; endregion

    (malformed "process-customer-order" :customer/order order)))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; check specs for :customer/order
(comment
  (spec/explain :customer/needs [0 1])
  (spec/explain :customer/order {:customer/id (uuid/v1)
                                 :order/id    (uuid/v1)
                                 :order/needs [0 1]})

  ())


; process-customer-order ->  build a :sales/request from a :customer/request
(comment
  (do
    (def order-id (uuid/v1))
    (def service-request-id (uuid/v1))

    (def order @last-event)
    (def order {:sales/request-id service-request-id
                :request/status   :request/submitted
                :customer/id      (uuid/v1)
                :order/id         order-id
                :order/needs      [0 1]
                :sales/resources  [{:resource/type 0 :resource/time-frames [0 1 2 3 4 5]}
                                   {:resource/type 1 :resource/time-frames [0 1 2 3 4 5]}]})

    (def service-id 0)
    (def local-view (atom [])))

  (spec/explain :customer/order order)

  (->> order
    :order/needs
    (mapcat (fn [service-id]
              (:service/elements
                (first
                  (filter #(= (:service/id %) service-id) @service-catalog-view)))))
    (into []))

  (swap! local-view conj (assoc order :sales/request-id service-request-id))

  ())


; send an "error" if the customer needs are incorrect (asking for something that
; doesn't exist, etc.)
(comment
  (do
    (def order-id (uuid/v1))
    (def customer-id (uuid/v1))
    (def order {:customer/id (uuid/v1)
                :order/id    order-id
                :order/needs [20]}))

  (->> order
    :order/needs
    (mapcat (fn [service-id]
              (:service/elements
                (first
                  (filter #(= (:service/id %) service-id) @service-catalog-view)))))
    (into []))

  (process-customer-order [] [] [] [{:order/id customer-id :customer/id customer-id}
                                    order])

  ())



; update order->sales-request-view with the new order->service-request mapping
(comment
  (do
    (def local-order->sales-request-view (atom {}))
    (def order-id (uuid/v1))
    (def order {:order/id order-id})
    (def request-id (uuid/v1)))


  (swap! local-order->sales-request-view
    assoc order-id (assoc order :sales/request-id request-id))


  ())

; endregion


