(ns mvs.dashboards)


(defn customer-dashboard [_ _ _ event]
  (println "CUSTOMER received " event))



(defn provider-dashboard [_ _ _ event]
  (println "PROVIDER " (:provider/id (second event)) " received " event))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments


; endregion
