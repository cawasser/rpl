(ns mvs.read-models
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [clojure.spec.alpha :as spec]
            [clj-uuid :as uuid]))


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

(def resource-measurement-init {#uuid"53cf7c01-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 0},
                                                                             :resource/measurements {:googoo/metric [76 61 55 74]},
                                                                             :provider/id           "delta-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cfca21-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 2},
                                                                             :resource/measurements {},
                                                                             :provider/id           "alpha-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cf7c00-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 1},
                                                                             :resource/measurements {},
                                                                             :provider/id           "delta-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cfca23-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 4},
                                                                             :resource/measurements {},
                                                                             :provider/id           "alpha-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cf7c02-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 1},
                                                                             :resource/measurements {},
                                                                             :provider/id           "delta-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cfca22-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 3},
                                                                             :resource/measurements {},
                                                                             :provider/id           "alpha-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cfca25-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 3},
                                                                             :resource/measurements {},
                                                                             :provider/id           "alpha-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cfca24-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 2},
                                                                             :resource/measurements {},
                                                                             :provider/id           "alpha-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cfca26-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 4},
                                                                             :resource/measurements {},
                                                                             :provider/id           "alpha-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cf54f1-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 0},
                                                                             :resource/measurements {},
                                                                             :provider/id           "delta-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cff131-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 5},
                                                                             :resource/measurements {},
                                                                             :provider/id           "bravo-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]},
                                #uuid"53cff132-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 5},
                                                                             :resource/measurements {},
                                                                             :provider/id           "bravo-googoos",
                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
                                                                             :order/needs           [0 1]}})
(def initial-state {:provider-catalog-view      {}
                    :available-resources-view   {}
                    :service-catalog-view       []
                    :sales-catalog-view         []
                    :order->sales-request-view  {}
                    :committed-resources-view   []
                    :resource-measurements-view resource-measurement-init
                    :resource-performance-view  {}
                    :resource-usage-view        {}
                    :customer-order-view        {}
                    :customer-agreement-view    {}})

(def app-db (atom (fx/create-context initial-state cache/lru-cache-factory)))



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

(def resource-state-view
  "all the resources that ACME must monitor" (atom {}))
(defn reset-resource-state-view [] (reset! resource-state-view {}))

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
