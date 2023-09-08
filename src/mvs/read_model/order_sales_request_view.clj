(ns mvs.read-model.order-sales-request-view
  (:require [mvs.constants :refer :all]
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
    assoc (:order/id content) content))


; :order/reserved (process-sales-request)
(defmethod e/event-handler :order/reserved
  [{event-key                         :event/key
    {:keys [order/event ] :as content} :event/content :as params}]

  (println ":order/reserved" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update (:order/id content)
    #(-> %)))


; :order/awaiting-approval (process-sales-commitment)
(defmethod e/event-handler :order/awaiting-approval
  [{event-key                         :event/key
    {:keys [order/event agreement/id agreement/resources] :as content} :event/content :as params}]

  (println ":order/awaiting-approval" event-key "//" event "//" content)

  (swap! state/app-db
    update (:order/id content)
    #(-> %
       (assoc :agreement/id id)
       (assoc :agreement/resources resources))))





(defmethod e/event-handler :order/approved
  [{event-key                         :event/key
    {:keys [order/event] :as content} :event/content :as params}]

  (println ":order/approved" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update (:order/id content)
    #(-> %
       (assoc :order/status :order/approved))))


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
  (order->sales-request (state/db))

  (do
    (def local (atom {}))
    (def event [{:customer/id #uuid"ed6fc000-4e53-11ee-bf37-acef1c2857e1",
                 :order/id    #uuid"ed6fc004-4e53-11ee-bf37-acef1c2857e1"}
                {:customer/id  #uuid"ed6fc000-4e53-11ee-bf37-acef1c2857e1",
                 :order/id     #uuid"ed6fc004-4e53-11ee-bf37-acef1c2857e1",
                 :order/status :order/submitted,
                 :order/needs  [0 1]}])
    (def event-key (first event))
    (def order (second event))
    (def request-id "dummy-sales-request")
    (def request-id "dummy-sales-request")
    (def request-id "dummy-sales-request"))

  ; :order/submitted (process-customer-order)
  (swap! local
    assoc (:order/id order) order)

  (order->sales-request-view [{:order-id (:order/id order)}
                              (assoc order
                                :sales/request-id request-id
                                :order/event :order/submitted)])


  ; :order/reserved (process-sales-request)
  (swap! local
    update (:order/id order)
    #(-> %
       (assoc :agreement/id "dummy-agreement")
       (assoc :agreement/resources [{} {} {}])))

  (order->sales-request-view [{:order-id (:order/id order)}
                              (assoc order
                                :order/event :order/reserved
                                :agreement/id "dummy-agreement"
                                :agreement/resources [{} {} {}])])


  ; :order/approved (process-order-approval)
  (swap! local
    update (:order/id order)
    (-> %
      (assoc :order/status :order/approved)))



  ; :order/shipped (process-shipment)
  (swap! local
    update (:order/id order)
    (-> %
      (assoc :order/status :order/shipped)))

  ())


; endregion
