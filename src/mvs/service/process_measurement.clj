(ns mvs.service.process-measurement
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-measurement [_ _ _ [event-key {:keys [resource/id
                                                    measurement/attribute
                                                    measurement/value]
                                             :as   measurement}]]

  (println "process-measurement" event-key " // " measurement)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do


      ; 1) update the resource-state-view with the new data
      (swap! resource-state-view
        assoc-in [id :resource/measurements attribute]

        ; measurements are a "time ordered" collection of values, so we can see
        ; history (materialized view)
        (conj (or (get-in @resource-state-view [id :resource/measurements attribute])
                [])
          value)))

      ; 2) republish this data upstream (?)

    (malformed "process-measurement" :resource/measurement measurement)))





(comment
  (do
    (def view (atom @resource-state-view))
    (def resource-id (-> @resource-state-view keys first))
    (def id resource-id)
    (def attribute :googoo/metric)
    (def value 100)
    (def measurement {:measurement/id        (uuid/v1)
                      :resource/id           resource-id
                      :measurement/attribute attribute
                      :measurement/value     value}))


  (publish! resource-measurement-topic [{:resource/id resource-id}
                                        measurement])

  (spec/explain :resource/measurement @last-event)


  (conj (or (get-in @resource-state-view [id :resource/measurements attribute])
          [])
    value)

  (swap! view
    assoc-in [id :resource/measurements attribute] value)

  ()

  ())

