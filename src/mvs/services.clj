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
  [k _ _ [{provider-id :provider/id} catalog :as params]]

  (println "available-resources (a)" k "//" provider-id)

  (let [new-values (->> catalog
                     (map (fn [{:keys [resource/id resource/time-frames]}]
                            {id (into #{}
                                  (map (fn [t]
                                         {t provider-id})
                                    time-frames))}))
                     (into {}))]

    (swap! available-resources-view #(merge-with into %1 %2) new-values)))


(defn process-customer-request
  "all we need to do here is

   1) assign an ACME :service/request-id to this request
   2) enrich with the actual resources associated with the chosen services

   and pass it along"

  [_ _ _ request]

  (println "process-customer-request" (:customer/request-id request))

  (let [request-id (uuid/v1)
        resources  (->> request
                     :customer/needs
                     (mapcat (fn [service-id]
                               (:service/elements
                                 (first
                                   (filter #(= (:service/id %) service-id) @service-catalog-view)))))
                     (into []))]

    (publish! service-request-topic [{:service/request-id  request-id
                                      :customer/request-id (:customer/request-id request)
                                      :customer/id         (:customer/id request)}
                                     {:service/request-id  request-id
                                      :customer/request-id (:customer/request-id request)
                                      :customer/id         (:customer/id request)
                                      :customer/needs      (:customer/needs request)
                                      :service/resources   resources}])))



(defn- allocate [resource-id time-t]
  (let [allocation (as-> @available-resources-view m
                     (get m resource-id)
                     (mapcat seq m)
                     (filter (fn [[k v]] (= time-t k)) m)
                     (map (fn [[k v]] {k v}) m)
                     (set m)
                     (first m))]
    (swap! available-resources-view update 0 disj allocation)))


(defn process-service-request
  "this function takes the request and tries to allocate system resources (Googoos)
  from the various providers to satisfy the expressed need(s)"

  [_ _ _ request]

  ; TODO: where does process-service-request get the available-resources?
  @available-resources-view

  (println "process-service-request"
    (:service/request-id request) "//" (:customer/needs request))

  (let [customer-actual-needs (:service/definition request)]))




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
  (spec/explain :customer/service-request {:customer/id         (uuid/v1)
                                           :customer/request-id (uuid/v1)
                                           :customer/needs      [0 1]})


  ())


; build available-resources-view from provider-catalogs
(comment
  (do
    (def provider-a [{:provider/id "alpha"} mvs.read-models/provider-alpha])
    (def provider-b [{:provider/id "bravo"} mvs.read-models/provider-bravo])

    (def local-available-resources (atom {})))


  (let [[m-key m-content] provider-a
        new-values (->> m-content
                     (map (fn [{:keys [resource/id resource/time-frames]}]
                            {id (into #{}
                                  (map (fn [t]
                                         {t (:provider/id m-key)})
                                    time-frames))}))
                     (into {}))]
    (swap! local-available-resources #(merge-with into %1 %2) new-values))

  (merge {} {0 #{{0 :a} {1 :a} {2 :a}}
             1 #{{0 :a} {1 :a} {2 :a}}})
  (merge-with into
    {0 #{{0 :a} {1 :a} {2 :a}}
     1 #{{0 :a} {1 :a} {2 :a}}}
    {0 #{{0 :b} {1 :b} {2 :b}}
     1 #{{0 :b} {1 :b} {2 :b}}})



  (apply merge [0 1 2 3] [0 4 5 6])
  (apply conj [0 1 2 3] [0 4 5 6])


  (defn process [catalog]
    (let [[m-key m-content] catalog
          new-values (->> m-content
                       (map (fn [{:keys [resource/id resource/time-frames]}]
                              {id time-frames}))
                       (into {}))]
      (swap! local-available-resources #(merge-with into %1 %2) new-values)))

  (process provider-a)
  (process provider-b)


  ())


; build a :service/request from a :customer/request
(comment
  (def request {:service/request-id  (uuid/v1)
                :customer/request-id (uuid/v1)
                :customer/needs      [0 1]
                :service/resources   [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
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


; process-service-request
(comment
  (do
    (def customer-id (uuid/v1))
    (def customer-request-id (uuid/v1))
    (def request-id (uuid/v1))

    (def request [{:service/request-id  request-id
                   :customer/request-id customer-request-id
                   :customer/id         customer-id}
                  {:service/request-id  request-id
                   :customer/request-id customer-request-id
                   :customer/id         customer-id
                   :customer/needs      [0 1]
                   :service/resources   [{:resource/id 0
                                          :resource/time-frames [0 1 2 3 4 5]}
                                         {:resource/id 1
                                          :resource/time-frames [0 1 2 3 4 5]}]}])

    (def request-key (first request))
    (def request-content (second request))

    (def local-available-resources-view {0 #{{5 "alpha"} {4 "echo"} {0 "delta"} {2 "delta"} {0 "alpha"}
                                             {3 "bravo"} {3 "echo"} {1 "alpha"} {5 "bravo"} {1 "delta"}
                                             {2 "charlie"} {5 "charlie"} {4 "charlie"} {1 "bravo"} {4 "alpha"}
                                             {3 "alpha"} {2 "alpha"}},
                                         1 #{{5 "alpha"} {4 "echo"} {0 "delta"} {2 "delta"} {0 "alpha"}
                                             {3 "bravo"} {3 "echo"} {1 "alpha"} {5 "bravo"} {1 "delta"}
                                             {2 "charlie"} {5 "charlie"} {4 "charlie"} {1 "bravo"} {4 "alpha"}
                                             {3 "alpha"} {2 "alpha"}},
                                         2 #{{5 "alpha"} {4 "echo"} {0 "delta"} {2 "delta"} {0 "alpha"} {3 "bravo"}
                                             {3 "echo"} {1 "alpha"} {5 "bravo"} {1 "delta"} {2 "charlie"} {5 "charlie"}
                                             {4 "charlie"} {1 "bravo"} {4 "alpha"} {3 "alpha"} {2 "alpha"}},
                                         3 #{{5 "alpha"} {4 "echo"} {0 "delta"} {2 "delta"} {0 "alpha"} {3 "bravo"}
                                             {3 "echo"} {1 "alpha"} {5 "bravo"} {1 "delta"} {2 "charlie"} {5 "charlie"}
                                             {4 "charlie"} {1 "bravo"} {4 "alpha"} {3 "alpha"} {2 "alpha"}},
                                         4 #{{5 "alpha"} {4 "echo"} {0 "delta"} {2 "delta"} {0 "alpha"} {3 "bravo"}
                                             {3 "echo"} {1 "alpha"} {5 "bravo"} {1 "delta"} {2 "charlie"} {5 "charlie"}
                                             {4 "charlie"} {1 "bravo"} {4 "alpha"} {3 "alpha"} {2 "alpha"}}}))


  (def customer-actual-needs (:service/resources request-content))

  (key {5 "alpha"})

  (map (fn [[k v :as m]]
         {:m m :k k :v v})
    (mapcat seq #{{5 "alpha"} {4 "echo"} {0 "delta"} {2 "delta"} {0 "alpha"} {3 "bravo"} {3 "echo"}
                  {1 "alpha"} {5 "bravo"} {1 "delta"} {2 "charlie"} {5 "charlie"} {4 "charlie"} {1 "bravo"}
                  {4 "alpha"} {3 "alpha"} {2 "alpha"}}))
  (filter (fn [[k v]] (= 3 k)) (mapcat seq (get local-available-resources-view 0)))

  ; find a provider for the correct :resource/id (0) and :resource/time (3)
  ;    (just take first one in the set with the correct :resource/time)
  ;
  (def allocation (as-> local-available-resources-view m
                    (get m 0)
                    (mapcat seq m)
                    (filter (fn [[k v]] (= 3 k)) m)
                    (map (fn [[k v]] {k v}) m)
                    (set m)
                    (first m)))

  ; then we can disj it from the original set and put it back into the read-model
  (update local-available-resources-view 0 disj allocation)

  (def old-avail @available-resources-view)
  (allocate 0 3)

  ; 1) can we satisfy everything?
  ; 2) if yes, do the allocations
  ; 3) if no, then publish! a :service/failure with the same keys as the :service/request

  ; map (allocate id time-t) over all the customer-actual-needs
  (doall
    (map (fn [{:keys [resource/id resource/time-frames]}]
           (map (fn [time-t]
                  [allocate id time-t])
             time-frames))
      customer-actual-needs))

  ())


; endregion

