(ns mvs.read-model.resource-measurements-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-models :as state]
            [mvs.read-model.event-handler :as e]))



(defn- update [current event new-value]
  ; planning for the future...
  ;       more flexible changes to an existing catalog
  (println "update-catalog (a)"
    event)
  ;current new-value "//"
  ;(conj current new-value))

  (condp = event
    :catalog/add-service (conj current new-value)
    :catalog/time-revision current
    :catalog/cost-revision current
    new-value))


(defmethod e/event-handler :resource-measurement
  [{event-key                             :event/key
    {:keys [catalog/event resource/id] :as content} :event/content :as params}]

  (println ":sales-catalog" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update-in
    [:resource-measurements-view id]
    update event (dissoc content :catalog/event)))


(defn resource-measurements [context]
  (fx/sub-val context :resource-measurements-view))


(defn resource-measurements-view [[event-key event-content :as event]]
  (println "resource-measurements-view" event)
  (e/event-handler {:event/type    :resource-measurement
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-sales-catalog-view []
  (swap! state/app-db
    fx/swap-context
    assoc :sales-catalog-view []))






(comment
  (resource-measurements @state/app-db)


  ())