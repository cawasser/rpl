(ns mvs.read-model.order-sales-request-view
  (:require [mvs.constants :as const]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))


; order/submitted (process-customer-order)
(defmethod e/event-handler :order/submitted
  [{event-key                         :event/key
    {:keys [order/event] :as content} :event/content :as params}]

  (println ":order/submitted" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    assoc-in [:order->sales-request-view (:order/id content)] content))


; :order/committed (process-sales-request)
(defmethod e/event-handler :order/committed
  [{event-key                                                   :event/key
    {:keys [order/event
            commitment/id commitment/resources
            commitment/time-frame commitment/cost] :as content} :event/content :as params}]

  (println ":order/reserved" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id content)]
    #(-> %
       (assoc :order/status :order/committed
         :commitment/id id
         :commitment/resources resources
         :commitment/time-frame time-frame
         :commitment/cost cost))))


; :order/awaiting-approval (process-sales-commitment)
(defmethod e/event-handler :order/awaiting-approval
  [{event-key                                                          :event/key
    {:keys [order/event agreement/id agreement/resources] :as content} :event/content :as event}]

  (println ":order/awaiting-approval" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in [:order->sales-request-view (:order/id content)]
    #(-> %
       (assoc :order/status :order/awaiting-approval
         :agreement/id id
         :agreement/resources resources))))


; :order/approved (process-order-approval)
(defmethod e/event-handler :order/approved
  [{event-key                                 :event/key
    {:keys [order/event plan/id] :as content} :event/content :as params}]

  (println ":order/approved" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update (:order/id content)
    #(-> %
       (assoc :order/status :order/approved
         :plan/id id))))



; :order/purchased (process-plan)
(defmethod e/event-handler :order/purchased
  [{event-key                         :event/key
    {:keys [order/event] :as content} :event/content :as params}]

  (println ":order/purchased" event-key "//" event "//" content))


; :order/fulfilled (process-shipment)
(defmethod e/event-handler :order/fulfilled
  [{event-key                         :event/key
    {:keys [order/event] :as content} :event/content :as params}]

  (println ":order/fulfilled" event-key "//" event "//" content))



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
                 :order/status :order/submitted,
                 :order/needs  [0 1]}])
    (def event-key (first event))
    (def order (second event)))

  ; :order/submitted (process-customer-order)
  (swap! local
    assoc (:order/id order) order)

  (order->sales-request-view [{:order-id (:order/id order)}
                              (assoc order
                                :sales/request-id "dummy-sales-request"
                                :order/event :order/submitted)])
  (order->sales-request (state/db))


  ; :order/reserved (process-sales-request)
  (do
    (def sales-request-id (-> (:order/id order) h/associated-sales-request :sales/request-id))
    (def assoc-agreement (h/associated-order sales-request-id))
    (def resources (:agreement/resources assoc-agreement))
    (def commitment-id "dummy-commitment")
    (def request-id "dummy-sales-request")
    (def time-frames [])
    (def total-cost 1000))

  (swap! local
    update (:order/id order)
    #(-> %
       (assoc :commitment/id commitment-id)
       (assoc :commitment/resources [{}])
       (assoc :commitment/time-frame [1 2 3 4 5])
       (assoc :commitment/cost 1000)))

  (order->sales-request-view [{:order-id (:order/id order)}
                              (assoc order
                                :order/event :order/reserved
                                :commitment/id commitment-id
                                :commitment/resources resources
                                :commitment/time-frame time-frames
                                :commitment/cost total-cost)])
  (order->sales-request (state/db))



  ; :order/awaiting-approval (process-sales-commitment)
  (swap! local
    update (:order/id order)
    #(-> %
       (assoc :agreement/id "dummy-agreement")
       (assoc :agreement/resources [{} {} {}])))

  (order->sales-request-view [{:order-id (:order/id order)}
                              (assoc order
                                :order/event :order/reserved)])

  (order->sales-request (state/db))



  ; :order/approved (process-order-approval)
  (swap! local
    update (:order/id order)
    (-> %
      (assoc :order/status :order/approved)))


  (let []
    (order->sales-request-view [{:order-id (:order/id order)}
                                (assoc order
                                  :order/event :order/approved
                                  :commitment/resources resources)]))


  ; :order/shipped (process-shipment)
  (swap! local
    update (:order/id order)
    (-> %
      (assoc :order/status :order/shipped)))

  ())


; endregion
