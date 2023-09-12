(ns mvs.read-model.order-sales-request-view
  (:require [mvs.constants :as const]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))


; order/accepted (process-customer-order)
(defmethod e/event-handler :order/accepted
  [{event-key                         :event/key
    {:keys [order/event] :as content} :event/content :as params}]

  (println ":order/submitted" event-key "//" (:order/id event-key) "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    assoc-in [:order->sales-request-view (:order/id event-key)]
    (-> content
      (dissoc :order/event)
      (assoc :order/status :order/accepted))))


; :order/committed (process-sales-request)
(defmethod e/event-handler :order/committed
  [{event-key                                                   :event/key
    {:keys [order/event
            commitment/id commitment/resources
            commitment/time-frame commitment/cost] :as content} :event/content :as params}]

  (println ":order/reserved" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id event-key)]
    #(-> %
       (assoc :order/status :order/committed
         :commitment/id id
         :commitment/resources resources
         :commitment/time-frame time-frame
         :commitment/cost cost))))


; :order/unable-to-reserve (process-sales-request)
(defmethod e/event-handler :order/unable-to-reserve
  [{event-key                         :event/key
    {:keys [order/event] :as content} :event/content :as params}]

  (println ":order/reserved" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id event-key)]
    #(-> %
       (assoc :order/status :order/unable-to-reserve))))


; :order/awaiting-approval (process-sales-commitment)
(defmethod e/event-handler :order/awaiting-approval
  [{event-key                             :event/key
    {:keys [order/event
            agreement/id agreement/resources
            agreement/time-frame agreement/price
            agreement/notes] :as content} :event/content :as event}]

  (println ":order/awaiting-approval" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id event-key)]
    #(-> %
       (assoc :order/status :order/awaiting-approval
         :agreement/id id
         :agreement/resources resources
         :agreement/time-frame time-frame
         :agreement/price price
         :agreement/notes notes))))


; :order/approved (process-order-approval)
(defmethod e/event-handler :order/awaiting-fulfilment
  [{event-key                                 :event/key
    {:keys [order/event plan/id] :as content} :event/content :as params}]

  (println ":order/awaiting-fulfilment" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id event-key)]
    #(-> %
       (assoc :order/status :order/awaiting-fulfilment
         :plan/id id))))


; :order/purchased (process-plan)
(defmethod e/event-handler :order/purchased
  [{event-key                                         :event/key
    {:keys [order/event order/providers] :as content} :event/content :as params}]

  (println ":order/purchased" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id event-key)]
    #(-> %
       (assoc :order/status :order/purchased
         :order/providers providers))))


; :order/fulfilled (????)
(defmethod e/event-handler :order/fulfilled
  [{event-key                         :event/key
    {:keys [order/event] :as content} :event/content :as params}]

  (println ":order/purchased" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id event-key)]
    #(-> %
       (dissoc :order/event)
       (assoc :order/status :order/fulfilled))))


(defn order->sales-request [context]
  (fx/sub-val context :order->sales-request-view))


(defn order->sales-request-view [[event-key event-content :as event]]
  (println "order->sales-request-view" event)
  (e/event-handler {:event/type    (:order/event event-content)
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-order->sales-request-view []
  (swap! state/app-db
    fx/swap-context
    assoc :order->sales-request-view {}))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (reset-order->sales-request-view)
  (order->sales-request (state/db))

  (do
    (require '[mvs.helpers :as h])
    (def local (atom {}))
    (def event [{:customer/id #uuid"ed6fc000-4e53-11ee-bf37-acef1c2857e1",
                 :order/id    #uuid"ed6fc004-4e53-11ee-bf37-acef1c2857e1"}
                {:customer/id  #uuid"ed6fc000-4e53-11ee-bf37-acef1c2857e1",
                 :order/id     #uuid"ed6fc004-4e53-11ee-bf37-acef1c2857e1",
                 :order/event  :order/submitted
                 :order/status :order/submitted,
                 :order/needs  [0 1]}])
    (def event-key (first event))
    (def order (second event)))

  ; :order/submitted (process-customer-order)
  (swap! local
    assoc (:order/id order)
    (dissoc order :order/event))

  (order->sales-request-view [{:order/id (:order/id order)}
                              (assoc order
                                :sales/request-id "dummy-sales-request"
                                :order/event :order/submitted)])
  (order->sales-request (state/db))


  ; :order/reserved (process-sales-request)
  (do
    (def sales-request-id (-> (:order/id order) h/associated-sales-request :sales/request-id))
    (def assoc-agreement (h/associated-order sales-request-id))
    (def commitment-id "dummy-commitment")
    (def request-id "dummy-sales-request")
    (def resources [{:resource/type        0
                     :provider/id          "provider-1"
                     :resource/time-frames [0 1 2 3]
                     :resource/cost        1000}
                    {:resource/type        1
                     :provider/id          "provider-1"
                     :resource/time-frames [0 1 2 3]
                     :resource/cost        1000}])
    (def time-frames [0 1 2 3])
    (def total-cost 2000))

  (swap! local
    update (:order/id order)
    #(-> %
       (assoc :commitment/id commitment-id
         :order/status :order/committed
         :commitment/resources resources
         :commitment/time-frame time-frames
         :commitment/cost total-cost)))

  (order->sales-request-view [{:order/id (:order/id order)}
                              (assoc order
                                :order/event :order/committed
                                :commitment/id commitment-id
                                :commitment/resources resources
                                :commitment/time-frame time-frames
                                :commitment/cost total-cost)])
  (order->sales-request (state/db))



  ; :order/awaiting-approval (process-sales-commitment)
  (swap! local
    update (:order/id order)
    #(-> %
       (assoc :order/status :order/awaiting-approval
         :agreement/id "dummy-agreement"
         :agreement/resources [{} {} {}])))

  (order->sales-request-view [{:order/id (:order/id order)}
                              (assoc {:order/event :order/awaiting-approval}
                                :order/status :order/awaiting-approval
                                :agreement/id "dummy-agreement"
                                :agreement/resources [{} {} {}])])

  (order->sales-request (state/db))



  ; :order/approved (process-order-approval)
  (swap! local
    update (:order/id order)
    #(-> %
       (assoc :order/status :order/awaiting-fulfilment
         :plan/id ":plan/id"
         :order/providers ["one" "two" "three"])))


  (let []
    (order->sales-request-view [{:order/id (:order/id order)}
                                (assoc order
                                  :order/event :order/approved
                                  :commitment/resources resources)]))
  (order->sales-request (state/db))


  ; :order/shipped (process-shipment)
  (swap! local
    update (:order/id order)
    #(-> %
       (assoc :order/status :order/shipped)))

  ())



