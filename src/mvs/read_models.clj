(ns mvs.read-models
  (:require [mvs.constants :refer :all]
            [mvs.read-model.state :as state]
            [mvs.read-model.provider-catalog-view :as pcv]
            [mvs.read-model.resource-measurements-view :as rmv]
            [mvs.read-model.resource-state-view :as rsv]
            [mvs.read-model.sales-catalog-view :as scv]
            [clojure.spec.alpha :as spec]
            [clj-uuid :as uuid]))


(def app-db #'state/app-db)
(def initial-state #'state/initial-state)


(def provider-catalog-view #'pcv/provider-catalog-view)
(def provider-catalogs #'pcv/provider-catalogs)
(def reset-provider-catalog-view #'pcv/reset-provider-catalog-view)
(def sales-catalog-view #'scv/sales-catalog-view)
(def sales-catalog #'scv/sales-catalog)
(def reset-sales-catalog-view #'scv/reset-sales-catalog-view)
(def resource-measurements-view #'rmv/resource-measurements-view)
(def resource-measurements #'rmv/resource-measurements)
(def reset-resource-measurements-view #'rmv/reset-resource-measurements-view)
(def resource-state-view #'rsv/resource-state-view)
(def resource-states #'rsv/resource-states)
(def reset-resource-state-view #'rsv/reset-resource-state-view)



(def googoos (->> (range num-googoos)
               (map (fn [id] {:resource/id id}))
               (into [])))


(def provider-alpha {:provider/id      "alpha-googoos"
                     :resource/catalog [{:resource/type 0 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 1 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 2 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 3 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 4 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}]})
(def provider-bravo {:provider/id      "bravo-googoos"
                     :resource/catalog [{:resource/type 0 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 1 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 2 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 3 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 4 :resource/time-frames [1 3 5] :resource/cost 5}]})
(def provider-charlie {:provider/id      "charlie-googoos"
                       :resource/catalog [{:resource/type 0 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 1 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 2 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 3 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 4 :resource/time-frames [2 4 5] :resource/cost 5}]})
(def provider-delta {:provider/id      "delta-googoos"
                     :resource/catalog [{:resource/type 0 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 1 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 2 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 3 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 4 :resource/time-frames [0 1 2] :resource/cost 5}]})
(def provider-echo {:provider/id      "echo-googoos"
                    :resource/catalog [{:resource/type 0 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 1 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 2 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 3 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 4 :resource/time-frames [3 4] :resource/cost 2}]})


; TODO: consider making these functions (microservices) that do the 'swap!'
;      themselves, rather than forcing another function to do it, i.e., more
;      like 'real' KTables. wrap 'update' to the atom 'inside' the function, leaving
;      the atom itself for the 'read' side of things.
;      For example, process-sales-request uses (commit-resources) to manipulate
;      the available-resources-view KTable by remote control. This kind of logic is
;      what should be 'inside' the KTable.


; work out new logic for storing all the read-models in app-db and using "events"
; to process them
;
; NOTE: we have 2 different definitions of "events" mow:
;     1) Kafka-like tuples between services and read-models
;     2) Cljfx hash-maps for managing the state so the UI can handle it
;
;





;(def provider-catalog-view
;  "summary of all the provider catalogs" (atom {}))
;(defn reset-provider-catalog-view [] (reset! provider-catalog-view {}))

(def available-resources-view
  "denormalized arrangements of all resources available" (atom {}))
(defn reset-available-resources-view [] (reset! available-resources-view {}))

(def service-catalog-view
  "catalog of service ACME offers to customer" (atom []))
(defn reset-service-catalog-view [] (reset! service-catalog-view []))

;(def sales-catalog-history-view
;  "history of all the sales catalogs of ACME ever offered to customer" (atom []))
;(defn reset-sales-catalog-history-view [] (reset! sales-catalog-history-view []))

(def order->sales-request-view
  "maps order/id to :sales/request-id so we can relate all information" (atom {}))
(defn reset-order->sales-request-view [] (reset! order->sales-request-view {}))

(def committed-resources-view
  "all the resources that ACME has committed to customers" (atom []))
(defn reset-committed-resources-view [] (reset! committed-resources-view []))

;(def resource-state-view
;  "all the resources that ACME must monitor" (atom {}))
;(defn reset-resource-state-view [] (reset! resource-state-view {}))

(def resource-performance-view
  "track update events against resources over time"
  (atom {}))
(defn reset-resource-performance-view [] (reset! resource-performance-view {}))

(def resource-health-view
  "track heath of resources over time"
  (atom {}))
(defn reset-resource-health-view [] (reset! resource-health-view {}))

(def resource-usage-view
  "track resources that have produces any events over time, tracking the frist only"
  (atom {}))
(defn reset-resource-performance-view [] (reset! resource-performance-view {}))

(def customer-order-view (atom {}))
(defn reset-customer-order-view [] (reset! customer-order-view {}))

(def customer-agreement-view (atom {}))
(defn reset-customer-agreement-view [] (reset! customer-agreement-view {}))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; test googoo specs
(comment
  (spec/explain :resource/id (uuid/v1))
  (spec/explain :resource/type 5)
  (spec/explain :resource/time 0)
  (spec/explain :resource/attributes [5 0 "alpha"])
  (spec/explain :resource/resource {:resource/id         (uuid/v1)
                                    :resource/attributes [0 0 "bravo"]})

  ())


; test :resource/definition specs
(comment
  (spec/explain :resource/time-frames [1 3 5])
  (spec/explain :resource/definition {:resource/type        0
                                      :resource/time-frames [1 3 5]
                                      :resource/cost        5})
  (spec/explain :resource/definition {:resource/type        0
                                      :resource/time-frames [0 1 2 3 4 5]
                                      :resource/cost        10})
  (spec/explain :resource/catalog [{:resource/type        0
                                    :resource/time-frames [0 1 2 3 4 5]
                                    :resource/cost        10}
                                   {:resource/type        2
                                    :resource/time-frames [0 1 2 3 4 5]
                                    :resource/cost        10}])


  ())


; test :provider/catalog specs
(comment
  (spec/explain :provider/catalog provider-alpha)
  (spec/explain :provider/id "alpha")
  (spec/explain :resource/catalog (:resource/catalog provider-alpha))

  ())




; endregion
