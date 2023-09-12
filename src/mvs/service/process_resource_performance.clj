(ns mvs.service.process-resource-performance
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :as rm]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :as rm]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(def history-size 10)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; helpers
;

(defn average [coll]
  (println "average" coll)
  (double (/ (reduce + coll) (max (count coll) 1))))


(defn- compute-performance [[event-key {:keys [resource/id measurement/attribute]}]]
  (let [history (get-in (rm/resource-performance (rm/state))
                  [id :resource/performance attribute :performance/history])]
    (average history)))


(defn- update-ktable
  "manage state materialization (reduce/fold update events over time) into a
  'ktable' (currently just an atom) and we'll consider performance over the last 'size'
  events per resource"
  [size [event-key {:keys [resource/id]
                    :as   measurement}]]

  (rm/resource-performance-view [{:resource/id id}
                                 (assoc measurement
                                   :performance/function [(str "average last " size) average]
                                   :history/size size)]))


; endregion


(defn process-resource-performance
  "enriches a :resource/measurement event with an assessment of the resource's
  'performance' (See `compute-performance` for details) using a :resource/performance key

  uses a 'local' ktable (atom) to materialize history for computing the average of the last
  `history-size` events"

  [[event-key measurement :as event]]

  (println "process-resource-performance" event-key " // " measurement)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do
      (let [_                 (update-ktable history-size event)
            perf              {:resource/performance (compute-performance event)}
            performance-event [event-key (merge measurement perf)]]

        (publish! performance-topic performance-event)))

    (malformed "process-resource-performance" :resource/measurement measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; some explorations on mutating ATOMS
(comment

  (def event {:id "r-1" :att "a-1" :val 50})

  (def ktable (atom {"r-1" {"a-1" [10 20 30 40,,,,]}}))

  (let [current (get-in @ktable [(:id event) (:att event)])]
    (conj current (:val event)))


  (assoc-in @ktable [(:id event) (:att event)]
    (conj (get-in @ktable
            [(:id event) (:att event)])
      (:val event)))

  (get-in @ktable ["r-2" (:att event)])

  (update-in @ktable [(:id event) (:att event)] conj,,, 60)
  (update-in @ktable ["r-2" (:att event)] conj,,, 10)


  (swap! ktable assoc-in,,, [(:id event) (:att event)]
    (conj (get-in @ktable
            [(:id event) (:att event)])
      (:val event)))


  (def a (atom {}))
  @a
  (assoc @a :a 10 :b 1000)

  (assoc-in {} [:a :b] 100)

  (swap! a assoc,, :a 10 :b 1000)
  (reset! a {:c 200})




  ())



; try updating the ktable
(comment
  (do
    (def resource-id (uuid/v1))
    (def measurement-id (uuid/v1))
    (def attribute "dummy")
    (def history-size 3)

    (def event [{:resource/id resource-id}
                {:resource/id           resource-id
                 :measurement/id        measurement-id
                 :measurement/attribute attribute
                 :measurement/value     (rand-int 100)}]))

  (update-ktable history-size [{:resource/id resource-id}
                               {:resource/id           resource-id
                                :measurement/id        measurement-id
                                :measurement/attribute attribute
                                :measurement/value     (rand-int 100)}])


  ())



; compute average
(comment
  (do
    (def event []))

  (def history (get-in (rm/resource-performance (rm/state))
                 [#uuid"3b7eecc0-50e9-11ee-bfe7-91e057b1b221"
                  :resource/performance
                  :googoo/metric]))


  (compute-performance (rm/resource-performance (rm/state))
    [{:resource/id #uuid"3b7eecc0-50e9-11ee-bfe7-91e057b1b221"}
     {:resource/id #uuid"3b7eecc0-50e9-11ee-bfe7-91e057b1b221"
      :measurement/attribute :googoo/metric}])


  (def measurement @last-event)


  ())

; endregion

