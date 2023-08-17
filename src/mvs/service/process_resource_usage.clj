(ns mvs.service.process-resource-usage
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.read-models :refer :all]
            [mvs.specs :refer :all]
            [clj-uuid :as uuid]))

(def last-event (atom []))


(defn process-resource-usage [_ _ _ [event-key measurement]]

  (println "process-resource-usage" event-key " // " measurement)

  (reset! last-event measurement)

  (if (spec/valid? :resource/measurement measurement)
    (do)

    (malformed "process-resource-usage" :sales/request measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;


; endregion
