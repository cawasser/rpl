(ns mvs.topology.approve-order
  (:require [mvs.services :as s]
            [mvs.topics :as t]
            [mvs.read-models :as v]
            [mvs.dashboards :as d]))


(def topo {:mvs/messages {:customer/approval    {:mvs/message-type :mvs/command}
                          :resource/plan        {:mvs/message-type :mvs/event}
                          :sales/order->request {:mvs/message-type :mvs/view}}

           :mvs/entities {:customer-order-approval   {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/customer-order-approval}
                          :plan-topic                {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/plan-topic}

                          :order->sales-request-view {:mvs/entity-type :mvs/ktable :mvs/topic-name #'v/order->sales-request-view}

                          :process-customer-order    {:mvs/entity-type :mvs/service :mvs/name #'s/process-customer-order}
                          :process-plan              {:mvs/entity-type :mvs/service :mvs/name #'s/process-plan}

                          :customer-dashboard        {:mvs/entity-type :mvs/dashboard :mvs/name #'d/customer-dashboard}
                          :sales-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'d/sales-dashboard}}

           :mvs/workflow #{[:customer-dashboard :customer-order-approval :customer/approval]
                           [:customer-order-approval :process-customer-order :customer/approval]
                           [:process-customer-order :order->sales-request-view :sales/order->request]
                           [:process-customer-order :plan-topic :resource/plan]
                           [:plan-topic :process-plan :resource/plan]
                           [:order->sales-request-view :sales-dashboard :sales/order->request]}})





(comment
  (require '[mvs.topology :as topo])

  (topo/view-topo topo)


  ())