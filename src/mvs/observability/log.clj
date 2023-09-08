(ns mvs.observability.log
  "build out a concept of observing the internal workings
  of the topology, using tooling inspired by Ryan Robitaille's Seajure
  September 2023 demo."
  (:require [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]
            [cljfx.api :as fx]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; ui event handling

(defmethod e/event-handler :activity-log
  [{event-key :event/key
    content   :event/content :as params}]

  (println ":sales-catalog" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update
    :activity-log
    conj content))


(defn activity-log [context]
  (fx/sub-val context :activity-log))


(defn activity-log-view [[event-key event-content :as event]]
  (println "sales-catalog-view" event)
  (e/event-handler {:event/type    :activity-log
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-activity-log-view []
  (swap! state/app-db
    fx/swap-context
    assoc :activity-log []))



; endregion


(defn log
  "'who' is reporting, 'what' are they doing, and then we'll add a 'when' are they reporting"
  [who what])





