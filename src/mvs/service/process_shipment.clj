(ns mvs.service.process-shipment
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :as rm :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-shipment
  "'select' the actual resources (by assigning them a `:resource/id`) to fulfil a `:provider/order`
  and publish the :provider/shipment"

  [[event-key {items       :shipment/items
               order-id    :order/id
               provider-id :provider/id
               :as         shipment}
    :as event]]

  (if (spec/valid? :provider/shipment shipment)

    (do
      (println "process-shipment" event-key " // " shipment)

      (reset! last-event event)

      ; create a place to hold the measurements for each :resource/id:
      ;    { <:resource/id> { <attributes & values go here> } }

      ; which SHOULD be in resource-measurements-view...
      ;
      ;
      ;
      ;
      ; TODO: we should also enrich with :customer/id, :order/id, :sales/request/id,
      ;       :agreement/id, & :order/needs for later use
      ;
      ;     use :order/id to find other data view from order->sales-request-view
      ;
      (let [extra-data (-> (rm/order->sales-request (rm/state))
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

        (doseq [[k v] new-vals]
          (rm/resource-state-view [{:resource/id k} v]))))

    (malformed "process-shipment" :provider/shipment shipment)))





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
    (def extra-data (-> (rm/order->sales-request (rm/state))
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
    (rm/resource-states (rm/state)) new-vals)


  ((juxt :resource/id :resource/time) (first items))

  (-> (rm/order->sales-request (rm/state))
    (get id)
    ((juxt :customer/id :sales/request-id
       :agreement/id :order/needs)))

  (do
    (def local (atom (rm/resource-states (rm/state))))
    (def id items)
    (def id (-> (rm/order->sales-request (rm/state)) keys first))
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

  (process-shipment [{} @last-event])

  ())


(comment
  (do
    (def event @last-event)
    (def event-key (first event))
    (def shipment (second event))
    (def items (:shipment/items shipment))
    (def order-id (:order/id shipment))
    (def provider-id (:provider/id shipment))
    (def local
      (atom {#uuid"02bbba94-4db9-11ee-918b-b9081dfd246f"
             {:customer/id         #uuid"02bbba90-4db9-11ee-918b-b9081dfd246f",
              :order/id            #uuid"02bbba94-4db9-11ee-918b-b9081dfd246f",
              :order/status        :order/submitted,
              :order/needs         [0 1],
              :sales/request-id    #uuid"2224a720-4db9-11ee-918b-b9081dfd246f",
              :agreement/id        #uuid"222654d0-4db9-11ee-918b-b9081dfd246f",
              :agreement/resources '({:resource/type        0,
                                      :provider/id          "delta-googoos",
                                      :resource/time-frames [0 1],
                                      :resource/cost        10}
                                     {:resource/type        0,
                                      :provider/id          "alpha-googoos",
                                      :resource/time-frames [2 3 4],
                                      :resource/cost        30}
                                     {:resource/type        0,
                                      :provider/id          "bravo-googoos",
                                      :resource/time-frames [5],
                                      :resource/cost        5}
                                     {:resource/type        1,
                                      :provider/id          "delta-googoos",
                                      :resource/time-frames [0 1],
                                      :resource/cost        10}
                                     {:resource/type        1,
                                      :provider/id          "alpha-googoos",
                                      :resource/time-frames [2 3 4],
                                      :resource/cost        30}
                                     {:resource/type        1,
                                      :provider/id          "bravo-googoos",
                                      :resource/time-frames [5],
                                      :resource/cost        5})}}))


    (def extra-data (-> @local
                      (get order-id)
                      ((juxt :order/id :customer/id :sales/request-id
                         :agreement/id :order/needs))))
    (def enrichment (zipmap [:order/id :customer/id :sales/request-id
                             :agreement/id :order/needs]
                      extra-data))
    (def new-vals (->> items
                    (map (fn [{:keys [resource/id] :as i}]
                           {id (merge {:resource/attributes   (dissoc i :resource/id)
                                       :resource/measurements {}}
                                 {:provider/id provider-id}
                                 enrichment)}))
                    (into {})))

    (def resource (first new-vals))
    (def resource-id (first resource))
    (def content (second resource)))

  (rm/resource-state-view [{:resource/id resource-id} content])

  (count new-vals)

  (doseq [[k v] new-vals]
    (rm/resource-state-view [{:resource/id k} v]))


  ())


; endregion
