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
(def shipment-topic "provider -> config" (atom []))
(def customer-delivery-topic "provider -> customer" (atom []))
(def resource-measurement-topic "provider -> monitoring" (atom []))
(def measurement-topic (atom []))
(def health-topic (atom []))
(def performance-topic (atom []))
(def usage-topic (atom []))


(defn- topic-name [topic]
  ;(println "topic-name" topic "//" *ns*)
  (:name (meta (second (first (filter #(and (var? (second %))
                                         (= topic (var-get (second %))))
                                (ns-map 'mvs.topics)))))))


(defn publish! [topic [msg-key content :as message]]
  (println "publish! " (topic-name topic) " // " msg-key)
  ;(println "publish! (b)" topic)

  (reset! topic message))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; look at 'watchers'
(comment
  (def x (atom 0))


  (add-watch x :watcher
    (fn [key atom old-state new-state]
      (println "new-state" new-state)))

  (reset! x 2)



  ())


; how to get the topic's name from the parameter
(comment
  (:name (meta #'provider-catalog-topic))

  (:name (meta #'sales-catalog-topic))

  (let [v provider-catalog-topic]
    (var v))


  (topic-name provider-catalog-topic)

  (let [x provider-catalog-topic]
    (topic-name x))


  (topic-name resource-measurement-topic)

  (def topic #'resource-measurement-topic)

  (filter #(and (var? (second %))
             (= resource-measurement-topic (var-get (second %))))
    (ns-map *ns*))

  (ns-map #'mvs.topics)

  (:name (meta (second (first (filter #(and (var? (second %))
                                         (= resource-measurement-topic (var-get (second %))))
                                (ns-map *ns*))))))

  (let [x provider-catalog-topic]
    (:name (meta (second (first (filter #(and (var? (second %))
                                            (= x (var-get (second %))))
                                   (ns-map *ns*)))))))

  (let [x resource-measurement-topic]
    (topic-name x))

  ())


(comment
  (meta #'topic-name)


  ())


; endregion
