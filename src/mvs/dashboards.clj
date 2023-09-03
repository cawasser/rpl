(ns mvs.dashboards
  (:require [mvs.dashboard.monitoring :as monitoring]
            [mvs.dashboard.planning :as planning]
            [mvs.dashboard.sales :as sales]))


(def monitoring-dashboard #'monitoring/monitoring-dashboard)
(def planning-dashboard #'planning/planning-dashboard)
(def sales-dashboard #'sales/dashboard)



(defn customer-dashboard [event]
  (println "CUSTOMER received " event))



(defn provider-dashboard [event]
  (println "PROVIDER " (:provider/id (second event)) " received " event))



(defn billing-dashboard [event]
  (println "BILLING received " event))


(defn customer-support-dashboard [event]
  (println "CUSTOMER SUPPORT received " event))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment

  (renderer)

  ())

; endregion
