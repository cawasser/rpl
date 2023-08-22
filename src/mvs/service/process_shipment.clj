(ns mvs.service.process-shipment
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-shipment [_ _ _ [event-key {items :shipment/items
                                          order-id :order/id
                                          provider-id :provider/id
                                          :as   shipment}]]

  (if (spec/valid? :provider/shipment shipment)

    (do
      (println "process-shipment" event-key " // " shipment)

      (reset! last-event shipment)

      ; create a place to hold the measurements for each :resource/id:
      ;    { <:resource/id> { <attributes & values go here> } }
      ;
      ; TODO: we should also enrich with :customer/id, :order/id, :sales/request/id,
      ;       :agreement/id, & :order/needs for later use
      ;
      ;     use :order/id to find other data view from order->sales-request-view
      ;
      (let [extra-data (-> @order->sales-request-view
                         (get order-id)
                         ((juxt :order/id :customer/id :sales/request-id
                            :agreement/id :order/needs)))
            enrichment (zipmap [:order/id :customer/id :sales/request-id
                                :agreement/id :order/needs]
                         extra-data)
            new-vals   (->> items
                         (map (fn [{:keys [resource/id] :as i}]
                                {id (merge {:resource/attributes   (dissoc i :resource/id)
                                            :resource/measurements {}}
                                      {:provider/id provider-id}
                                      enrichment)}))
                         (into {}))]

        (reset! resource-state-view
          (reduce (fn [m [k v :as x]] (assoc m k v))
            @resource-state-view new-vals))))

    (malformed :provider/shipment shipment)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments

(comment
  (do
    (def view (atom {}))
    (def k1 (uuid/v1))
    (def k2 (uuid/v1))
    (def items
      [{:resource/id #uuid"db9c50e1-3c57-11ee-a6ba-72e2bbe3a0f7", :resource/type 0, :resource/time 0}
       {:resource/id #uuid"db9c50e2-3c57-11ee-a6ba-72e2bbe3a0f7", :resource/type 0, :resource/time 1}
       {:resource/id #uuid"db9c50e3-3c57-11ee-a6ba-72e2bbe3a0f7", :resource/type 0, :resource/time 2}
       {:resource/id #uuid"db9c50e4-3c57-11ee-a6ba-72e2bbe3a0f7", :resource/type 0, :resource/time 3}
       {:resource/id #uuid"db9c50e5-3c57-11ee-a6ba-72e2bbe3a0f7", :resource/type 0, :resource/time 4}
       {:resource/id   #uuid"db9c50e6-3c57-11ee-a6ba-72e2bbe3a0f7",
        :resource/type 0, :resource/time 5}])
    (def provider-id "alpha")
    (def extra-data (-> @order->sales-request-view
                      (get id)
                      ((juxt :order/id :customer/id :sales/request-id
                         :agreement/id :order/needs))))
    (def enrichment (zipmap [:order/id :customer/id :sales/request-id
                             :agreement/id :order/needs]
                      extra-data)))

  (def new-vals (->> items
                  (map (fn [{:keys [resource/id] :as i}]
                         {id (merge {:resource/attributes   (dissoc i :resource/id)
                                     :resource/measurements {}}
                               {:provider/id provider-id}
                               enrichment)}))
                  (into {})))

  (reduce (fn [m [k v :as x]] (assoc m k v))
    @resource-state-view new-vals)


  ((juxt :resource/id :resource/time) (first items))

  (-> @order->sales-request-view
    (get id)
    ((juxt :customer/id :sales/request-id
       :agreement/id :order/needs)))

  (do
    (def local (atom @resource-state-view))
    (def id items)
    (def id (-> @order->sales-request-view keys first))
    (def new-vals (->> items
                    (map (fn [{:keys [resource/id] :as i}]
                           {id {:resource/attributes   (dissoc i :resource/id)
                                :resource/measurements {}}}))
                    (into {}))))




  (def qwqert {:resource/attributes {} :resource/measurements {}})
  (merge qwqert enrichment)

  (as-> @local r
    (reduce (fn [m [k v :as x]] (assoc m k v))
      r new-vals)
    (merge r enrichment))


  (reset! resource-state-view {})

  (process-shipment [] [] [] [{} @last-event])

  ())


; endregion
