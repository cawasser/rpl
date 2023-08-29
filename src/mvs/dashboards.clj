(ns mvs.dashboards
  (:require [mvs.dashboard.monitoring :as monitoring]
            [mvs.dashboard.planning :as planning]
            [mvs.dashboard.sales :as sales]))


(def monitoring-dashboard #'monitoring/monitoring-dashboard)
(def planning-dashboard #'planning/planning-dashboard)
(def sales-dashboard #'sales/sales-dashboard)



(defn customer-dashboard [_ _ _ event]
  (println "CUSTOMER received " event))



(defn provider-dashboard [_ _ _ event]
  (println "PROVIDER " (:provider/id (second event)) " received " event))



(defn billing-dashboard [_ _ _ event]
  (println "BILLING received " event))


(defn customer-support-dashboard [_ _ _ event]
  (println "CUSTOMER SUPPORT received " event))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment

  (renderer)

  ())

; endregion
