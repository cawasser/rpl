(ns mvs.read-model.resource-measurements-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))



(defmethod e/event-handler :resource-measurement
  [{event-key                               :event/key
    {:keys [resource/id
            measurement/attribute
            measurement/value] :as content} :event/content :as params}]

  (println ":resource-measurement" event-key "//" content)

  ; measurements are a "time ordered" collection of values, so we can see
  ; history (materialized view)

  (swap! state/app-db
    fx/swap-context
    update-in
    [:resource-measurements-view id :resource/measurements attribute]
    conj value))


(defn resource-measurements [context]
  (println "resource-measurements" context)
  (fx/sub-val context :resource-measurements-view))


(defn resource-measurements-view [[event-key event-content :as event]]
  (println "resource-measurements-view" event)
  (e/event-handler {:event/type    :resource-measurement
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-resource-measurements-view []
  (swap! state/app-db
    fx/swap-context
    assoc :resource-measurements-view {}))






(comment
  (resource-measurements @state/app-db)




  ())