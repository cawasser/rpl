(ns mvs.topics)


(def provider-catalog-topic "provider -> [sales, planning]" (atom []))
(def sales-catalog-topic "sales -> customer" (atom []))
(def customer-order-topic "customer -> sales" (atom []))
(def sales-request-topic "sales -> planning" (atom []))
(def sales-commitment-topic "planning -> sales" (atom []))
(def sales-failure-topic "planning -> sales" (atom []))
(def sales-agreement-topic "sales -> customer" (atom []))
(def customer-order-approval (atom []))
(def plan-topic (atom []))
(def fix-topic (atom []))
(def provider-order-topic (atom []))
(def resource-measurement-topic (atom []))
(def measurement-topic (atom []))
(def health-topic (atom []))
(def performance-topic (atom []))
(def usage-topic (atom []))


(defn- topic-name [topic]
  (:name (meta (second (first (filter #(and (var? (second %))
                                         (= topic (var-get (second %))))
                                (ns-map *ns*)))))))


(defn publish! [topic [msg-key content :as message]]
  (println "publish! " (topic-name topic) " // " msg-key)
  (reset! topic message))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments


(comment
  (:name (meta #'provider-catalog-topic))

  (:name (meta #'sales-catalog-topic))

  (let [v provider-catalog-topic]
    (var v))

  (comment
    (topic-name provider-catalog-topic)

    (let [x provider-catalog-topic]
      (topic-name x))


    ())



  (let [x provider-catalog-topic]
    (:name (meta (second (first (filter #(and (var? (second %))
                                            (= x (var-get (second %))))
                                   (ns-map *ns*)))))))
  ())

; endregion
