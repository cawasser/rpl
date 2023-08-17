(ns mvs.core
  (:require [mvs.commands :refer :all]
    [mvs.constants :refer :all]
    [mvs.dashboards :refer :all]
    [mvs.events :refer :all]
    [mvs.helpers :refer :all]
    [mvs.read-models :refer :all]
    [mvs.services :refer :all]
    [mvs.specs :refer :all]
    [mvs.topics :refer :all]
    [clojure.spec.alpha :as spec]
    [clj-uuid :as uuid]
    [loom.graph :as lg]
    [loom.io :as lio]))


(set! *print-namespace-maps* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Wiring (a Willa-like DSL using 'topics' and 'ktables')
;
;  wire the services together using watchers on the various atoms

(def mvs-wiring {:mvs/entities {:provider-catalog-topic       {:mvs/entity-type :mvs/topic :mvs/topic-name provider-catalog-topic}
                                :provider-order-topic         {:mvs/entity-type :mvs/topic :mvs/topic-name provider-order-topic}
                                :sales-catalog-topic          {:mvs/entity-type :mvs/topic :mvs/topic-name sales-catalog-topic}
                                :customer-order-topic         {:mvs/entity-type :mvs/topic :mvs/topic-name customer-order-topic}
                                :sales-request-topic          {:mvs/entity-type :mvs/topic :mvs/topic-name sales-request-topic}
                                :sales-commitment-topic       {:mvs/entity-type :mvs/topic :mvs/topic-name sales-commitment-topic}
                                :sales-failure-topic          {:mvs/entity-type :nvs/topic :mvs/topic-name sales-failure-topic}
                                :customer-order-approval      {:mvs/entity-type :mvs/topic :mvs/topic-name customer-order-approval}
                                :plan-topic                   {:mvs/entity-type :mvs/topic :mvs/topic-name plan-topic}
                                :shipment-topic               {:mvs/entity-type :mvs/topic :mvs/topic-name shipment-topic}
                                :resource-measurement-topic   {:mvs/entity-type :mvs/topic :mvs/topic-name resource-measurement-topic}

                                :service-catalog-view         {:mvs/entity-type :mvs/ktable :mvs/topic-name service-catalog-view}
                                :committed-resource-view      {:mvs/entity-type :mvs/ktable :mvs/topic-name committed-resources-view}
                                :resource-state-view          {:mvs/entity-type :mvs/ktable :mvs/topic-name resource-state-view}

                                :customer-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'customer-dashboard}
                                :provider-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'provider-dashboard}
                                :monitoring-dashboard         {:mvs/entity-type :mvs/dashboard :mvs/name #'monitoring-dashboard}

                                :process-available-resources  {:mvs/entity-type :mvs/service :mvs/name #'process-available-resources}
                                :process-provider-catalog     {:mvs/entity-type :mvs/service :mvs/name #'process-provider-catalog}
                                ;:process-sales-catalog       {:mvs/entity-type :mvs/service :mvs/name #'process-sales-catalog}
                                :process-customer-order       {:mvs/entity-type :mvs/service :mvs/name #'process-customer-order}
                                :process-sales-request        {:mvs/entity-type :mvs/service :mvs/name #'process-sales-request}
                                :process-sales-commitment     {:mvs/entity-type :mvs/service :mvs/name #'process-sales-commitment}
                                :process-order-approval       {:mvs/entity-type :mvs/service :mvs/name #'process-order-approval}

                                :process-plan                 {:mvs/entity-type :mvs/service :mvs/name #'process-plan}
                                :process-shipment             {:mvs/entity-type :mvs/service :mvs/name #'process-shipment}
                                :process-measurement          {:mvs/entity-type :mvs/service :mvs/name #'process-measurement}
                                :process-resource-usage       {:mvs/entity-type :mvs/service :mvs/name #'process-resource-usage}
                                :process-resource-health      {:mvs/entity-type :mvs/service :mvs/name #'process-resource-health}
                                :process-resource-performance {:mvs/entity-type :mvs/service :mvs/name #'process-resource-performance}}

                 :mvs/workflow [[:provider-catalog-topic :process-provider-catalog]
                                [:provider-catalog-topic :process-available-resources]
                                [:process-provider-catalog :sales-catalog-topic]
                                [:sales-catalog-topic :customer-dashboard]

                                [:customer-order-topic :process-customer-order]
                                [:customer-order-approval :process-order-approval]
                                [:sales-request-topic :process-sales-request]

                                [:sales-commitment-topic :process-sales-commitment]
                                [:sales-failure-topic :process-sales-commitment]
                                [:sales-agreement-topic :customer-dashboard]

                                [:process-order-approval :plan-topic]
                                [:plan-topic :process-plan]

                                [:committed-resource-view :process-order-approval]

                                [:process-customer-order :sales-request-topic]
                                [:process-sales-request :sales-commitment-topic]
                                [:process-sales-request :sales-failure-topic]
                                [:process-sales-commitment :sales-agreement-topic]

                                [:process-plan :provider-order-topic]
                                [:process-plan :resource-state-view]
                                [:provider-order-topic :provider-dashboard]

                                [:shipment-topic :process-shipment]

                                [:resource-measurement-topic :process-measurement]
                                [:resource-state-view :process-measurement]
                                [:resource-state-view :monitoring-dashboard]
                                [:process-measurement :resource-state-view]

                                [:resource-state-view :process-resource-usage]
                                [:resource-state-view :process-resource-health]
                                [:resource-state-view :process-resource-performance]

                                [:customer-dashboard :customer-order-topic]
                                [:customer-dashboard :customer-order-approval]]})



(defn init-topology [wiring]
  (let [entities (:mvs/entities wiring)
        workflow (:mvs/workflow wiring)]
    (doall
      (map (fn [[from to]]
             (when (= (-> entities from :mvs/entity-type) :mvs/topic)
               (do
                 (println "add-watch " from " -> " to)
                 (add-watch (-> entities from :mvs/topic-name)
                   to (-> entities to :mvs/name)))))
        workflow))))


(defn view-topo [{:keys [mvs/entities mvs/workflow] :as topo}]
  (-> (lg/digraph)
    (#(apply lg/add-nodes % (keys entities)))
    (#(apply lg/add-edges % workflow))
    (lio/view)))


(defn reset-topology [topo]
  (reset! provider-catalog-view {})
  (reset! available-resources-view {})
  (reset! service-catalog-view {})
  (reset! order->sales-request-view {})
  (reset! resource-state-view {})

  (init-topology topo)

  ; providers 'publish' catalogs

  (do
    (publish! provider-catalog-topic [{:provider/id "alpha"} provider-alpha])
    (publish! provider-catalog-topic [{:provider/id "bravo"} provider-bravo])
    (publish! provider-catalog-topic [{:provider/id "charlie"} provider-charlie])
    (publish! provider-catalog-topic [{:provider/id "delta"} provider-delta])
    (publish! provider-catalog-topic [{:provider/id "echo"} provider-echo])))


(comment
  (view-topo mvs-wiring)

  (init-topology mvs-wiring)

  ())




; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DESIGN CONSIDERATIONS
;
; TODO: should we use the existing "customer-order-topic" for :customer/order, :sales/agreement
;       and :order/approval?
;           Advantages:
;             - everything concerning an Order is in one topic, not scattered across several
;           Disadvantages:
;             - hard to implement using atom (no consumer index tracking, like Kafka)
;
; TODO: if so, do we have just 1 service which handles everything (turning the current functions
;       into helpers? OR, do we hang multiple services off one topics and each one must filter
;       based upon some property inside the event itself?
;
; TODO: This same question arises between each pair of "domains":
;          customer <-> sales
;          sales <-> planning
;          etc.
;



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Scripting

(comment

  (view-topo mvs-wiring)

  ; 1) start
  (reset-topology mvs-wiring)



  (init-topology mvs-wiring)

  ; region ; reset everything and re-load the catalog(s)
  (do
    (reset! provider-catalog-view {})
    (reset! available-resources-view {})
    (reset! service-catalog-view {})
    (reset! order->sales-request-view {})

    (init-topology mvs-wiring)

    ; providers 'publish' catalogs

    (do
      (publish! provider-catalog-topic [{:provider/id "alpha"} provider-alpha])
      (publish! provider-catalog-topic [{:provider/id "bravo"} provider-bravo])
      (publish! provider-catalog-topic [{:provider/id "charlie"} provider-charlie])
      (publish! provider-catalog-topic [{:provider/id "delta"} provider-delta])
      (publish! provider-catalog-topic [{:provider/id "echo"} provider-echo])))

  ; endregion


  @provider-catalog-view
  @available-resources-view
  @order->sales-request-view
  @service-catalog-view
  @resource-state-view


  ; region ; 2) customers request services
  (do
    (def customer-1 #uuid"6d9bc4e0-3a4a-11ee-8473-e65ce679c38d")
    (def customer-2 #uuid"5a9ff450-3ac3-11ee-8473-e65ce679c38d")
    (def customer-3 #uuid"5a9ff451-3ac3-11ee-8473-e65ce679c38d")

    (def order-1 #uuid"75f888c0-3ac3-11ee-8473-e65ce679c38d")
    (def order-2 #uuid"7a8a9400-3ac3-11ee-8473-e65ce679c38d")

    (def order-3 #uuid"7f6e8fd0-3ac3-11ee-8473-e65ce679c38d")
    (def order-failure #uuid"84389b00-3ac3-11ee-8473-e65ce679c38d"))

  (publish! customer-order-topic [{:order/id order-1 :customer/id customer-1}
                                  {:order/id    order-1 :customer/id customer-1
                                   :order/needs [0 1]}])

  (publish! customer-order-topic [{:order/id order-2 :customer/id customer-2}
                                  {:order/id    order-2 :customer/id customer-2
                                   :order/needs [0 1]}])

  ; this one "fails" because we've used all the 0/0 and 0/1 Googoos
  (publish! customer-order-topic [{:customer/id customer-3 :order/id order-3}
                                  {:customer/id customer-3 :order/id order-3
                                   :order/needs [0 1]}])

  ; this one "errors" because it asks for nonsense service ids
  (publish! customer-order-topic [{:customer/id customer-1
                                   :order/id    order-failure}
                                  {:customer/id customer-1
                                   :order/id    order-failure
                                   :order/needs [20]}])

  ; endregion


  ; region ; 3) customers agree to successful orders (order-1 & order-2 above) i.e., :order/approval
  ;
  ; approve order-1
  (do
    (def agreement-id (-> @order->sales-request-view first second :agreement/id))
    (def order-id (-> @order->sales-request-view first second :order/id))
    (def customer-id (-> @order->sales-request-view first second :customer/id))

    (publish! customer-order-approval [{:order/id order-id}
                                       {:agreement/id agreement-id
                                        :order/id     order-id
                                        :customer/id  customer-id
                                        :order/status :order/purchased}]))

  ; approve order-2
  (do
    (def agreement-id (-> @order->sales-request-view second second :agreement/id))
    (def order-id (-> @order->sales-request-view first second :order/id))
    (def customer-id (-> @order->sales-request-view first second :customer/id))

    (publish! customer-order-approval [{:order/id order-id}
                                       {:agreement/id agreement-id
                                        :order/id     order-id
                                        :customer/id  customer-id
                                        :order/status :order/purchased}]))

  ; endregion


  ; region ; 4) providers ship resources for order-1
  (do
    (def order-id (-> @order->sales-request-view first second :order/id))
    (def shipment-id (uuid/v1))
    (def alpha-shipment
      [{:shipment/id shipment-id}
       {:shipment/id    shipment-id
        :order/id       order-id
        :provider/id    "alpha"
        :shipment/items [{:resource/id (uuid/v1) :resource/type 0 :resource/time 0}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 1}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 2}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 3}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 4}
                         {:resource/id (uuid/v1) :resource/type 0 :resource/time 5}]}]))

  (spec/explain :shipment/line-item {:resource/id (uuid/v1) :resource/type 0 :resource/time 0})
  (spec/explain :provider/shipment (second alpha-shipment))

  (publish! shipment-topic alpha-shipment)


  ; endregion


  @resource-state-view


  ; region 5) resources start reporting health & status
  (do
    (def resource-id (-> @resource-state-view keys first)))

  (publish! resource-measurement-topic [{:resource/id resource-id}
                                        {:measurement/id        (uuid/v1)
                                         :resource/id           resource-id
                                         :measurement/attribute :googoo/metric
                                         :measurement/value     100}])

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
  @resource-state-view

  (reset! resource-state-view {})

  ; endregion


  ())


; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; look at 'watchers'
(comment
  (def x (atom 0))


  (add-watch x :watcher
    (fn [key atom old-state new-state]
      (println "new-state" new-state)))

  (reset! x 2)



  ())



; endregion
