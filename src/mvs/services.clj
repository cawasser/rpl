(ns mvs.services
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(defn process-producer-catalog
  "takes a (spec) `:provider/catalog`

  i.e., tuple of hash-maps (key and message-content) with the producer encoded inside the 'key'
  and the catalog itself being the message-content"

  [k _ _ [{:keys [:provider/id]} catalog :as params]]

  (println "process-producer-catalog" k)

  ; 1) update provider-catalog-view
  (swap! provider-catalog-view assoc id catalog)

  ; 2) 'publish' ACME's "Service Catalog"
  (publish! service-catalog-view service-catalog))


(defn available-resources
  [k _ _ [{:keys [:provider/id]} catalog :as params]]

  (println "available-resources" k)

  ; update with new data, arranged by:
  ;     a) :resource/id
  ;     b) :resource/time-frame
  ;     c) :resource/cost
  ;
  (let [by-id   (->> catalog)
        by-time (->> catalog)
        by-cost (->> catalog)]

    (reset! available-resources-view
      (-> @available-resources-view
        (assoc :by-id by-id)
        (assoc :by-time by-time)
        (assoc :by-cost by-cost)))))


(defn process-customer-request
  "all we need to do here is

   1) assign an ACME :service/id to this request
   2) enrich with the actual resources associated with the chosen services

   and pass it along"

  [_ _ _ request]

  (println "process-customer-request" (:customer/request-id request))

  (let [request-id (uuid/v1)
        resources (->> request
                    :customer/needs
                    (mapcat (fn [service-id]
                              (:service/elements
                                (first
                                  (filter #(= (:service/id %) service-id) @service-catalog-view)))))
                    (into []))]

    (publish! service-request-topic [{:service/request-id request-id
                                      :customer/request-id (:customer/request-id request)
                                      :customer/id (:customer/id request)}
                                     {:service/request-id request-id
                                      :customer/request-id (:customer/request-id request)
                                      :customer/id (:customer/id request)
                                      :customer/needs (:customer/needs request)
                                      :service/resources resources}])))


(defn process-service-request
  "this function takes the request and tries to allocate system resources (Googoos) form the
  various providers to satisfy the expressed need(s)"
  [_ _ _ request]

  (println "process-service-request" (:service/request-id request) "//" (:customer/needs request)))




(defn process-customer-commitment
  "this function takes a service-commitment form 'planning' and enriches it for submision
  to the customer for approval"
  [_ _ _ commitment]

  (println "process-customer-commitment"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; test process-producer-catalog
(comment
  (def event-1 [{:provider/id "alpha"} provider-alpha])
  (def event-2 [{:provider/id "bravo"} provider-bravo])

  @provider-catalog-view
  (reset! provider-catalog-view {})

  (process-producer-catalog [] [] [] event-1)
  (process-producer-catalog [] [] [] event-2)
  (process-producer-catalog [] [] [] [{:provider/id "charlie"} provider-charlie])
  (process-producer-catalog [] [] [] [{:provider/id "delta"} provider-delta])
  (process-producer-catalog [] [] [] [{:provider/id "echo"} provider-echo])

  ())


; check specs for :customer/service-request
(comment
  (spec/explain :customer/needs [0 1])
  (spec/explain :customer/service-request {:customer/id (uuid/v1)
                                           :customer/request-id (uuid/v1)
                                           :customer/needs [0 1]})


  ())


; build a :service/request form a :customer/request
(comment
  (def request {:service/request-id (uuid/v1)
                :customer/request-id (uuid/v1)
                :customer/needs [0 1]
                :service/resources [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
                                    {:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]})

  (def service-id 0)
  (->> request
    :customer/needs
    (mapcat (fn [service-id]
              (:service/elements
                (first
                  (filter #(= (:service/id %) service-id) @service-catalog-view)))))
    (into []))

  ())



; endregion

