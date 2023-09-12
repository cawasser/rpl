(ns mvs.read-model.resource-usage-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))



; :resource/usage (process-resource-usage)
;
; the idea here is ot keep track of all the :resource/ids that report
;  metrics, count them , and report 'usage' as the percentage of the
;  purchased resources that have reported.
;
;  For example, if the order is for 5 resources and 3 of them hav reported
;  at least once, the SLA will be 0.60 or 60%
;
(defmethod e/event-handler :resource/usage
  [{event-key             :event/key
    {resource-id :resource/id
     customer-id :customer/id
     order-id    :order/id
     :as         content} :event/content :as params}]

  (println ":resource/usage" event-key "//" content)

  ; usage are a "time ordered" collection of values, so we can see
  ; history (materialized view)

  (swap! state/app-db
    fx/swap-context
    update-in
    [:resource-usage-view customer-id order-id]
    #(-> %
       (conj resource-id)
       set)))


(defn resource-usage [context]
  (println "resource-usage" context)
  (fx/sub-val context :resource-usage-view))


(defn resource-usage-view [[event-key event-content :as event]]
  (println "resource-usage-view" event)
  (e/event-handler {:event/type    :resource/usage
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-resource-usage-view []
  (swap! state/app-db
    fx/swap-context
    assoc :resource-usage-view {}))




