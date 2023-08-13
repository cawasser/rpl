(ns mvs.topics)


(def provider-catalog-topic (atom []))
(def sales-catalog-topic (atom []))
(def customer-order-topic (atom []))
(def sales-request-topic (atom []))
(def sales-commitment-topic (atom []))
(def sales-failure-topic (atom []))
(def service-agreement-topic (atom []))
(def plan-topic (atom []))
(def fix-topic (atom []))
(def configure-provider-topic (atom []))
(def configure-monitoring-topic (atom []))
(def measurement-topic (atom []))
(def health-topic (atom []))
(def performance-topic (atom []))
(def usage-topic (atom []))


(defn publish! [topic message]
  (reset! topic message))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments


; endregion
