(ns mvs.topology.place-order
  (:require [mvs.services :as s]
            [mvs.topics :as t]
            [mvs.read-models :as v]
            [mvs.dashboards :as d]))


(def topo
  {:mvs/messages {:customer/order       {:mvs/message-type :mvs/command}
                  :orders/state         {:mvs/message-type :mvs/view}
                  :agreement/state      {:mvs/message-type :mvs/view}
                  :sales/request        {:mvs/message-type :mvs/command}
                  :sales/order->request {:mvs/message-type :mvs/view}
                  :sales/commitment     {:mvs/message-type :mvs/event}
                  :sales/failure        {:mvs/message-type :mvs/event}
                  :customer/agreement   {:mvs/message-type :mvs/event}
                  :resource/resources   {:mvs/message-type :mvs/view}}

   :mvs/entities {:customer-order-topic      {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/customer-order-topic}
                  :sales-request-topic       {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/sales-request-topic}
                  :sales-commitment-topic    {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/sales-commitment-topic}
                  :sales-failure-topic       {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/sales-failure-topic}
                  :sales-agreement-topic     {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/sales-agreement-topic}

                  :available-resources-view  {:mvs/entity-type :mvs/ktable :mvs/topic-name #'v/available-resources-view}
                  :order->sales-request-view {:mvs/entity-type :mvs/ktable :mvs/topic-name #'v/order->sales-request-view}

                  :process-customer-order    {:mvs/entity-type :mvs/service :mvs/name #'s/process-customer-order}
                  :process-sales-request     {:mvs/entity-type :mvs/service :mvs/name #'s/process-sales-request}
                  :process-sales-commitment  {:mvs/entity-type :mvs/service :mvs/name #'s/process-sales-commitment}

                  :customer-dashboard        {:mvs/entity-type :mvs/dashboard :mvs/name #'d/customer-dashboard}
                  :planning-dashboard        {:mvs/entity-type :mvs/dashboard :mvs/name #'d/planning-dashboard}
                  :sales-dashboard           {:mvs/entity-type :mvs/dashboard :mvs/name #'d/sales-dashboard}}

   :mvs/workflow #{[:customer-dashboard :customer-order-topic :customer/order]
                   [:customer-order-topic :process-customer-order :customer/order]
                   [:process-customer-order :sales-request-topic :sales/request]
                   [:process-customer-order :order->sales-request-view :sales/order->request]
                   [:sales-request-topic :process-sales-request :sales/request]
                   [:available-resources-view :process-sales-request :resource/resources]
                   [:process-sales-request :sales-commitment-topic :sales/commitment]
                   [:process-sales-request :sales-failure-topic :sales/failure]
                   [:process-sales-request :committed-resource-view :resource/resources]
                   [:process-sales-request :order->sales-request-view :resource/resources]
                   [:sales-commitment-topic :process-sales-commitment :sales/commitment]
                   [:order->sales-request-view :process-sales-commitment :sales/order->request]
                   [:process-sales-commitment :sales-agreement-topic :customer/agreement]
                   [:process-sales-commitment :order->sales-request-view :sales/order->request]
                   [:order->sales-request-view :sales-dashboard :sales/order->request]
                   [:available-resources-view :planning-dashboard :resource/resources]
                   [:sales-agreement-topic :customer-dashboard :customer/agreement]}})




(comment
  (require '[mvs.topology :as topo])

  (topo/view-topo topo)


  ())
