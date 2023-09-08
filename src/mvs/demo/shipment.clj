(ns mvs.demo.shipment
  (:require [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [clj-uuid :as uuid]))


(defn- make-shipment-events [order-id]
  (as-> @order->sales-request-view d
    (get d order-id)
    (:agreement/resources d)
    (group-by :provider/id d)
    (map (fn [[provider-id items]]
           {:shipment/id    (uuid/v1)
            :provider/id    provider-id
            :order/id       order-id
            :shipment/items (into []
                              (mapcat (fn [{:keys [resource/type resource/time-frames]}]
                                        (map (fn [t]
                                               {:resource/id   (uuid/v1)
                                                :resource/type type
                                                :resource/time t})
                                          time-frames))
                                items))})
      d)))


(defn providers-ship-order [order-id]
  (doall
    (map (fn [{id :provider/id :as event}]
           (publish! shipment-topic [{:provider/id id} event]))
      (make-shipment-events order-id))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

(comment
  (do
    (def order-1 #uuid"75f888c0-3ac3-11ee-8473-e65ce679c38d")
    (def order-2 #uuid"7a8a9400-3ac3-11ee-8473-e65ce679c38d")
    (def order-3 #uuid"7f6e8fd0-3ac3-11ee-8473-e65ce679c38d")

    (def order-id order-1)

    (def shipment-id (uuid/v1))

    (def resources (as-> @order->sales-request-view d
                     (get d order-id)
                     (:agreement/resources d)
                     (group-by :provider/id d))))


  ; :shipment/id
  ; :order/id
  ; :provider/id
  ; :shipment/items -> [ :resource/id ; assigned by the provider
  ;                      :resource/type
  ;                      :resource/time ]

  (mapcat (fn [[provider-id items]]
            (map (fn [{:keys [resource/type resource/time-frames]}]
                   {:p-id provider-id :o-i order-id :t type :t-f time-frames})
              items))
    resources)

  ; should be 1 shipment per provider that includes ALL resources (of all types and times)
  (map (fn [[provider-id items]]
         {:shipment/id    (uuid/v1)
          :provider/id    provider-id
          :order/id       order-id
          :shipment/items items})
    resources)

  (map (fn [[provider-id items]]
         {:shipment/id    (uuid/v1)
          :provider/id    provider-id
          :order/id       order-id
          :shipment/items (map (fn [item]
                                 (assoc item :resource-id (uuid/v1)))
                            items)})
    resources)


  (map (fn [[provider-id items]]
         {:shipment/id    (uuid/v1)
          :provider/id    provider-id
          :order/id       order-id
          :shipment/items (map (fn [{:keys [resource/type resource/time-frames] :as i}]
                                 {:id (uuid/v1) :i i :t type :t-f time-frames})
                            items)})
    resources)

  (as-> @order->sales-request-view d
    (get d order-id)
    (:agreement/resources d)
    (group-by :provider/id d)
    (map (fn [[provider-id items]]
           {:shipment/id    (uuid/v1)
            :provider/id    provider-id
            :order/id       order-id
            :shipment/items (into []
                              (mapcat (fn [{:keys [resource/type resource/time-frames]}]
                                        (map (fn [t]
                                               {:resource/id   (uuid/v1)
                                                :resource/type type
                                                :resource/time t})
                                          time-frames))
                                items))})
      d))


  (make-shipment-events order-1)
  (make-shipment-events order-2)

  ())



; can we convert from (map (map (map ...) ) ) to (for ...)?
(comment
  (do
    (def order-id #uuid"0f5bf774-4c64-11ee-b4ec-b9081dfd246f")
    (def local (atom {#uuid"0f5bf774-4c64-11ee-b4ec-b9081dfd246f"
                      {:customer/id         #uuid"0f5bf770-4c64-11ee-b4ec-b9081dfd246f",
                       :order/id            #uuid"0f5bf774-4c64-11ee-b4ec-b9081dfd246f",
                       :order/status        :order/submitted,
                       :order/needs         [0 1],
                       :sales/request-id    #uuid"155003b0-4c64-11ee-b4ec-b9081dfd246f",
                       :agreement/id        #uuid"1551d870-4c64-11ee-b4ec-b9081dfd246f",
                       :agreement/resources '({:resource/type        0,
                                               :provider/id          "delta-googoos",
                                               :resource/time-frames [0 1],
                                               :resource/cost        10}
                                              {:resource/type        0,
                                               :provider/id          "alpha-googoos",
                                               :resource/time-frames [2 3 4],
                                               :resource/cost        30})}})))

  (as-> @local d
    (get d order-id)
    (:agreement/resources d)
    (group-by :provider/id d)
    (map (fn [[provider-id items]]
           {:shipment/id    (uuid/v1)
            :provider/id    provider-id
            :order/id       order-id
            :shipment/items (into []
                              (mapcat (fn [{:keys [resource/type resource/time-frames]}]
                                        (map (fn [t]
                                               {:resource/id   (uuid/v1)
                                                :resource/type type
                                                :resource/time t})
                                          time-frames))
                                items))})
      d))

  (as-> @local d
    (get d order-id)
    (:agreement/resources d)
    (group-by :provider/id d)
    (for [[provider-id items] d
          {:keys [resource/type resource/time-frames]} items
          t time-frames]
      {:shipment/id    (uuid/v1)
       :provider/id    provider-id
       :order/id       order-id
       :shipment/items [{:resource/id   (uuid/v1)
                         :resource/type type
                         :resource/time t}]}))
  ; => produces 5 maps (one for each resource), rather than 2 (one for each provider)...




  ())


(comment
  (do
    (def events (make-shipment-events order-id))
    (def event (first events))
    (def id (:provider/id event)))

  (map (fn [{:keys [provider/id] :as event}]
         (publish! shipment-topic [{:provider/id id} event]))
    (make-shipment-events order-id))

  (map (fn [{:keys [provider/id] :as event}]
         [{:provider/id id} event])
    (make-shipment-events order-id))

  (providers-ship-order order-1)


  (mvs.read-model.resource-state-view/resource-states @mvs.read-model.state/app-db)
  (mvs.read-model.resource-state-view/reset-resource-state-view)



  ())


; endregion