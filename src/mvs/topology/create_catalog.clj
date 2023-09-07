(ns mvs.topology.create-catalog
  (:require [mvs.services :as s]
            [mvs.topics :as t]
            [mvs.read-models :as v]
            [mvs.dashboards :as d]))


(def topo
  {
   :mvs/messages {:provider/catalog   {:mvs/message-type :mvs/event}
                  :sales/catalog      {:mvs/message-type :mvs/event}
                  :resource/resources {:mvs/message-type :mvs/event}}

   :mvs/entities {:provider-catalog-topic      {:mvs/entity-type :mvs/topic :mvs/topic-name #'t/provider-catalog-topic}

                  :process-available-resources {:mvs/entity-type :mvs/service :mvs/name #'s/process-available-resources}
                  :process-provider-catalog    {:mvs/entity-type :mvs/service :mvs/name #'s/process-provider-catalog}

                  :available-resources-view    {:mvs/entity-type :mvs/ktable :mvs/topic-name #'v/available-resources-view}
                  :sales-catalog-view          {:mvs/entity-type :mvs/ktable :mvs/topic-name #'v/sales-catalog-view}

                  :customer-dashboard          {:mvs/entity-type :mvs/dashboard :mvs/name #'d/customer-dashboard}
                  :sales-dashboard             {:mvs/entity-type :mvs/dashboard :mvs/name #'d/sales-dashboard}
                  :planning-dashboard          {:mvs/entity-type :mvs/dashboard :mvs/name #'d/planning-dashboard}
                  :provider-dashboard          {:mvs/entity-type :mvs/dashboard :mvs/name #'d/provider-dashboard}}

   :mvs/workflow [[:provider-dashboard :provider-catalog-topic :provider/catalog]
                  [:provider-catalog-topic :process-provider-catalog :provider/catalog]
                  [:provider-catalog-topic :process-available-resources :provider/catalog]
                  [:process-provider-catalog :sales-catalog-view :sales/catalog]
                  [:process-available-resources :available-resources-view :resource/resources]
                  [:sales-catalog-view :customer-dashboard :sales/catalog]
                  [:sales-catalog-view :sales-dashboard :sales/catalog]
                  [:available-resources-view :planning-dashboard :resource/resources]]})




(comment
  (require '[mvs.topology :as topo])

  (topo/view-topo topo)


  ())
