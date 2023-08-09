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
            [clj-uuid :as uuid]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Wiring (a Willa-like DSL using 'topics' and 'ktables')
;
;  wire the services together using watchers on the various atoms

(def mvs-wiring {:mvs/entities {:provider-catalog-topic      {:mvs/entity-type :mvs/topic :mvs/topic-name provider-catalog-topic}
                                :customer-request-topic      {:mvs/entity-type :mvs/topic :mvs/topic-name customer-request-topic}
                                :service-request-topic       {:mvs/entity-type :mvs/topic :mvs/topic-name service-request-topic}
                                :service-commitment-topic    {:mvs/entity-type :mvs/topic :mvs/topic-name service-commitment-topic}

                                :service-catalog-view        {:mvs/entity-type :mvs/ktable :mvs/topic-name service-catalog-view}

                                :available-resources         {:mvs/entity-type :mvs/service :mvs/service-name #'available-resources}
                                :process-producer-catalog    {:mvs/entity-type :mvs/service :mvs/service-name #'process-producer-catalog}
                                :customer-service-catalog    {:mvs/entity-type :mvs/service :mvs/service-name #'customer-service-catalog}
                                :process-customer-request    {:mvs/entity-type :mvs/service :mvs/service-name #'process-customer-request}
                                :process-service-request     {:mvs/entity-type :mvs/service :mvs/service-name #'process-service-request}
                                :process-customer-commitment {:mvs/entity-type :mvs/service :mvs/service-name #'process-customer-commitment}}

                 :mvs/workflow [[:provider-catalog-topic :process-producer-catalog]
                                [:provider-catalog-topic :available-resources]

                                [:customer-request-topic :process-customer-request]
                                [:service-request-topic :process-service-request]
                                [:service-commitment-topic :process-customer-commitment]

                                [:service-catalog-view :customer-service-catalog]]})


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

  ; region ; reset everything
  (do
    (reset! provider-catalog-topic [])
    (reset! provider-catalog-view {})
    (reset! available-resources-view {})

    (init-topology mvs-wiring))

  ; endregion

  ; region ; providers 'publish' catalogs

  (publish! provider-catalog-topic [{:provider/id "alpha"} provider-alpha])
  (publish! provider-catalog-topic [{:provider/id "bravo"} provider-bravo])
  (publish! provider-catalog-topic [{:provider/id "charlie"} provider-charlie])
  (publish! provider-catalog-topic [{:provider/id "delta"} provider-delta])
  (publish! provider-catalog-topic [{:provider/id "echo"} provider-echo])

  ;endregion

  ; region ; customers request services
  (def customer-1 (uuid/v1))
  (def customer-2 (uuid/v1))
  (def customer-3 (uuid/v1))

  (def req-1 (uuid/v1))
  (def req-2 (uuid/v1))
  (def req-3 (uuid/v1))

  (publish! customer-request-topic [{:customer/id         customer-1
                                     :customer/request-id req-1}
                                    {:customer/id         customer-1
                                     :customer/request-id req-1
                                     :customer/needs      [0 1]}])

  (publish! customer-request-topic [{:customer/id         customer-2
                                     :customer/request-id req-2}
                                    {:customer/id         customer-2
                                     :customer/request-id req-2
                                     :customer/needs      [0 1]}])

  (publish! customer-request-topic [{:customer/id         customer-3
                                     :customer/request-id req-3}
                                    {:customer/id         customer-3
                                     :customer/request-id req-3
                                     :customer/needs      [0 1]}])

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
