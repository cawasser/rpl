(ns mvs.service.process-resource-health
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defmulti resource-health
  "we might decide :resource/status based upon the attribute (we only have Googoos right now)"
  :measurement/attribute)


(defmethod resource-health :googoo/metric [{v :measurement/value}]
  (cond
    (<= 0 v) :status/off-line
    (>= v 100) :status/running
    (> v 100) :status/off-line
    :else :status/unknown))


(defn process-resource-health [_ _ _ [event-key measurement]]

  (println "process-resource-health" event-key " // " measurement)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do
      (let [health-event [event-key
                          (assoc measurement
                            :resource/status (resource-health measurement))]]

        (publish! health-topic health-event)))

    (malformed "process-resource-health" :sales/request measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

(comment
  (do
    (def resource-id (uuid/v1)))


  (publish! resource-measurement-topic
    [{:resource/id resource-id}
     {:measurement/id (uuid/v1)
      :resource/id resource-id
      :measurement/attribute :googoo/metric
      :measurement/value 0}])


  (publish! resource-measurement-topic
    [{:resource/id resource-id}
     {:measurement/id (uuid/v1)
      :resource/id resource-id
      :measurement/attribute :googoo/metric
      :measurement/value 33}])


  (publish! resource-measurement-topic
    [{:resource/id resource-id}
     {:measurement/id (uuid/v1)
      :resource/id resource-id
      :measurement/attribute :googoo/metric
      :measurement/value 107}])



  ())

; endregion

