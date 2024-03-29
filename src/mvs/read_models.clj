(ns mvs.read-models
  (:require [mvs.constants :refer :all]
            [mvs.read-model.state :as state]
            [mvs.read-model.available-resources-view :as arv]
            [mvs.read-model.order-sales-request-view :as osr]
            [mvs.read-model.provider-catalog-view :as pcv]
            [mvs.read-model.resource-measurements-view :as rmv]
            [mvs.read-model.resource-performance-view :as rpv]
            [mvs.read-model.resource-state-view :as rsv]
            [mvs.read-model.resource-usage-view :as ruv]
            [mvs.read-model.sales-catalog-view :as scv]
            [clojure.spec.alpha :as spec]
            [clj-uuid :as uuid]))


(def state #'state/db)
(def initial-state #'state/initial-state)

(def available-resources-view #'arv/available-resources-view)
(def available-resources #'arv/available-resources)
(def reset-available-resources-view #'arv/reset-available-resources-view)

(def order->sales-request-view #'osr/order->sales-request-view)
(def order->sales-request #'osr/order->sales-request)
(def reset-order->sales-request-view #'osr/reset-order->sales-request-view)

(def provider-catalog-view #'pcv/provider-catalog-view)
(def provider-catalogs #'pcv/provider-catalogs)
(def reset-provider-catalog-view #'pcv/reset-provider-catalog-view)

(def sales-catalog-view #'scv/sales-catalog-view)
(def sales-catalog #'scv/sales-catalog)
(def reset-sales-catalog-view #'scv/reset-sales-catalog-view)

(def resource-measurements-view #'rmv/resource-measurements-view)
(def resource-measurements #'rmv/resource-measurements)
(def reset-resource-measurements-view #'rmv/reset-resource-measurements-view)

(def resource-performance-view #'rpv/resource-performance-view)
(def resource-performance #'rpv/resource-performance)
(def reset-resource-performance-view #'rpv/reset-resource-performance-view)

(def resource-state-view #'rsv/resource-state-view)
(def resource-states #'rsv/resource-states)
(def reset-resource-state-view #'rsv/reset-resource-state-view)

(def resource-usage-view #'ruv/resource-usage-view)
(def resource-usage #'ruv/resource-usage)
(def reset-resource-usage-view #'ruv/reset-resource-usage-view)




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


;(def resource-usage-view (atom {}))
;(defn reset-resource-usage-view [] (reset! resource-usage-view {}))

(def resource-health-view
  "track heath of resources over time"
  (atom {}))
(defn reset-resource-health-view [] (reset! resource-health-view {}))

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
