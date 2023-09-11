(ns mvs.service.process-sales-request
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :as rm]
            [mvs.topics :as t]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; helpers

(defn- allocate
  "pick the next available Googoo 'off the shelf' and 'put it in the box'

  - available : all available resources, formatted as (`{ <:resource/type> #{ { <:resource/time> <:provider/id> }
                                                                            { <:resource/time> <:provider/id> } }
                                                          <:resource/type> #{ { <:resource/time> <:provider/id> }
                                                                            { <:resource/time> <:provider/id> } } }`)
  - resource-type : (:resource/type)
  - time-t : (:resource/time)"

  [available resource-type time-t]
  (as-> available m
    (get m resource-type)
    (mapcat seq m)
    (filter (fn [[k v]] (= time-t k)) m)
    (map (fn [[k v]] {k v}) m)
    (set m)
    (first m)))


(defn- get-provider-resource-cost [provider-id resource-type]
  (as-> (rm/provider-catalogs (rm/state)) v
    (get v provider-id)
    (:resource/catalog v)
    (filter #(= resource-type (:resource/type %)) v)
    (first v)
    (:resource/cost v)))


(defn- allocation->resource
  "turn allocs, which are in a reduced format, back into :commitment/resources

  - resource-type : (:resource/type) which resource are we processing?
  - allocs : collection of allocated resources, formatted as: `{ <:resource/type> [ { <:resource/time> <:provider/id> } ] }
  "
  [resource-type allocs]
  (->> allocs
    (mapcat seq)
    (group-by second)
    (map (fn [[provider-id t]]
           {:resource/type        resource-type
            :provider/id          provider-id
            :resource/time-frames (into [] (map first t))
            :resource/cost        (* (count (into [] (map first t)))
                                    (get-provider-resource-cost provider-id resource-type))}))))


(defn- commit-resources
  "use a reducer to remove (via disj) all the allocated resources from the
  available-resources-view

  - resources : the resources we want to commit to a :sales/request, formatted as:
             `{ <:resource/type> [ { <:resource/time> <:provider/id> } ] }`"

  [resources]

  (reset! rm/available-resources-view
    (reduce (fn [m [id alloc]]
              (assoc m id (apply disj (get m id) alloc)))
      @rm/available-resources-view
      resources)))

; endregion


(defn process-sales-request
  "this function takes the request and tries to allocate system resources (Googoos)
  from the various providers to satisfy the expressed need(s)

  - request : (:sales/request) everything we need to locate in the 'warehouse' for a given customer"

  [[event-key request :as event]]

  (reset! last-event event)

  ; TODO: where does process-service-request get the available-resources?
  ;         currently using @available-resources-view

  (println "process-sales-request"
    (:sales/request-id request) "//" (:order/needs request)
    "//" (:sales/resources request))

  (if (spec/valid? :sales/request request)
    ; region ; handle the valid request
    (let [customer-actual-needs (:sales/resources request)
          ; TODO: map -> for?
          allocations           (into {}
                                  (map (fn [{:keys [resource/type resource/time-frames]}]
                                         {type (into [] (map (fn [time-t]
                                                               (allocate @rm/available-resources-view type time-t))
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
                                  (mapcat (fn [[_ time-frames]]
                                            (map nil? time-frames))
                                    allocations))
          total-cost            (->> allocated-resources
                                  (map :resource/cost)
                                  (reduce +))]

      (if successful-allocation
        (let [commitment-id (uuid/v1)]
          ; 1) commit the allocations, durably
          (commit-resources allocations)

          ; TODO: 2) update the :order/status to :order/reserved
          ;  this should probably be a side-effect of (publish! sales-commitment-topic)
          (rm/order->sales-request-view [{:order/id (:order/id request)}
                                         {:order/event           :order/committed
                                          :commitment/id         commitment-id
                                          :commitment/resources  allocated-resources
                                          :commitment/time-frame time-frame
                                          :commitment/cost       total-cost}])

          ; 3) publish the :sales/committed event
          (t/publish! t/sales-commitment-topic
            [{:sales/request-id (:sales/request-id request)}
             {:commitment/id         commitment-id
              :sales/request-id      (:sales/request-id request)
              :request/status        :request/successful
              :commitment/resources  allocated-resources
              :commitment/time-frame time-frame
              :commitment/cost       total-cost}]))

        ; OR, we failed to satisfy (allocate) all the customer needs
        (do
          (println "process-sales-request (failed)" allocations)

          (rm/order->sales-request-view [{:order/id (:order/id request)}
                                         {:order/event :order/unable-to-reserve}])

          (t/publish! t/sales-failure-topic
            [{:sales/request-id (:sales/request-id request)}
             {:failure/id       (uuid/v1)
              :sales/request-id (:sales/request-id request)
              :request/status   :request/failed
              :failure/reasons  ["we will one day put some reasons here"
                                 "should the reasons also include the failed 'needs'?"]}]))))
    ; endregion

    (malformed "process-sales-request" :sales/request request)))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

(comment
  (def event [{:sales/request-id #uuid"0869e140-3bff-11ee-bad8-8e6f1376370b",
               :order/id         #uuid"7a8a9400-3ac3-11ee-8473-e65ce679c38d",
               :customer/id      #uuid"5a9ff450-3ac3-11ee-8473-e65ce679c38d"}
              {:sales/request-id #uuid"0869e140-3bff-11ee-bad8-8e6f1376370b",
               :request/status   :request/submitted,
               :order/id         #uuid"7a8a9400-3ac3-11ee-8473-e65ce679c38d",
               :customer/id      #uuid"5a9ff450-3ac3-11ee-8473-e65ce679c38d",
               :order/needs      [0 1],
               :sales/resources  [{:resource/type 0, :resource/time-frames [0 1 2 3 4 5]}
                                  {:resource/type 1, :resource/time-frames [0 1 2 3 4 5]}]}])

  ())


; getting the cost for a provider's resource
(comment
  (do
    (def provider-id "alpha-googoos")
    (def resource-type 3))

  (as-> (rm/provider-catalogs (rm/state)) v
    (get v provider-id)
    (:resource/catalog v)
    (filter #(= resource-type (:resource/type %)) v)
    (first v)
    (:resource/cost v))



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
                   :sales/resources     [{:resource/type        0
                                          :resource/time-frames [0 1 2 3 4 5]}
                                         {:resource/type        1
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
                                             {4 "charlie"} {1 "bravo"} {4 "alpha"} {3 "alpha"} {2 "alpha"}}})


    (def customer-actual-needs (:sales/resources request-content)))

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


  ; 1) find a provider for the correct :resource/type (0) and :resource/time (3)
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
    (map (fn [{:keys [resource/type resource/time-frames]}]
           {type (into [] (map (fn [time-t]
                                 (allocate local-available-resources-view type time-t))
                            time-frames))})
      customer-actual-needs))

  ; what if we can't satisfy a customer need?
  ;     how do we catch this and return a :service/failure?
  ;
  ; missing time-t = 0 for :resource/type = 0 AND anything for :resource/type = 1
  (def empty-available-resources-view {0 #{{5 "alpha"} {4 "echo"} {2 "delta"} {3 "bravo"} {3 "echo"}
                                           {1 "alpha"} {5 "bravo"} {1 "delta"} {2 "charlie"} {5 "charlie"}
                                           {4 "charlie"} {1 "bravo"} {4 "alpha"} {3 "alpha"} {2 "alpha"}}})

  (allocate empty-available-resources-view 0 0)
  (allocate empty-available-resources-view 1 3)
  ; => nil


  (map (fn [{:keys [resource/type resource/time-frames]}]
         {:t type :r time-frames})
    customer-actual-needs)

  (map (fn [{:keys [resource/type resource/time-frames]}]
         (map (fn [time-t]
                (allocate @rm/available-resources-view type time-t))
           time-frames))
    customer-actual-needs)

  (def allocations (into {}
                     (map (fn [{:keys [resource/type resource/time-frames]}]
                            {type (into [] (map (fn [time-t]
                                                  (allocate @rm/available-resources-view type time-t))
                                             time-frames))})
                       customer-actual-needs)))

  ; GOOD! we ge a nil for any missing resource/time-t,
  ;   so if nil shows up anywhere we can "FAIL"!

  ; now we need to look for any nils
  (def successful-allocation (every? false?
                               (mapcat (fn [[resource-type time-frames]]
                                         (map nil? time-frames))
                                 allocations)))



  ; then we can "commit" the allocations
  ;     (disj)
  (update @local-available-resources-view 0 disj allocation)

  (def old-avail @rm/available-resources-view)
  (allocate @rm/available-resources-view 0 3)


  ())


; we also need to turn the allocations BACK into :commitment/resources,
; so we can attach them to the :service/commitment
(comment

  (spec/def :commitment/resource (spec/keys :req [:resource/type
                                                  :provider/id
                                                  :resource/time-frames
                                                  :resource/cost]))

  (do
    (def allocations {0 [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}],
                      1 [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}]})
    (def allocs [{0 "delta"} {1 "alpha"} {2 "delta"} {3 "bravo"} {4 "echo"} {5 "alpha"}])

    (def goal [{:resource/type        0 :provider/id "delta"
                :resource/time-frames [0 2] :resource/cost (* 2 5)}
               {:resource/type        0 :provider/id "alpha"
                :resource/time-frames [1 5] :resource/cost (* 2 10)}
               {:resource/type        0 :provider/id "bravo"
                :resource/time-frames [3] :resource/cost (* 2 5)}
               {:resource/type        0 :provider/id "echo"
                :resource/time-frames [4] :resource/cost (* 2 2)}

               {:resource/type        1 :provider/id "delta"
                :resource/time-frames [0 2] :resource/cost (* 2 5)}
               {:resource/type        1 :provider/id "alpha"
                :resource/time-frames [1 5] :resource/cost (* 2 10)}
               {:resource/type        1 :provider/id "bravo"
                :resource/time-frames [3] :resource/cost (* 2 5)}
               {:resource/type        1 :provider/id "echo"
                :resource/time-frames [4] :resource/cost (* 2 2)}]))

  (spec/explain :commitment/resource {:resource/type        0 :provider/id "delta"
                                      :resource/time-frames [0 2] :resource/cost (* 2 5)})
  (spec/explain :commitment/resources goal)

  (def allocated-resources (->> allocs
                             (mapcat seq)
                             (group-by second)
                             (map (fn [[provider t]]
                                    {:resource/type        0
                                     :provider/id          provider
                                     :resource/time-frames (into [] (map first t))
                                     :resource/cost        0}))))
  (spec/explain :commitment/resources allocated-resources)


  ; now to map it over all the allocations
  (defn allocation->resource [resource-type allocs]
    (->> allocs
      (mapcat seq)
      (group-by second)
      (map (fn [[provider-id t]]
             {:resource/type        resource-type
              :provider/id          provider-id
              :resource/time-frames (into [] (map first t))
              :resource/cost        (* (count (into [] (map first t)))
                                      (get-provider-resource-cost provider-id resource-type))}))))

  (def allocated-resources (mapcat (fn [[resource-type allocs]]
                                     (allocation->resource resource-type allocs))
                             allocations))


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


; test out process-sales-request with some simple :customer/requests
(comment
  (do
    (def sales-id (uuid/v1))
    (def order-id (uuid/v1))

    ; one that can be satisfied
    (def event [{:sales/request-id sales-id}
                {:sales/request-id sales-id
                 :request/status   :request/submitted
                 :order/id         order-id
                 :order/needs      [0 1]
                 :sales/resources  [{:resource/type 0 :resource/time-frames [0 1 2 3 4 5]}
                                    {:resource/type 1 :resource/time-frames [0 1 2 3 4 5]}]}])

    ; one that can NOT be satisfied
    (def event2 [{:sales/request-id sales-id}
                 {:sales/request-id sales-id
                  :request/status   :request/submitted
                  :order/id         order-id
                  :order/needs      [20]
                  :sales/resources  [{:resource/type        20
                                      :resource/time-frames [10 11]}]}]))

  [:sales/request-id
   :request/status
   :order/id
   :order/needs
   :sales/resources]

  ; happy-path
  (spec/explain :sales/commitment
    (second (process-sales-request event)))

  (spec/explain :sales/failure
    (second (process-sales-request event2)))


  (do
    (def event-key (first event))
    (def request (second event))
    (def customer-actual-needs (:sales/resources request))
    (def allocations (into {}
                       (map (fn [{:keys [resource/id resource/time-frames]}]
                              {id (into [] (map (fn [time-t]
                                                  (allocate @rm/available-resources-view id time-t))
                                             time-frames))})
                         customer-actual-needs)))
    (def allocated-resources (mapcat (fn [[resource-type allocs]]
                                       (allocation->resource resource-type allocs))
                               allocations))
    (def all-times (when (not-empty allocated-resources)
                     (->> allocated-resources
                       (map :resource/time-frames)
                       (reduce #(apply conj %1 %2)))))
    (def time-frame (if all-times
                      [(apply min all-times) (apply max all-times)]
                      []))
    (def successful-allocation (every? false?
                                 (mapcat (fn [[resource-type time-frames]]
                                           (map nil? time-frames))
                                   allocations)))
    (def total-cost (->> allocated-resources
                      (map :resource/cost)
                      (reduce +))))


  ; sidebar: how does (allocate...) work?
  (do
    (def available @rm/available-resources-view)
    (def resource-type 0)
    (def time-t 3)
    (def sorted (map (fn [[r t]] {r (sort-by first t)}) available)))

  (as-> available m
    (get m resource-type)
    (mapcat seq m)
    (filter (fn [[k v]] (= time-t k)) m)
    (map (fn [[k v]] {k v}) m)
    (set m)
    (first m))


  ; sidebar #2: how does (allocation->resource ...) work?
  (do
    (def allocs (-> allocations first second))
    (def resource-type (-> allocations first first)))

  (->> allocs
    (mapcat seq)
    (group-by second)
    (map (fn [[provider t]]
           {:resource/type        resource-type
            :provider/id          provider
            :resource/time-frames (into [] (map first t))
            :resource/cost        (* (count (into [] (map first t)))
                                    (get-in (rm/provider-catalogs (rm/state))
                                      [provider
                                       resource-type
                                       :resource/cost]))})))




  (let [customer-actual-needs (:sales/resources request)
        allocations           (into {}
                                (map (fn [{:keys [resource/id resource/time-frames]}]
                                       {id (into [] (map (fn [time-t]
                                                           (allocate @rm/available-resources-view id time-t))
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
                                (mapcat (fn [[_ time-frames]]
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
                                                  (allocate @rm/available-resources-view id time-t))
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
                                 (mapcat (fn [[_ time-frames]]
                                           (map nil? time-frames))
                                   allocations)))
    (def total-cost (->> allocated-resources
                      (map :resource/cost)
                      (reduce +))))


  ())


; commit the resources we've selected by removing them from available-resources-view
(comment
  (do
    (def customer-actual-needs [{:resource/type        0
                                 :resource/time-frames [0 1]}])
    ;{:resource/type          1
    ; :resource/time-frames [0 1]}])
    (def t 0)
    (def id 0)
    (def allocations (into {}
                       (map (fn [{:keys [resource/id resource/time-frames]}]
                              {id (into [] (map (fn [time-t]
                                                  (allocate @rm/available-resources-view id time-t))
                                             time-frames))})
                         customer-actual-needs)))
    (def allocations {0 [{0 "delta"} {1 "alpha"}]
                      1 [{0 "bravo"} {1 "echo"}]})
    (def allocations {0 [{1 "alpha"}]
                      1 [{1 "echo"}]})
    (def local-available-resources-view (atom {0 #{{0 "delta"} {1 "alpha"}}
                                               1 #{{0 "bravo"} {1 "echo"}}})))


  (apply disj #{1 2 3} [1 3])

  (apply disj (get @local-available-resources-view id) (get allocations id))

  ; now, how do we apply multiple disj to the data at each step? REDUCE!

  (reset! local-available-resources-view
    (reduce (fn [m [id alloc]]
              (assoc m id (apply disj (get m id) alloc)))
      @local-available-resources-view
      allocations))


  ())


; debuggging "Execution error (NullPointerException) at cljfx.context/sub-ctx (context.clj:107)."
(comment
  (do
    (def event @last-event)
    (def request (second event)))


  (def customer-actual-needs (:sales/resources request))
  ; TODO: map -> for?
  (def allocations (into {}
                     (map (fn [{:keys [resource/type resource/time-frames]}]
                            {type (into [] (map (fn [time-t]
                                                  (allocate @rm/available-resources-view type time-t))
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
  ; TODO: map -> for?
  (def successful-allocation (every? false?
                               (mapcat (fn [[_ time-frames]]
                                         (map nil? time-frames))
                                 allocations)))
  (def total-cost (->> allocated-resources
                    (map :resource/cost)
                    (reduce +)))




  ())


; endregion