; test all the event-handler multi-methods
(comment
  (do
    (reset-order->sales-request-view)
    (def order {:customer/id  "alice"
                :order/id     "alice-order-1"
                :order/status :order/submitted
                :order/needs  [0 1]})
    (def order-id (:order/id order))
    (def customer-id (:customer/id order))
    (def order-needs (:order/needs order))
    (def request-id (clj-uuid/v1))
    (def commitment-id (clj-uuid/v1))
    (def agreement-id (clj-uuid/v1))
    (def plan-id (clj-uuid/v1)))

  ; 1) process-customer-order    :order/submitted -> :order/accepted
  (let []
    (order->sales-request-view [{:order/id (:order/id order)}
                                (assoc order
                                  :sales/request-id request-id
                                  :order/event :order/accepted)]))
  (order->sales-request (state/db))


  ; 2) process-sales-request    :order/accepted -> :order/committed
  (let [request             (assoc order :sales/request-id (clj-uuid/v1)
                              :order/event :order/submitted)

        allocated-resources []
        time-frame          []
        total-cost          1]
    (order->sales-request-view [{:order/id (:order/id request)}
                                {:order/event           :order/committed
                                 :commitment/id         commitment-id
                                 :commitment/resources  allocated-resources
                                 :commitment/time-frame time-frame
                                 :commitment/cost       total-cost}]))
  (order->sales-request (state/db))


  ; 3) process-sales-commitment    :order/committed -> :order/awaiting-approval
  (let [sales-request-id     request-id
        customer-id          "alice"
        order-id             "alice-order-1"
        order-needs          [0 1]
        agreement-resources  []
        agreement-time-frame []
        agreement-price      100
        agreement-notes      ["note 1" "note 2"]
        sales-agreement      {:agreement/id         agreement-id
                              :customer/id          customer-id
                              :order/id             order-id
                              :order/needs          order-needs
                              :agreement/resources  agreement-resources
                              :agreement/time-frame agreement-time-frame
                              :agreement/price      agreement-price
                              :agreement/notes      agreement-notes}]
    (order->sales-request-view [{:order/id order-id}
                                (assoc sales-agreement
                                  :order/event :order/awaiting-approval)]))
  (order->sales-request (state/db))


  ; 4) process-order-approval    :order/awaiting-approval -> :order/awaiting-fulfilment
  (let [id   (:order/id order)
        plan {:plan/id              plan-id
              :order/id             order-id
              :customer/id          customer-id
              :sales/request-id     request-id
              :commitment/resources [{} {} {}]
              :commitment/cost      1}]
    (order->sales-request-view [{:order/id id}
                                (assoc plan
                                  :order/event :order/awaiting-fulfilment)]))
  (order->sales-request (state/db))


  ; 5) process-plan    :order/awaiting-fulfilment -> :order/purchased
  (let [plan          {:plan/id              plan-id
                       :order/id             order-id
                       :customer/id          customer-id
                       :sales/request-id     request-id
                       :commitment/resources [{} {} {}]
                       :commitment/cost      1}
        expanded-plan ["alpha-googoos" "delta-googoos"]]
    (order->sales-request-view [{:order/id order-id}
                                (assoc plan
                                  :order/event :order/purchased
                                  :order/providers expanded-plan)]))
  (order->sales-request (state/db))


  ; 6) process-shipment    :order/purchased -> :order/fulfilled
  (order->sales-request (state/db))



  ())


; endregion
