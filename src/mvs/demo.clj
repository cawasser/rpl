(ns mvs.demo
  (:require [mvs.core :refer :all]
            [mvs.constants :refer :all]
            [mvs.dashboards :refer :all]
            [mvs.demo :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-model.resource-measurements-view :as mv]
            [mvs.read-models :refer :all]
            [mvs.services :refer :all]
            [mvs.specs :refer :all]
            [mvs.topics :refer :all]
            [mvs.topology :refer :all]
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





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Scripting

(comment

  (view-topo mvs-topology)

  ; region ; 1) start the backend services and the UIs
  (do
    (reset-topology mvs-topology)
    (start-ui))


  @app-db
  ; endregion

  ; region ; 2) re-load the catalog(s)
  (do
    (publish! provider-catalog-topic [{:provider/id "alpha-googoos"} provider-alpha])
    (publish! provider-catalog-topic [{:provider/id "bravo-googoos"} provider-bravo])
    (publish! provider-catalog-topic [{:provider/id "charlie-googoos"} provider-charlie])
    (publish! provider-catalog-topic [{:provider/id "delta-googoos"} provider-delta])
    (publish! provider-catalog-topic [{:provider/id "echo-googoos"} provider-echo]))

  @app-db

  ; endregion

  ; region ; 2b) view read-models as a sanity check

  (mvs.read-model.provider-catalog-view/provider-catalogs @app-db)
  @service-catalog-view
  @available-resources-view
  @order->sales-request-view
  @resource-state-view
  @resource-performance-view
  @resource-usage-view

  ; endregion

  ; region ; 3) customers orders services
  ;(do
  ;  (def customer-1 #uuid"6d9bc4e0-3a4a-11ee-8473-e65ce679c38d")
  ;  (def customer-2 #uuid"5a9ff450-3ac3-11ee-8473-e65ce679c38d")
  ;  (def customer-3 #uuid"5a9ff451-3ac3-11ee-8473-e65ce679c38d")
  ;
  ;  (def order-1 #uuid"75f888c0-3ac3-11ee-8473-e65ce679c38d")
  ;  (def order-2 #uuid"7a8a9400-3ac3-11ee-8473-e65ce679c38d")
  ;
  ;  (def order-3 #uuid"7f6e8fd0-3ac3-11ee-8473-e65ce679c38d")
  ;  (def order-failure #uuid"84389b00-3ac3-11ee-8473-e65ce679c38d"))

  ; TODO: should we drop :order/id from the key here, leaving only :customer/id?
  (publish! customer-order-topic [{:customer/id alice :order/id alice-order-1}
                                  {:customer/id  alice
                                   :order/id     alice-order-1
                                   :order/status :order/submitted
                                   :order/needs  [0 1]}])

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
  (publish! customer-order-approval [{:order/id alice-order-1}
                                     {:agreement/id (-> @order->sales-request-view (get alice-order-1) :agreement/id)
                                      :order/id     alice-order-1
                                      :customer/id  alice
                                      :order/status :order/purchased}])

  ; approve bob-order-1
  (publish! customer-order-approval [{:order/id bob-order-1}
                                     {:agreement/id (-> @order->sales-request-view (get bob-order-1) :agreement/id)
                                      :order/id     bob-order-1
                                      :customer/id  bob
                                      :order/status :order/purchased}])

  ; endregion

  ; region (OBE) providers ship resources for order-1
  (do
    (def order-id (-> @order->sales-request-view first second :order/id))
    (def shipment-id (uuid/v1))
    (def alpha-shipment
      [{:provider/id    "alpha"}
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

  (ship/providers-ship-order alice-order-1)

  (ship/providers-ship-order bob-order-1)

  ; this should fail since order-3 cannot be committed...
  (ship/providers-ship-order carol-order-1)

  @resource-state-view

  ; endregion

  ; region ; (OBE) resources start reporting health & status (by hand)
  (do
    (def resource-id (-> @app-db mv/resource-measurements keys first)))

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
  @resource-state-view
  @resource-performance-view
  @resource-usage-view

  @performance-topic


  (reset! resource-state-view {})
  (reset! resource-usage-view {})

  ; endregion


  ; region 6a) each shipped resource reports 1 time
  (do
    ; register all the resources
    (->> @resource-state-view
      keys
      (map (fn [id] (measure/register-resource-update id
                      :googoo/metric #(measure/generate-integer 100))))
      doall))

  (measure/report-once resource-measurement-topic)

  ; endregion

  ; region ; 6b) all shipped resources start reporting automatically (every 5 sends)

  (do
    ; register all the resources
    (->> @resource-state-view
      keys
      (map (fn [id] (measure/register-resource-update id
                      :googoo/metric #(measure/generate-integer 100))))
      doall)

    ; start the background thread to publish the reports
    (measure/start-reporting resource-measurement-topic 5))

  (provider-catalogs @app-db)

  @resource-state-view
  @mvs.demo.measurement/registry
  @resource-measurement-topic
  (reset! resource-measurement-topic nil)
  (reset! mvs.demo.measurement/registry {})


  (measure/stop-reporting)

  @measurement-topic
  @health-topic
  @performance-topic
  @usage-topic


  (first @resource-state-view)
  (first @resource-performance-view)

  (double (/ (+ 58 15 70 89 0) 5))

  ; endregion

  ())


; endregion


