(ns mvs.dashboards
  (:require [mvs.dashboard.sales :as sales]))


(def sales-dashboard #'sales/sales-dashboard)



(defn customer-dashboard [_ _ _ event]
  (println "CUSTOMER received " event))



(defn provider-dashboard [_ _ _ event]
  (println "PROVIDER " (:provider/id (second event)) " received " event))



(defn monitoring-dashboard [_ _ _ event]
  (println "MONITORING received " event))



(defn billing-dashboard [_ _ _ event]
  (println "BILLING received " event))


(defn planning-dashboard [_ _ _ event]
  (println "PLANNING received " event))


(defn customer-support-dashboard [_ _ _ event]
  (println "CUSTOMER SUPPORT received " event))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments


; endregion
