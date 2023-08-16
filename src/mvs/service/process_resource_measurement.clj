(ns mvs.service.process-resource-measurement
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-resource-measurement [_ _ _ [{:keys [resoruce/id] :as event-key}
                                           {:keys [resource/id
                                                   resource/time-frame
                                                   measurement/value]
                                            :as   measurement}]]

  (println "process-resource-measurement" event-key)

  (if (spec/valid? :resource/measurement measurement)
    (do)


    (malformed "process-resource-measurement" :resource/measurement measurement)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;


; endregion

