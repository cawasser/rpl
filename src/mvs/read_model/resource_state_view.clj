(ns mvs.read-model.resource-state-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))


(defmethod e/event-handler :resource-state
  [{{:keys [resource/id] :as event-key} :event/key
    content                             :event/content :as event}]

  (println ":resource-state" event-key "//" content)

  (swap! state/app-db
    fx/swap-context
    assoc-in [:resource-state-view id] content))


(defn resource-states [context]
  (fx/sub-val context :resource-state-view))


(defn resource-state-view [[event-key event-content :as event]]
  (println "resource-state-view" event-key "//" event-content)
  (e/event-handler {:event/type    :resource-state
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-resource-state-view []
  (swap! state/app-db
    fx/swap-context
    assoc :resource-state-view {}))




(comment

  (resource-states (state/db))

  (def state-event [{:resource/id #uuid"b4337b81-4dbb-11ee-aeab-b9081dfd246f"}
                    {:resource/attributes   {:resource/type 0, :resource/time 0},
                     :resource/measurements {},
                     :provider/id           "delta-googoos",
                     :order/id              nil,
                     :customer/id           nil,
                     :sales/request-id      nil,
                     :agreement/id          nil,
                     :order/needs           nil}])

  (resource-state-view state-event)
  (reset-resource-state-view)




  ())
