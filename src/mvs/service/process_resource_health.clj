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


(defn process-resource-health [_ _ _ [event-key measurement]]

  (println "process-resource-health" event-key " // " measurement)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do)

    (malformed "process-resource-health" :sales/request measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;


; endregion

