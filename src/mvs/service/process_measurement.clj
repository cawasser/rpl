(ns mvs.service.process-measurement
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-model.resource-measurements-view :as mv]
            [mvs.read-models :refer :all]
            [mvs.read-model.state :as state]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-measurement
  "accepts a :resource/measurement, materialized it into resource-state-view,
  and enriches it with:

  - :order/id
  - :customer/id
  - :sales/request-id
  - :agreement/id
  - :order/needs

  all of which are found in resource-state-view. The enriched event is then
  published to measurement-topic for further downstream analytical processing.

  > NOTE: the inbound event is keyed by :resource/id, but this is domain specific
  "
  [[event-key {:keys [resource/id
                      measurement/attribute
                      measurement/value]
               :as   measurement}
    :as event]]

  (println "process-measurement" event-key " // " measurement)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do
      ; 1) update the resource-measurement-view with the new data
      (mv/resource-measurements-view event)
      ;(swap! resource-state-view
      ;  assoc-in [id :resource/measurements attribute]
      ;
      ;  ; measurements are a "time ordered" collection of values, so we can see
      ;  ; history (materialized view)
      ;  (conj (or (get-in @resource-state-view [id :resource/measurements attribute])
      ;          [])
      ;    value))

      ; 2) republish this data upstream (?)
      ;       enriched with :order/id, :customer/id, :sales/request-id, etc. which can be acquired from
      ;       @resource-state-view
      ;
      (let [enrichment (->> (get (mv/resource-measurements @state/app-db) id)
                         ((juxt :order/id :customer/id :sales/request-id
                            :agreement/id :order/needs))
                         (zipmap [:order/id :customer/id :sales/request-id
                                  :agreement/id :order/needs]))]
        (publish! measurement-topic [event-key (merge measurement enrichment)])))

    (malformed "process-measurement" :resource/measurement measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;


; convert to app-db
(comment
  (do
    (def measurements (mv/resource-measurements @state/app-db)))




  ())


(comment
  (do
    (def view (atom @resource-state-view))
    (def resource-id (-> @resource-state-view keys first))
    (def event-key {:resource/id resource-id})
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

  (get @resource-state-view id)
  (get-in @resource-state-view [id :resource/measurements])

  (conj (or (get-in @resource-state-view [id :resource/measurements attribute])
          [])
    value)

  (swap! view
    assoc-in [id :resource/measurements attribute]
    (conj (or (get-in @resource-state-view [id :resource/measurements attribute])
            [])
      value))


  (def enrichment (->> (get @resource-state-view id)
                    ((juxt :order/id :customer/id :sales/request-id
                       :agreement/id :order/needs))
                    (zipmap [:order/id :customer/id :sales/request-id
                             :agreement/id :order/needs])))


  ())


(comment
  (spec/explain :resource/measurement @last-event)


  ())

; endregion

