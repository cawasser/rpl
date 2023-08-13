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
            [clj-uuid :as uuid]))


(set! *print-namespace-maps* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Wiring (a Willa-like DSL using 'topics' and 'ktables')
;
;  wire the services together using watchers on the various atoms

(def mvs-wiring {:mvs/entities {:provider-catalog-topic      {:mvs/entity-type :mvs/topic :mvs/topic-name provider-catalog-topic}
                                :customer-order-topic        {:mvs/entity-type :mvs/topic :mvs/topic-name customer-order-topic}
                                :sales-request-topic         {:mvs/entity-type :mvs/topic :mvs/topic-name sales-request-topic}
                                :sales-commitment-topic      {:mvs/entity-type :mvs/topic :mvs/topic-name sales-commitment-topic}
                                :sales-failure-topic         {:mvs/entity-type :nvs/topic :mvs/topic-name sales-failure-topic}

                                :service-catalog-view        {:mvs/entity-type :mvs/ktable :mvs/topic-name service-catalog-view}
                                :committed-resource-view     {:mvs/entity-type :mvs/ktable :mvs/topic-name committed-resources-view}

                                :process-available-resources {:mvs/entity-type :mvs/service :mvs/service-name #'process-available-resources}
                                :process-provider-catalog    {:mvs/entity-type :mvs/service :mvs/service-name #'process-provider-catalog}
                                ;:process-sales-catalog       {:mvs/entity-type :mvs/service :mvs/service-name #'process-sales-catalog}
                                :process-customer-order      {:mvs/entity-type :mvs/service :mvs/service-name #'process-customer-order}
                                :process-sales-request       {:mvs/entity-type :mvs/service :mvs/service-name #'process-sales-request}
                                :process-sales-commitment    {:mvs/entity-type :mvs/service :mvs/service-name #'process-sales-commitment}
                                :process-customer-agreement  {:mvs/entity-type :mvs/service :mvs/service-name #'process-customer-agreement}}

                 :mvs/workflow [[:provider-catalog-topic :process-provider-catalog]
                                [:provider-catalog-topic :process-available-resources]

                                [:customer-order-topic :process-customer-order]
                                [:sales-request-topic :process-sales-request]

                                [:sales-commitment-topic :process-sales-commitment]
                                [:sales-failure-topic :process-sales-commitment]

                                ;[:service-catalog-view :process-sales-catalog]
                                [:committed-resource-view :process-customer-agreement]]})


(defn init-topology [wiring]
  (let [entities (:mvs/entities wiring)
        workflow (:mvs/workflow wiring)]
    (doall
      (map (fn [[from to]]
             (add-watch (-> entities from :mvs/topic-name)
               to (-> entities to :mvs/service-name)))
        workflow))))


; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Scripting

(comment

  ; region ; reset everything and re-load the catalog(s)
  (do
    (reset! provider-catalog-view {})
    (reset! available-resources-view {})
    (reset! service-catalog-view {})
    (reset! order->sales-request-view [])

    (init-topology mvs-wiring)

    ; providers 'publish' catalogs

    (do
      (publish! provider-catalog-topic [{:provider/id "alpha"} provider-alpha])
      (publish! provider-catalog-topic [{:provider/id "bravo"} provider-bravo])
      (publish! provider-catalog-topic [{:provider/id "charlie"} provider-charlie])
      (publish! provider-catalog-topic [{:provider/id "delta"} provider-delta])
      (publish! provider-catalog-topic [{:provider/id "echo"} provider-echo])))

  ; endregion


  ; region ; customers request services
  (do
    (def customer-1 (uuid/v1))
    (def customer-2 (uuid/v1))
    (def customer-3 (uuid/v1))

    (def order-1 (uuid/v1))
    (def order-2 (uuid/v1))
    (def order-3 (uuid/v1)))

  (publish! customer-order-topic [{:order/id order-1 :customer/id customer-1}
                                  {:order/id    order-1 :customer/id customer-1
                                   :order/needs [0 1]}])

  (publish! customer-order-topic [{:order/id order-2 :customer/id customer-2}
                                  {:order/id       order-2 :customer/id customer-2
                                   :customer/needs [0 1]}])

  (publish! customer-order-topic [{:customer/id customer-3 :order/id order-3}
                                  {:customer/id    customer-3 :order/id order-3
                                   :customer/needs [0 1]}])

  ; this one "fails"
  (publish! customer-order-topic [{:customer/id         customer-1
                                   :customer/request-id req-1}
                                  {:customer/id         customer-1
                                   :customer/request-id req-1
                                   :customer/needs      [20]}])

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
