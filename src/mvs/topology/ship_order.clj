(ns mvs.topology.ship-order
  (:require [mvs.services :as s]
            [mvs.topics :as t]
            [mvs.read-models :as v]
            [mvs.dashboards :as d]))



(def topo {:mvs/messages {:provider/shipment    {:mvs/message-type :mvs/event}
                          :provider/shipments   {:mvs/message-type :mvs/view}
                          :sales/order->request {:mvs/message-type :mvs/view}}

           :mvs/entities {:shipment-topic            {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/shipment-topic}
                          :customer-delivery-topic   {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/customer-delivery-topic}

                          :resource-state-view       {:mvs/entity-type :mvs/ktable :mvs/topic-name #'v/resource-state-view}
                          :order->sales-request-view {:mvs/entity-type :mvs/ktable :mvs/topic-name #'v/order->sales-request-view}

                          :process-shipment          {:mvs/entity-type :mvs/service :mvs/name #'s/process-shipment}

                          :provider-dashboard        {:mvs/entity-type :mvs/dashboard :mvs/name #'d/provider-dashboard}
                          :customer-dashboard        {:mvs/entity-type :mvs/dashboard :mvs/name #'d/customer-dashboard}
                          :sales-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'d/sales-dashboard}}

           :mvs/workflow #{[:provider-dashboard :shipment-topic :provider/shipment]
                           [:shipment-topic :process-shipment :provider/shipment]
                           [:process-shipment :resource-state-view :provider/shipment]
                           [:process-shipment :order->sales-request-view :sales/order->request]
                           [:process-shipment :customer-delivery-topic :provider/shipment]
                           [:customer-delivery-topic :customer-dashboard :provider/shipments]
                           [:order->sales-request-view :sales-dashboard :sales/order->request]}})




(comment
  (require '[mvs.topology :as topo])

  (topo/view-topo topo)


  ())