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
                                      :request/status      :request/submitted
                                      :customer/request-id (:customer/request-id request)
                                      :customer/id         (:customer/id request)
                                      :customer/needs      (:customer/needs request)
                                      :service/resources   resources}])))



(defn- allocate [available resource-id time-t]
  (as-> available m
    (get m resource-id)
    (mapcat seq m)
    (filter (fn [[k v]] (= time-t k)) m)
    (map (fn [[k v]] {k v}) m)
    (set m)
    (first m)))


(defn- allocation->resource [resource-id allocs]
  (->> allocs
    (mapcat seq)
    (group-by second)
    (map (fn [[provider t]]
           {:resource/id          resource-id
            :provider/id          provider
            :resource/time-frames (into [] (map first t))
            :resource/cost        (* (count (into [] (map first t)))
                                    (get-in @provider-catalog-view [provider
                                                                    resource-id
                                                                    :resource/cost]))}))))


(defn process-sales-request
  "this function takes the request and tries to allocate system resources (Googoos)
  from the various providers to satisfy the expressed need(s)"

  [_ _ _ [event-key request]]

  ; TODO: where does process-service-request get the available-resources?
  ;         currently using @available-resources-view

  (println "process-sales-request"
    (:sales/request-id request) "//" (:customer/needs request)
    "//" (:sales/resources request))

  (let [customer-actual-needs (:sales/resources request)
        allocations           (into {}
                                (map (fn [{:keys [resource/id resource/time-frames]}]
                                       {id (into [] (map (fn [time-t]
                                                           (allocate @available-resources-view id time-t))
                                                      time-frames))})
                                  customer-actual-needs))
        allocated-resources   (mapcat (fn [[provider-id allocs]]
                                        (allocation->resource provider-id allocs))
                                allocations)
        all-times             (when (not-empty allocated-resources)
                                (->> allocated-resources
                                  (map :resource/time-frames)
                                  (reduce #(apply conj %1 %2))))
        time-frame            (if all-times
                                [(apply min all-times) (apply max all-times)]
                                [])
        successful-allocation (every? false?
                                (mapcat (fn [[resource-id time-frames]]
                                          (map nil? time-frames))
                                  allocations))
        total-cost            (->> allocated-resources
                                (map :resource/cost)
                                (reduce +))]

    (if successful-allocation
      (do
        ; 1) commit the allocations

        ; 2) publish the :sales/committed event
        (publish! sales-commitment-topic
          [{:sales/request-id (:sales/request-id request)}
           {:commitment/id         (uuid/v1)
            :sales/request-id      (:sales/request-id request)
            :request/status        :request/successful
            :commitment/resources  allocated-resources
            :commitment/time-frame time-frame
            :commitment/cost       total-cost}]))

      (publish! sales-failure-topic
        [{:sales/request-id (:sales/request-id request)}
         {:failure/id       (uuid/v1)
          :sales/request-id (:sales/request-id request)
          :request/status   :request/failed
          :failure/reasons  ["we will one day put some reasons here"
                             "should the reasons also include the failed 'needs'?"]}]))))


; test out process-sales-request with some simple :customer/requests
(comment
  (do
    (def sales-id (uuid/v1))
    (def customer-request-id (uuid/v1))

    (def event [{:sales/request-id sales-id}
                {:sales/request-id    sales-id
                 :request/status      :request/submitted
                 :customer/request-id customer-request-id
                 :customer/needs      [0 1]
                 :sales/resources     [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
                                       {:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]}])
    (def event2 [{:sales/request-id sales-id}
                 {:sales/request-id    sales-id
                  :request/status      :request/submitted
                  :customer/request-id customer-request-id
                  :customer/needs      [20]
                  :sales/resources     [{:resource/id 20 :resource/time-frames [10 11]}]}]))


  ; happy-path
  (spec/explain :sales/commitment
    (second (process-sales-request [] [] [] event)))

  (spec/explain :sales/failure
    (second (process-sales-request [] [] [] event2)))


  (do
    (def event-key (first event))
    (def request (second event))
    (def customer-actual-needs (:sales/resources request))
    (def allocations (into {}
                       (map (fn [{:keys [resource/id resource/time-frames]}]
                              {id (into [] (map (fn [time-t]
                                                  (allocate @available-resources-view id time-t))
                                             time-frames))})
                         customer-actual-needs)))
    (def allocated-resources (mapcat (fn [[provider-id allocs]]
                                       (allocation->resource provider-id allocs))
                               allocations))
    (def all-times (when (not-empty allocated-resources)
                     (->> allocated-resources
                       (map :resource/time-frames)
                       (reduce #(apply conj %1 %2)))))
    (def time-frame (if all-times
                      [(apply min all-times) (apply max all-times)]
                      []))
    (def successful-allocation (every? false?
                                 (mapcat (fn [[resource-id time-frames]]
                                           (map nil? time-frames))
                                   allocations)))
    (def total-cost (->> allocated-resources
                      (map :resource/cost)
                      (reduce +))))


  (let [customer-actual-needs (:sales/resources request)
        allocations           (into {}
                                (map (fn [{:keys [resource/id resource/time-frames]}]
                                       {id (into [] (map (fn [time-t]
                                                           (allocate @available-resources-view id time-t))
                                                      time-frames))})
                                  customer-actual-needs))
        allocated-resources   (mapcat (fn [[provider-id allocs]]
                                        (allocation->resource provider-id allocs))
                                allocations)
        all-times             (when (not-empty allocated-resources)
                                (->> allocated-resources
                                  (map :resource/time-frames)
                                  (reduce #(apply conj %1 %2))))
        time-frame            (if all-times
                                [(apply min all-times) (apply max all-times)]
                                [])
        successful-allocation (every? false?
                                (mapcat (fn [[resource-id time-frames]]
                                          (map nil? time-frames))
                                  allocations))]
    {:success successful-allocation :time time-frame})



  (do
    (def event-key (first event2))
    (def request (second event2))
    (def customer-actual-needs (:sales/resources request))
    (def allocations (into {}
                       (map (fn [{:keys [resource/id resource/time-frames]}]
                              {id (into [] (map (fn [time-t]
                                                  (allocate @available-resources-view id time-t))
                                             time-frames))})
                         customer-actual-needs)))
    (def allocated-resources (mapcat (fn [[provider-id allocs]]
                                       (allocation->resource provider-id allocs))
                               allocations))
    (def all-times (when (not-empty allocated-resources)
                     (->> allocated-resources
                       (map :resource/time-frames)
                       (reduce #(apply conj %1 %2)))))
    (def time-frame (if all-times
                      [(apply min all-times) (apply max all-times)]
                      []))
    (def successful-allocation (every? false?
                                 (mapcat (fn [[resource-id time-frames]]
                                           (map nil? time-frames))
                                   allocations)))
    (def total-cost (->> allocated-resources
                      (map :resource/cost)
                      (reduce +))))


  ())




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


; build a :sales/request from a :customer/request
(comment
  (do
    (def request {:sales/request-id    (uuid/v1)
                  :request/status      :request/submitted
                  :customer/request-id (uuid/v1)
                  :customer/needs      [0 1]
                  :sales/resources     [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
                                        {:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]})

    (def service-id 0))

  (->> request
    :customer/needs
    (mapcat (fn [service-id]
              (:service/elements
                (first
                  (filter #(= (:service/id %) service-id) @service-catalog-view)))))
    (into []))

  ())


; process-sales-request
(comment
  (do
    (def customer-id (uuid/v1))
    (def customer-request-id (uuid/v1))
    (def request-id (uuid/v1))

    (def request [{:sales/request-id    request-id
                   :customer/request-id customer-request-id
                   :customer/id         customer-id}
                  {:sales/request-id    request-id
                   :customer/request-id customer-request-id
                   :customer/id         customer-id
                   :customer/needs      [0 1]
                   :sales/resources     [{:resource/id          0
                                          :resource/time-frames [0 1 2 3 4 5]}
                                         {:resource/id          1
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


  (def customer-actual-needs (:sales/resources request-content))

  (key {5 "alpha"})

  (map (fn [[k v :as m]]
         {:m m :k k :v v})
    (mapcat seq #{{5 "alpha"} {4 "echo"} {0 "delta"} {2 "delta"} {0 "alpha"} {3 "bravo"} {3 "echo"}
                  {1 "alpha"} {5 "bravo"} {1 "delta"} {2 "charlie"} {5 "charlie"} {4 "charlie"} {1 "bravo"}
                  {4 "alpha"} {3 "alpha"} {2 "alpha"}}))
  (filter (fn [[k v]] (= 3 k)) (mapcat seq (get local-available-resources-view 0)))

  ; 1) can we satisfy everything?
  ; 2) if yes, do the allocations
  ; 3) if no, then publish! a :service/failure with the same keys as the :service/request


  ; 1) find a provider for the correct :resource/id (0) and :resource/time (3)
  ;    (just take first one in the set with the correct :resource/time)
  ;
  (def allocation (as-> local-available-resources-view m
                    (get m 0)
                    (mapcat seq m)
                    (filter (fn [[k v]] (= 3 k)) m)
                    (map (fn [[k v]] {k v}) m)
                    (set m)
                    (first m)))

  ; use this to find allocations for ALL the customer needs
  (into {}
    (map (fn [{:keys [resource/id resource/time-frames]}]
           {id (into [] (map (fn [time-t]
                               (allocate local-available-resources-view id time-t))
                          time-frames))})
      customer-actual-needs))

  ; what if we can't satisfy a customer need?
  ;     how do we catch this and return a :service/failure?
  ;
  ; missing time-t = 0 for :resource/id = 0 AND anything for :resource/id = 1
  (def empty-available-resources-view {0 #{{5 "alpha"} {4 "echo"} {2 "delta"} {3 "bravo"} {3 "echo"}
                                           {1 "alpha"} {5 "bravo"} {1 "delta"} {2 "charlie"} {5 "charlie"}
                                           {4 "charlie"} {1 "bravo"} {4 "alpha"} {3 "alpha"} {2 "alpha"}}})

  (allocate empty-available-resources-view 0 0)
  (allocate empty-available-resources-view 1 3)
  ; => nil

  (def allocations (into {}
                     (map (fn [{:keys [resource/id resource/time-frames]}]
                            {id (into [] (map (fn [time-t]
                                                (allocate empty-available-resources-view id time-t))
                                           time-frames))})
                       customer-actual-needs)))

  ; GOOD! we ge a nil for any missing resource/time-t,
  ;   so if nil shows up anywhere we can "FAIL"!

  ; now we need to look for any nils
  (def successful-allocation (every? false?
                               (mapcat (fn [[resource-id time-frames]]
                                         (map nil? time-frames))
                                 allocations)))



  ; then we can "commit" the allocations
  ;     (disj)
  (update local-available-resources-view 0 disj allocation)

  (def old-avail @available-resources-view)
  (allocate @available-resources-view 0 3)


  ())


; we also need to turn the allocations BACK into :commitment/resources,
; so we can attach them to the :service/commitment
(comment

  ; (spec/def :commitment/resource (spec/keys :req [:resource/id
  ;                                                 :provider/id
  ;                                                 :resource/time-frames
  ;                                                 :resource/cost]))

  (do
    (def allocations {0 [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}],
                      1 [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}]})
    (def allocs [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}])

    (def goal [{:resource/id          0 :provider/id "delta"
                :resource/time-frames [0 2] :resource/cost (* 2 5)}
               {:resource/id          0 :provider/id "alpha"
                :resource/time-frames [1 5] :resource/cost (* 2 10)}
               {:resource/id          0 :provider/id "bravo"
                :resource/time-frames [3] :resource/cost (* 2 5)}
               {:resource/id          0 :provider/id "echo"
                :resource/time-frames [4] :resource/cost (* 2 2)}

               {:resource/id          1 :provider/id "delta"
                :resource/time-frames [0 2] :resource/cost (* 2 5)}
               {:resource/id          1 :provider/id "alpha"
                :resource/time-frames [1 5] :resource/cost (* 2 10)}
               {:resource/id          1 :provider/id "bravo"
                :resource/time-frames [3] :resource/cost (* 2 5)}
               {:resource/id          1 :provider/id "echo"
                :resource/time-frames [4] :resource/cost (* 2 2)}]))

  (spec/explain :commitment/resource {:resource/id          0 :provider/id "delta"
                                      :resource/time-frames [0 2] :resource/cost (* 2 5)})
  (spec/explain :commitment/resources goal)

  (def allocated-resources (->> allocs
                             (mapcat seq)
                             (group-by second)
                             (map (fn [[provider t]]
                                    {:resource/id          0
                                     :provider/id          provider
                                     :resource/time-frames (into [] (map first t))
                                     :resource/cost        0}))))
  (spec/explain :commitment/resources allocated-resources)


  ; now to map it over all the allocations
  (defn allocation->resource [resource-id allocs]
    (->> allocs
      (mapcat seq)
      (group-by second)
      (map (fn [[provider t]]
             {:resource/id          resource-id
              :provider/id          provider
              :resource/time-frames (into [] (map first t))
              :resource/cost        (get-in @provider-catalog-view [provider
                                                                    resource-id
                                                                    :resource/cost])}))))

  (def allocated-resources (mapcat (fn [[resource-id allocs]]
                                     (allocation->resource resource-id allocs))
                             allocations))


  ; work out getting the correct cost from the provider-catalog
  (get-in @provider-catalog-view ["alpha" 0 :resource/cost])

  ; lastly, we need the min & max of all the time-frames
  (let [all-times (->> allocated-resources
                    (map :resource/time-frames)
                    (reduce #(apply conj %1 %2)))]
    [(apply min all-times) (apply max all-times)])







  ())


; check some events against the relevant specs
(comment
  (do
    (def allocations {0 [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}],
                      1 [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}]})
    (def failure-event {:service/failure-id      (uuid/v1)
                        :service/request-id      (uuid/v1)
                        :request/status          :request/failed
                        :service/failure-reasons ["we will one day put some reasons here"
                                                  "should the reasons also include the failed 'needs'?"]})
    (def allocated-resources []))

  (spec/explain :commitment/id (uuid/v1))
  (spec/explain :sales/request-id (uuid/v1))
  (spec/explain :request/status :request/successful)
  (spec/explain :commitment/resources allocated-resources)
  (spec/explain :commitment/time-frame [0 5])

  (spec/explain :sales/commitment
    {:commitment/id         (uuid/v1)
     :sales/request-id      (uuid/v1)
     :request/status        :request/successful
     :commitment/resources  allocated-resources
     :commitment/time-frame [0 5]})

  (spec/explain :sales/failure
    {:failure/id       (uuid/v1)
     :sales/request-id (uuid/v1)
     :request/status   :request/failed
     :failure/reasons  ["we will one day put some reasons here"
                        "should the reasons also include the failed 'needs'?"]})


  ())





; endregion

