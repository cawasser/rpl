(ns mvs.helpers)


(defn make-resource
  "utility to build :resource/definition (spec) compliant structures quickly and programmatically"

  [id [start-time end-time]]

  {:resource/id id :resource/time-frame (into [] (range start-time end-time))})



(defn malformed [service-name expected-spec & structure]
  (println (str service-name " ******** MALFORMED ********  " expected-spec " // " (first structure))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments


; endregion
