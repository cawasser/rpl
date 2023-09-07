(ns mvs.topology.fulfill-order
  (:require [mvs.services :as s]
            [mvs.topics :as t]
            [mvs.read-models :as v]
            [mvs.dashboards :as d]))



(def topo {:mvs/messages {:provider/order {:mvs/message-type :mvs/command}}

           :mvs/entities {:provider-order-topic {:mvs/entity-type :mvs/topic :mvs/name #'t/provider-order-topic}

                          :process-plan         {:mvs/entity-type :mvs/service :mvs/name #'s/process-plan}

                          :provider-dashboard   {:mvs/entity-type :mvs/dashboard :mvs/name #'d/provider-dashboard}}

           :mvs/workflow #{[:process-plan :provider-order-topic :provider/order]
                           [:provider-order-topic :provider-dashboard :provider/order]}})




(comment
  (require '[mvs.topology :as topo])

  (topo/view-topo topo)


  ())