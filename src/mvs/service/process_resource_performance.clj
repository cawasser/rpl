(ns mvs.service.process-resource-performance
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(def history-size 10)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; helpers
;


(defn- update-ktable
  "manage state materialization (reduce/fold update events over time) into a
  'ktable' (currently just an atom) and we'll consider performance over the last 'size'
  events per resource"
  [ktable size [event-key {:keys [resource/id measurement/attribute measurement/value]
                           :as   measurement}]]

  (swap! ktable assoc-in [id attribute]
    (conj (or (into []
                (as-> @ktable x
                  (get-in x [id attribute])
                  (take-last (dec size) x)))
            [])
      value)))


(defn average [coll]
  (double (/ (reduce + coll) (count coll))))


(defn- compute-performance [ktable [event-key {:keys [resource/id measurement/attribute]}]]
  (let [history (get-in @ktable [id attribute])]
    {:resource/performance (average history)}))

; endregion


(defn process-resource-performance
  "enriches a :resource/measurement event with an assessment of the resource's
  'performance' (See `compute-performance` for details) using a :resource/performance key

  uses a 'local' ktable (atom) to materialize history for computing the average of the last
  `history-size` events"

  [_ _ _ [event-key measurement :as event]]

  (println "process-resource-performance" event-key " // " measurement)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do
      (let [_                 (update-ktable resource-performance-view history-size event)
            perf              (compute-performance resource-performance-view event)
            performance-event [event-key (merge measurement perf)]]

        (publish! performance-topic performance-event)))

    (malformed "process-resource-performance" :resource/measurement measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; test the "circular buffer" and average
(comment
  (do
    (def view (atom {}))
    (def resource-id #uuid"74bb49c1-4095-11ee-84d5-efb2362fd299"))

  (update-ktable view 3 [{:resource/id resource-id}
                         {:resource/id resource-id
                          :measurement/attribute :googoo/metric
                          :measurement/value 10}])
  (average (get-in @view [resource-id :googoo/metric])) ; 10

  (update-ktable view 3 [{:resource/id resource-id}
                         {:resource/id resource-id
                          :measurement/attribute :googoo/metric
                          :measurement/value 20}])
  (average (get-in @view [resource-id :googoo/metric])) ; 13

  (update-ktable view 3 [{:resource/id resource-id}
                         {:resource/id resource-id
                          :measurement/attribute :googoo/metric
                          :measurement/value 30}])
  (average (get-in @view [resource-id :googoo/metric])) ; 20

  (update-ktable view 3 [{:resource/id resource-id}
                         {:resource/id resource-id
                          :measurement/attribute :googoo/metric
                          :measurement/value 40}])
  (average (get-in @view [resource-id :googoo/metric])) 30

  (update-ktable view 3 [{:resource/id resource-id}
                         {:resource/id resource-id
                          :measurement/attribute :googoo/metric
                          :measurement/value 50}])
  (average (get-in @view [resource-id :googoo/metric])) ; 40

  ())


; endregion

