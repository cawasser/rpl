(ns mvs.core
  (:require [mvs.constants :refer :all]
            [mvs.dashboard.sales :as sales]
            [mvs.dashboards :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :refer :all]
            [mvs.read-model.customer-order-view :as cov]
            [mvs.read-model.order-sales-request-view :as osr]
            [mvs.read-model.provider-catalog-view :as pcv]
            [mvs.read-model.resource-measurements-view :as rmv]
            [mvs.read-model.resource-performance-view :as rpv]
            [mvs.read-model.resource-state-view :as rsv]
            [mvs.read-model.sales-catalog-view :as scv]
            [mvs.services :refer :all]
            [mvs.specs :refer :all]
            [mvs.topics :refer :all]
            [mvs.topology :as topo]))


(set! *print-namespace-maps* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Wiring (a Willa-like DSL using 'topics' and 'ktables')
;
;  wire the services together using watchers on the various atoms

(def mvs-topology {
                   ; region ; :mvs/messages
                   :mvs/messages {
                                  :provider/catalog          {:mvs/message-type :mvs/event}
                                  :sales/catalog             {:mvs/message-type :mvs/event}
                                  :customer/order            {:mvs/message-type :mvs/command}
                                  :orders/state              {:mvs/message-type :mvs/view}
                                  :agreement/state           {:mvs/message-type :mvs/view}
                                  :sales/request             {:mvs/message-type :mvs/command}
                                  :sales/commitment          {:mvs/message-type :mvs/event}
                                  :sales/failure             {:mvs/message-type :mvs/event}
                                  :customer/agreement        {:mvs/message-type :mvs/event}
                                  :customer/approval         {:mvs/message-type :mvs/command}
                                  :resource/plan             {:mvs/message-type :mvs/event}
                                  :resource/config           {:mvs/message-type :mvs/command}
                                  :provider/order            {:mvs/message-type :mvs/command}
                                  :provider/shipment         {:mvs/message-type :mvs/event}
                                  :resource/measurement      {:mvs/message-type :mvs/event}
                                  :resource/usage            {:mvs/message-type :mvs/event}
                                  :resource/health           {:mvs/message-type :mvs/event}
                                  :resource/performance      {:mvs/message-type :mvs/event}
                                  :resource/usage-view       {:mvs/message-type :mvs/view}
                                  :resource/health-view      {:mvs/message-type :mvs/view}
                                  :resource/performance-view {:mvs/message-type :mvs/view}
                                  :resource/resources        {:mvs/message-type :mvs/view}
                                  :resource/state            {:mvs/message-type :mvs/view}}
                   ; endregion

                   ; region ; :mvs/entities
                   :mvs/entities {
                                  :provider-catalog-topic       {:mvs/entity-type :mvs/topic :mvs/topic-name provider-catalog-topic}
                                  :provider-order-topic         {:mvs/entity-type :mvs/topic :mvs/topic-name provider-order-topic}
                                  :sales-catalog-topic          {:mvs/entity-type :mvs/topic :mvs/topic-name sales-catalog-topic}
                                  :customer-order-topic         {:mvs/entity-type :mvs/topic :mvs/topic-name customer-order-topic}
                                  :sales-request-topic          {:mvs/entity-type :mvs/topic :mvs/topic-name sales-request-topic}
                                  :sales-commitment-topic       {:mvs/entity-type :mvs/topic :mvs/topic-name sales-commitment-topic}
                                  :sales-failure-topic          {:mvs/entity-type :mvs/topic :mvs/topic-name sales-failure-topic}
                                  :sales-agreement-topic        {:mvs/entity-type :mvs/topic :mvs/topic-name sales-agreement-topic}
                                  :customer-order-approval      {:mvs/entity-type :mvs/topic :mvs/topic-name customer-order-approval}
                                  :plan-topic                   {:mvs/entity-type :mvs/topic :mvs/topic-name plan-topic}
                                  :shipment-topic               {:mvs/entity-type :mvs/topic :mvs/topic-name shipment-topic}
                                  :resource-measurement-topic   {:mvs/entity-type :mvs/topic :mvs/topic-name resource-measurement-topic}
                                  :measurement-topic            {:mvs/entity-type :mvs/topic :mvs/topic-name measurement-topic}
                                  :health-topic                 {:mvs/entity-type :mvs/topic :mvs/topic-name health-topic}
                                  :performance-topic            {:mvs/entity-type :mvs/topic :mvs/topic-name performance-topic}
                                  :usage-topic                  {:mvs/entity-type :mvs/topic :mvs/topic-name usage-topic}

                                  :available-resources-view     {:mvs/entity-type :mvs/ktable :mvs/topic-name #'available-resources-view}
                                  :customer-order-view          {:mvs/entity-type :mvs/ktable :mvs/topic-name #'customer-order-view}
                                  :customer-agreement-view      {:mvs/entity-type :mvs/ktable :mvs/topic-name #'customer-agreement-view}
                                  :resource-health-view         {:mvs/entity-type :mvs/ktable :mvs/topic-name #'resource-health-view}
                                  :resource-usage-view          {:mvs/entity-type :mvs/ktable :mvs/topic-name #'resource-usage-view}




                                  ; TODO: finish the conversion form atoms to app-db events
                                  :order->sales-request-view    {:mvs/entity-type :mvs/ktable :mvs/name #'osr/order->sales-request-view}
                                  :provider-catalog-view        {:mvs/entity-type :mvs/ktable :mvs/name #'pcv/provider-catalog-view}
                                  :resource-measurement-view    {:mvs/entity-type :mvs/ktable :mvs/name #'rmv/reset-resource-measurements-view}
                                  :resource-performance-view    {:mvs/entity-type :mvs/ktable :mvs/name #'rpv/resource-performance-view}
                                  :resource-state-view          {:mvs/entity-type :mvs/ktable :mvs/name #'rsv/resource-state-view}
                                  :sales-catalog-view           {:mvs/entity-type :mvs/ktable :mvs/name #'scv/sales-catalog-view}



                                  :customer-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'customer-dashboard}
                                  :provider-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'provider-dashboard}
                                  :monitoring-dashboard         {:mvs/entity-type :mvs/dashboard :mvs/name #'monitoring-dashboard}
                                  :billing-dashboard            {:mvs/entity-type :mvs/dashboard :mvs/name #'billing-dashboard}
                                  :planning-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'planning-dashboard}
                                  :customer-support-dashboard   {:mvs/entity-type :mvs/dashboard :mvs/name #'customer-support-dashboard}
                                  :sales-dashboard              {:mvs/entity-type :mvs/dashboard :mvs/name #'sales-dashboard}

                                  :process-available-resources  {:mvs/entity-type :mvs/service :mvs/name #'process-available-resources}
                                  :process-provider-catalog     {:mvs/entity-type :mvs/service :mvs/name #'process-provider-catalog}
                                  ;:process-sales-catalog        {:mvs/entity-type :mvs/service :mvs/name #'process-sales-catalog}
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
                   ; endregion

                   ; region ; :mvs/workflow
                   :mvs/workflow #{
                                   [:provider-catalog-topic :process-provider-catalog :provider/catalog]
                                   [:provider-catalog-topic :process-available-resources :provider/catalog]
                                   [:provider-catalog-topic :provider-catalog-view :provider/catalog]
                                   [:sales-catalog-view :process-provider-catalog :provider/catalog]
                                   [:process-provider-catalog :sales-catalog-view :provider/catalog]
                                   [:process-provider-catalog :sales-catalog-view :sales/catalog]
                                   [:process-available-resources :available-resources-view :resource/resources]
                                   [:sales-catalog-view :customer-dashboard :sales/catalog]

                                   [:customer-order-topic :process-customer-order :customer/order]
                                   [:process-customer-order :customer-order-view :orders/state]
                                   [:customer-order-view :sales-dashboard :orders/state]

                                   [:customer-agreement-view :sales-dashboard :agreement/state]
                                   [:planning-dashboard :customer-agreement-view :agreement/state]
                                   [:customer-order-approval :process-order-approval :customer/approval]
                                   [:process-order-approval :customer-agreement-view :agreement/state]
                                   [:process-order-approval :customer-order-view :orders/state]
                                   [:sales-request-topic :process-sales-request :sales/request]
                                   [:available-resources-view :process-sales-request :resource/resources]

                                   [:sales-commitment-topic :process-sales-commitment :sales/commitment]
                                   [:sales-failure-topic :process-sales-commitment :sales/failure]
                                   [:process-sales-commitment :customer-order-view :orders/state]
                                   [:process-sales-commitment :customer-agreement-view :agreement/state]
                                   [:sales-agreement-topic :customer-dashboard :customer/agreement]

                                   [:process-order-approval :plan-topic :resource/plan]
                                   [:plan-topic :process-plan :resource/plan]

                                   [:process-customer-order :sales-request-topic :sales/request]
                                   [:process-sales-request :sales-commitment-topic :sales/commitment]
                                   [:process-sales-request :sales-failure-topic :sales/failure]
                                   [:process-sales-commitment :sales-agreement-topic :sales/commitment]

                                   [:process-plan :provider-order-topic :provider/order]
                                   [:process-plan :resource-state-view :resource/resources]
                                   [:provider-order-topic :provider-dashboard :provider/order]

                                   [:shipment-topic :process-shipment :provider/shipment]
                                   [:process-shipment :resource-state-view :resource/resources]

                                   [:resource-measurement-topic :process-measurement :resource/measurement]
                                   [:resource-state-view :process-measurement :resource/state]
                                   [:resource-state-view :monitoring-dashboard :resource/state]
                                   [:process-measurement :resource-state-view :resource/state]
                                   [:process-measurement :measurement-topic :resource/measurement]

                                   [:measurement-topic :process-resource-health :resource/measurement]
                                   [:process-resource-health :health-topic :resource/health]
                                   [:health-topic :resource-health-view :resource/health]
                                   [:resource-health-view :planning-dashboard :resource/health-view]
                                   [:resource-health-view :customer-support-dashboard :resource/health-view]
                                   [:resource-health-view :monitoring-dashboard :resource/health-view]

                                   [:measurement-topic :process-resource-performance :resource/measurement]
                                   [:process-resource-performance :performance-topic :resource/performance]
                                   ;[:performance-topic :resource-performance-view :resource/performance] ; duplicating the data, since we add "by-hand" and then again with this link
                                   [:resource-performance-view :planning-dashboard :resource/performance-view]
                                   [:resource-performance-view :customer-support-dashboard :resource/performance-view]
                                   [:resource-performance-view :monitoring-dashboard :resource/performance-view]
                                   [:resource-performance-view :customer-dashboard :resource/performance-view]

                                   [:measurement-topic :process-resource-usage :resource/measurement]
                                   [:process-resource-usage :usage-topic :resource/usage]
                                   [:usage-topic :resource-usage-view :resource/usage]
                                   [:resource-usage-view :billing-dashboard :resource/usage-view]
                                   [:resource-usage-view :customer-support-dashboard :resource/usage-view]
                                   [:resource-usage-view :monitoring-dashboard :resource/usage-view]

                                   [:customer-dashboard :customer-order-topic :customer/order]
                                   [:customer-dashboard :customer-order-approval :customer/approval]
                                   [:provider-dashboard :shipment-topic :provider/shipment]
                                   [:provider-dashboard :provider-catalog-topic :provider/catalog]
                                   [:provider-dashboard :resource-measurement-topic :resource/measurement]}
                   ; endregion
                   :_            nil})



(defn start-ui [])



; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; DESIGN CONSIDERATIONS
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
; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (topo/view-topo mvs-topology)

  (topo/init-topology mvs-topology)

  (reset-topology mvs-topology)

  ())


; try combining the partial-topos
(comment
  (-> topo/complete-system
    topo/compose-topology
    topo/view-topo)



  ())

; endregion
