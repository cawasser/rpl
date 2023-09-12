(ns mvs.demo
  (:require [mvs.core :as mvs]
            [mvs.constants :refer :all]
            [mvs.dashboards :refer :all]
            [mvs.demo :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-model.state :as state]
            [mvs.read-models :as rm :refer :all]
            [mvs.services :refer :all]
            [mvs.specs :refer :all]
            [mvs.topics :as t]
            [mvs.topics :refer :all]
            [mvs.topology :as topo]
            [mvs.demo.measurement :as measure]
            [mvs.demo.shipment :as ship]
            [clojure.spec.alpha :as spec]
            [clj-uuid :as uuid]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Defining Entity IDs for the various organizations in the Demo

; :customer/id
(def alice (uuid/v1))
(def bob (uuid/v1))
(def carol (uuid/v1))
(def dave (uuid/v1))

; :order/id
(def alice-order-1 (uuid/v1))
(def alice-order-2 (uuid/v1))
(def bob-order-1 (uuid/v1))
(def carol-order-1 (uuid/v1))
(def dave-order-1 (uuid/v1))
(def dave-order-2 (uuid/v1))

; :provider/id
(def alpha-googoos "alpha-googoos")
(def bravo-googoos "bravo-googoos")
(def charlie-googoos "charlie-googoos")
(def delta-googoos "delta-googoos")
(def echo-googoos "echo-googoos")


; endregion



(defn reset-read-models
  "reset all the read-models to their starting value (generally, empty)"
  []
  (state/reset)

  (reset-available-resources-view)
  ;(reset-service-catalog-view)
  ;(reset-sales-catalog-history-view)
  (reset-resource-state-view)
  (reset-available-resources-view)
  (reset-resource-performance-view)
  (reset-customer-order-view)
  (reset-customer-agreement-view))


(defn reset-topology [topo]
  (reset-read-models)
  (topo/init-topology topo))



(defn step-1 []
  (reset-topology mvs/mvs-topology))


(defn step-2 []
  (publish! provider-catalog-topic [{:provider/id "alpha-googoos"} provider-alpha])
  (publish! provider-catalog-topic [{:provider/id "bravo-googoos"} provider-bravo])
  (publish! provider-catalog-topic [{:provider/id "charlie-googoos"} provider-charlie])
  (publish! provider-catalog-topic [{:provider/id "delta-googoos"} provider-delta])
  (publish! provider-catalog-topic [{:provider/id "echo-googoos"} provider-echo]))


(defn step-3 []
  (publish! customer-order-topic [{:customer/id alice :order/id alice-order-1}
                                  {:customer/id  alice
                                   :order/id     alice-order-1
                                   :order/status :order/submitted
                                   :order/needs  [0 1]}]))


(defn step-4 []
  (publish! customer-order-approval [{:order/id alice-order-1}
                                     {:agreement/id (-> (rm/order->sales-request (rm/state))
                                                      (get alice-order-1) :agreement/id)
                                      :order/id     alice-order-1
                                      :customer/id  alice
                                      :order/status :order/approved}]))


(defn step-5 []
  (ship/providers-ship-order alice-order-1))


(defn step-6a []
  (->> (mvs.read-model.resource-state-view/resource-states @mvs.read-model.state/app-db)
    keys
    (map (fn [id] (measure/register-resource-update id
                    :googoo/metric #(measure/generate-integer 100))))
    doall)

  (measure/report-once resource-measurement-topic))


(defn step-6b []
  ; register all the resources
  (->> (mvs.read-model.resource-state-view/resource-states @mvs.read-model.state/app-db)
    keys
    (map (fn [id] (measure/register-resource-update id
                    :googoo/metric #(measure/generate-integer 100))))
    doall)

  ; start the background thread to publish the reports
  (measure/start-reporting resource-measurement-topic 5))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Scripting

(comment

  (topo/view-topo mvs/mvs-topology)

  ; region ; 1) start the backend services and the UIs
  (step-1)
  (mvs/start-ui)


  (state/db)
  ; endregion

  ; region ; 2) re-load the catalog(s)
  (step-2)

  (state/db)

  ; endregion

  ; region ; 2b) view read-models as a sanity check

  (mvs.read-model.provider-catalog-view/provider-catalogs (state/db))
  (mvs.read-model.sales-catalog-view/sales-catalog (state/db))
  (mvs.read-model.available-resources-view/available-resources (state/db))
  (mvs.read-model.order-sales-request-view/order->sales-request (state/db))
  (mvs.read-model.resource-state-view/resource-states (state/db))
  (mvs.read-model.resource-performance-view/resource-performance (state/db))
  @resource-usage-view

  ; endregion

  ; region ; 3) customers orders services

  ; TODO: should we drop :order/id from the key here, leaving only :customer/id?
  (step-3)

  (publish! customer-order-topic [{:customer/id bob :order/id bob-order-1}
                                  {:customer/id  bob
                                   :order/id     bob-order-1
                                   :order/status :order/submitted
                                   :order/needs  [0 1]}])

  ; this one "fails" because we've used all the 0/0 and 0/1 Googoos in the "warehouse"
  (publish! customer-order-topic [{:customer/id carol :order/id carol-order-1}
                                  {:customer/id  carol
                                   :order/id     carol-order-1
                                   :order/status :order/submitted
                                   :order/needs  [0 1]}])

  ; this one "errors" because it asks for nonsense service ids
  (publish! customer-order-topic [{:customer/id dave :order/id dave-order-1}
                                  {:customer/id  dave
                                   :order/id     dave-order-1
                                   :order/status :order/submitted
                                   :order/needs  [20]}])

  ; endregion

  ; region ; 4) customers agree to successful orders (order-1 & order-2 above) i.e., :order/approval
  ;
  ; approve alice-order-1
  (step-4)

  ; approve bob-order-1
  (publish! customer-order-approval [{:order/id bob-order-1}
                                     {:agreement/id (-> (rm/order->sales-request (rm/state))
                                                      (get bob-order-1) :agreement/id)
                                      :order/id     bob-order-1
                                      :customer/id  bob
                                      :order/status :order/approved}])

  ; endregion

  ; region (OBE) providers ship resources for order-1
  (do
    (def order-id (-> (rm/order->sales-request (rm/state)) first second :order/id))
    (def shipment-id (uuid/v1))
    (def alpha-shipment
      [{:provider/id "alpha"}
       {:shipment/id    shipment-id
        :order/id       order-id
        :provider/id    "alpha"
        :shipment/items [{:resource/id (uuid/v1) :resource/type 0 :resource/time 0}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 1}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 2}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 3}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 4}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 5}]}])

    (spec/explain :shipment/line-item {:resource/id (uuid/v1) :resource/type 0 :resource/time 0})
    (spec/explain :provider/shipment (second alpha-shipment))

    (publish! shipment-topic alpha-shipment))


  ; endregion

  ; region ; 5) build the "real" :provider/shipments for the :customer/orders

  (step-5)

  (ship/providers-ship-order bob-order-1)

  ; this should fail since order-3 cannot be committed...
  (ship/providers-ship-order carol-order-1)

  (mvs.read-model.resource-state-view/resource-states (mvs.read-model.state/db))

  ; endregion

  ; region ; (OBE) resources start reporting health & status (by hand)
  (do
    (def resource-id (-> (state/db) mv/resource-measurements keys first)))

  (publish! resource-measurement-topic [{:resource/id resource-id}
                                        {:measurement/id        (uuid/v1)
                                         :resource/id           resource-id
                                         :measurement/attribute :googoo/metric
                                         :measurement/value     10}])

  (publish! resource-measurement-topic [{:resource/id resource-id}
                                        {:measurement/id        (uuid/v1)
                                         :resource/id           resource-id
                                         :measurement/attribute :googoo/metric
                                         :measurement/value     90}])

  (publish! resource-measurement-topic [{:resource/id resource-id}
                                        {:measurement/id        (uuid/v1)
                                         :resource/id           resource-id
                                         :measurement/attribute :googoo/metric
                                         :measurement/value     110}])

  (publish! resource-measurement-topic [{:resource/id resource-id}
                                        {:measurement/id        (uuid/v1)
                                         :resource/id           resource-id
                                         :measurement/attribute :googoo/metric
                                         :measurement/value     50}])

  (publish! resource-measurement-topic [{:resource/id resource-id}
                                        {:measurement/id        (uuid/v1)
                                         :resource/id           resource-id
                                         :measurement/attribute :googoo/metric
                                         :measurement/value     0}])
  (mvs.read-model.resource-state-view/resource-states @mvs.read-model.state/app-db)
  (mvs.read-model.resource-performance-view/resource-performance @mvs.read-model.state/app-db)
  @resource-usage-view

  @performance-topic


  (reset! resource-state-view {})
  (reset! resource-usage-view {})

  ; endregion


  ; region 6a) each shipped resource reports 1 time
  (do
    ; register all the resources
    (->> (mvs.read-model.resource-state-view/resource-states @mvs.read-model.state/app-db)
      keys
      (map (fn [id] (measure/register-resource-update id
                      :googoo/metric #(measure/generate-integer 100))))
      doall))

  (measure/report-once resource-measurement-topic)

  ; endregion

  ; region ; 6b) all shipped resources start reporting automatically (every 5 sends)

  (step-6b)

  (provider-catalogs (state/db))

  (mvs.read-model.resource-state-view/resource-states (mvs.read-model.state/db))
  @mvs.demo.measurement/registry
  @resource-measurement-topic
  (reset! resource-measurement-topic nil)
  (reset! mvs.demo.measurement/registry {})


  (measure/stop-reporting)

  (first (mv/resource-measurements (state/db)))
  @health-topic
  @performance-topic
  @usage-topic


  (first (mvs.read-model.resource-state-view/resource-states @mvs.read-model.state/app-db))
  (first (mvs.read-model.resource-performance-view/resource-performance @mvs.read-model.state/app-db))

  (double (/ (+ 58 15 70 89 0) 5))

  ; endregion

  ())


; through step 3 - customer orders services
(comment
  (do
    (step-1)
    (step-2)
    (step-3))

  (rm/state)

  ())


; through step 4 - customer approved agreement
(comment
  (do
    (step-1)
    (step-2)
    (step-3)
    (step-4))

  (rm/state)
  (rm/order->sales-request (rm/state))

  ())


; through step 5 - providers ship resources
(comment
  (do
    (step-1)
    (step-2)
    (step-3)
    (step-4)
    (step-5))

  (rm/state)
  (rm/resource-states (rm/state))
  (rm/resource-performance (rm/state))

  ())


; through step 6a - resources start reporting metrics
(comment
  (do
    (step-1)
    (step-2)
    (step-3)
    (step-4)
    (step-5)
    (step-6a))

  (rm/state)
  (rm/resource-performance (rm/state))
  (rm/resource-usage (rm/state))

  @mvs.topics/measurement-topic

  ())




; endregion


