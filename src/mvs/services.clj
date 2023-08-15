(ns mvs.services

  "Note: each 'service' is in its own 'block', alon with any helper function (before) and any
  rich comments for repl dev & testing (after). This will make it easier to pull the relevant
  code into actual microservice components when the time comes."

  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-provider-catalog
;
(defn process-provider-catalog
  "takes a (spec) `:provider/catalog`

  i.e., tuple of hash-maps (key and message-content) with the producer encoded inside the 'key'
  and the catalog itself being the message-content"

  [k _ _ [{:keys [:provider/id]} catalog]]


  (println "process-provider-catalog" k)

  (if (spec/valid? :provider/catalog catalog)
    (do
      ; 1) update provider-catalog-view
      (swap! provider-catalog-view assoc id catalog)

      ; 2) update service-catalog-view (for later use)
      (reset! service-catalog-view service-catalog)

      ; 3) 'publish' ACME's "Service Catalog"
      (publish! sales-catalog-topic service-catalog))

    (malformed "process-provider-catalog" :provider/catalog)))





; check provider-* against :provider/catalog
(comment
  (spec/explain :provider/catalog provider-alpha)


  ())


; test process-provider-catalog
(comment
  (def event-1 [{:provider/id "alpha"} provider-alpha])
  (def event-2 [{:provider/id "bravo"} provider-bravo])

  @provider-catalog-view
  (reset! provider-catalog-view {})

  (process-provider-catalog [] [] [] event-1)
  (process-provider-catalog [] [] [] event-2)
  (process-provider-catalog [] [] [] [{:provider/id "charlie"} provider-charlie])
  (process-provider-catalog [] [] [] [{:provider/id "delta"} provider-delta])
  (process-provider-catalog [] [] [] [{:provider/id "echo"} provider-echo])

  ())

; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-available-resources
;
(defn process-available-resources
  "take provider catalog data (:provider/catalog) and turn it into something useful for allocating
  to customers as: `{ <:resource/id> #{ { <:resource/time> <:provider/id> }
                                        { <:resource/time> <:provider/id> } }
                      <:resource/id> #{ { <:resource/time> <:provider/id> }
                                        { <:resource/time> <:provider/id> } } }`
  "
  [_ _ _ [_ catalog]]

  (println "process-available-resources" (:provider/id catalog))

  (if (spec/valid? :provider/catalog catalog)
    (let [provider-id (:provider/id catalog)
          new-values  (->> catalog
                        :resource/catalog
                        (map (fn [{:keys [resource/id resource/time-frames]}]
                               {id (into #{}
                                     (map (fn [t]
                                            {t provider-id})
                                       time-frames))}))
                        (into {}))]

      (swap! available-resources-view #(merge-with into %1 %2) new-values))

    (malformed "process-available-resources" :provider/catalog)))





; build available-resources-view from provider-catalogs
(comment
  (do
    (def provider-a [{:provider/id "alpha"} provider-alpha])
    (def provider-b [{:provider/id "bravo"} provider-bravo])
    (def m-key {:provider/id "alpha"})

    (def local-available-resources (atom {})))


  (->> (second provider-a)
    :resource/catalog
    (map (fn [{:keys [resource/id resource/time-frames]}]
           {id (into #{}
                 (map (fn [t]
                        {t (:provider/id m-key)})
                   time-frames))}))
    (into {}))

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


; test process-available-resources
(comment
  (do
    (reset! available-resources-view {})
    (def provider-id (:provider/id provider-alpha)))

  (process-available-resources [] [] [] [{:provider/id provider-id}
                                         provider-alpha])

  ())

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-customer-order
;
(defn process-customer-order
  "we:

   1) assign an ACME :sale/request-id to this request
   2) enrich with the actual resources associated with the chosen services

   and pass it along

   we also associate the :order-id with the :sales/request-id generated here, so we
   can find all the relevant data as we need
   "
  [_ _ _ [event-key order]]

  (println "process-customer-order" (:order/id order))

  (reset! last-event order)

  (if (spec/valid? :customer/order order)
    ; region ; handle :customer/order
    (let [request-id (uuid/v1)
          resources  (->> order
                       :order/needs
                       (mapcat (fn [service-id]
                                 (:service/elements
                                   (first
                                     (filter #(= (:service/id %) service-id) @service-catalog-view)))))
                       (into []))]

      (println "process-customer-order (b) " (:order/needs order) " // " resources)

      (if (not-empty resources)
        (do
          ; 1) store the mapping from the :order/id to the :sales/request-id
          (swap! order->sales-request-view assoc
            (:order/id order) (assoc order :sales/request-id request-id))

          ; 2) publish the :sales/request or send the customer some kind of Error

          (publish! sales-request-topic [{:sales/request-id request-id
                                          :order/id         (:order/id order)
                                          :customer/id      (:customer/id order)}
                                         {:sales/request-id request-id
                                          :request/status   :request/submitted
                                          :order/id         (:order/id order)
                                          :customer/id      (:customer/id order)
                                          :order/needs      (:order/needs order)
                                          :sales/resources  resources}]))

        (println "process-customer-order !!!!!! Error Error Error !!!!!! "
          (:order/id order) " // " (:order/needs order))))
    ; endregion

    (malformed "process-customer-order" :customer/order order)))






; check specs for :customer/order
(comment
  (spec/explain :customer/needs [0 1])
  (spec/explain :customer/order {:customer/id (uuid/v1)
                                 :order/id    (uuid/v1)
                                 :order/needs [0 1]})

  ())


; process-customer-order ->  build a :sales/request from a :customer/request
(comment
  (do
    (def order-id (uuid/v1))
    (def service-request-id (uuid/v1))

    (def order @last-event)
    (def order {:sales/request-id service-request-id
                :request/status   :request/submitted
                :customer/id      (uuid/v1)
                :order/id         order-id
                :order/needs      [0 1]
                :sales/resources  [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
                                   {:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]})

    (def service-id 0)
    (def local-view (atom [])))

  (spec/explain :customer/order order)

  (->> order
    :order/needs
    (mapcat (fn [service-id]
              (:service/elements
                (first
                  (filter #(= (:service/id %) service-id) @service-catalog-view)))))
    (into []))

  (swap! local-view conj (assoc order :sales/request-id service-request-id))

  ())


; send an "error" if the customer needs are incorrect (asking for something that
; doesn't exist, etc.)
(comment
  (do
    (def order-id (uuid/v1))
    (def customer-id (uuid/v1))
    (def order {:customer/id (uuid/v1)
                :order/id    order-id
                :order/needs [20]}))

  (->> order
    :order/needs
    (mapcat (fn [service-id]
              (:service/elements
                (first
                  (filter #(= (:service/id %) service-id) @service-catalog-view)))))
    (into []))

  (process-customer-order [] [] [] [{:order/id customer-id :customer/id customer-id}
                                    order])

  ())



; update order->sales-request-view with the new order->service-request mapping
(comment
  (do
    (def local-order->sales-request-view (atom {}))
    (def order-id (uuid/v1))
    (def order {:order/id order-id})
    (def request-id (uuid/v1)))


  (swap! local-order->sales-request-view
    assoc order-id (assoc order :sales/request-id request-id))


  ())

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-sales-request
;

; helpers
(defn- allocate
  "pick the next available Googoo 'off the shelf' and 'put it in the box'

  - available : all available resources, formatted as (`{ <:resource/id> #{ { <:resource/time> <:provider/id> }
                                                                            { <:resource/time> <:provider/id> } }
                                                          <:resource/id> #{ { <:resource/time> <:provider/id> }
                                                                            { <:resource/time> <:provider/id> } } }`)
  - resource-id : (:resource/id)
  - time-t : (:resource/time)"

  [available resource-id time-t]
  (as-> available m
    (get m resource-id)
    (mapcat seq m)
    (filter (fn [[k v]] (= time-t k)) m)
    (map (fn [[k v]] {k v}) m)
    (set m)
    (first m)))


(defn- get-provider-resource-cost [provider-id resource-id]
  (as-> @provider-catalog-view v
    (get v provider-id)
    (:resource/catalog v)
    (filter #(= resource-id (:resource/id %)) v)
    (first v)
    (:resource/cost v)))


(defn- allocation->resource
  "turn allocs, which are in a reduced format, back into :commitment/resources

  - resource-id : (:resource/id) which resource are we processing?
  - allocs : collection of allocated resources, formatted as: `{ <:resource/id> [ { <:resource/time> <:provider/id> } ] }
  "
  [resource-id allocs]
  (->> allocs
    (mapcat seq)
    (group-by second)
    (map (fn [[provider-id t]]
           {:resource/id          resource-id
            :provider/id          provider-id
            :resource/time-frames (into [] (map first t))
            :resource/cost        (* (count (into [] (map first t)))
                                    (get-provider-resource-cost provider-id resource-id))}))))


(defn- commit-resources
  "use a reducer to remove (via disj) all the allocated resources from the
  available-resources-view

  - resources : the resources we want to commit to a :sales/request, formatted as:
             `{ <:resource/id> [ { <:resource/time> <:provider/id> } ] }`"

  [resources]

  (reset! available-resources-view
    (reduce (fn [m [id alloc]]
              (assoc m id (apply disj (get m id) alloc)))
      @available-resources-view
      resources)))


(defn process-sales-request
  "this function takes the request and tries to allocate system resources (Googoos)
  from the various providers to satisfy the expressed need(s)

  - request : (:sales/request) everything we need to locate in the 'warehouse' for a given customer"

  [_ _ _ [event-key request]]

  ;(reset! last-event request)

  ; TODO: where does process-service-request get the available-resources?
  ;         currently using @available-resources-view

  (println "process-sales-request"
    (:sales/request-id request) "//" (:order/needs request)
    "//" (:sales/resources request))

  (if (spec/valid? :sales/request request)
    ; region ; handle the valid request
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
          ; 1) commit the allocations, durably
          (commit-resources allocations)

          ; 2) publish the :sales/committed event
          (publish! sales-commitment-topic
            [{:sales/request-id (:sales/request-id request)}
             {:commitment/id         (uuid/v1)
              :sales/request-id      (:sales/request-id request)
              :request/status        :request/successful
              :commitment/resources  allocated-resources
              :commitment/time-frame time-frame
              :commitment/cost       total-cost}]))

        ; OR, we failed to satisfy (allocate) all the customer needs
        (publish! sales-failure-topic
          [{:sales/request-id (:sales/request-id request)}
           {:failure/id       (uuid/v1)
            :sales/request-id (:sales/request-id request)
            :request/status   :request/failed
            :failure/reasons  ["we will one day put some reasons here"
                               "should the reasons also include the failed 'needs'?"]}])))
    ; endregion

    (println "process-sales-request   ********* MALFORMED ******** expected :sales/request" request)))





; getting the cost for a provider's resource
(comment
  (do
    (def provider-id "alpha")
    (def resource-id 3))

  (as-> @provider-catalog-view v
    (get v provider-id)
    (:resource/catalog v)
    (filter #(= resource-id (:resource/id %)) v)
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
  (update @local-available-resources-view 0 disj allocation)

  (def old-avail @available-resources-view)
  (allocate @available-resources-view 0 3)


  ())


; we also need to turn the allocations BACK into :commitment/resources,
; so we can attach them to the :service/commitment
(comment

  (spec/def :commitment/resource (spec/keys :req [:resource/id
                                                  :provider/id
                                                  :resource/time-frames
                                                  :resource/cost]))

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
      (map (fn [[provider-id t]]
             {:resource/id          resource-id
              :provider/id          provider-id
              :resource/time-frames (into [] (map first t))
              :resource/cost        (* (count (into [] (map first t)))
                                      (get-provider-resource-cost provider-id resource-id))}))))

  (def allocated-resources (mapcat (fn [[resource-id allocs]]
                                     (allocation->resource resource-id allocs))
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
    (def customer-request-id (uuid/v1))

    ; one that can be satisfied
    (def event [{:sales/request-id sales-id}
                {:sales/request-id    sales-id
                 :request/status      :request/submitted
                 :customer/request-id customer-request-id
                 :customer/needs      [0 1]
                 :sales/resources     [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
                                       {:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]}])

    ; one that can NOT be satisfied
    (def event2 [{:sales/request-id sales-id}
                 {:sales/request-id    sales-id
                  :request/status      :request/submitted
                  :customer/request-id customer-request-id
                  :customer/needs      [20]
                  :sales/resources     [{:resource/id          20
                                         :resource/time-frames [10 11]}]}]))


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
    (def allocated-resources (mapcat (fn [[resource-id allocs]]
                                       (allocation->resource resource-id allocs))
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


  ; sidebar: how does (allocate...) work?
  (do
    (def available @available-resources-view)
    (def resource-id 0)
    (def time-t 3)
    (def sorted (map (fn [[r t]] {r (sort-by first t)}) available)))

  (as-> available m
    (get m resource-id)
    (mapcat seq m)
    (filter (fn [[k v]] (= time-t k)) m)
    (map (fn [[k v]] {k v}) m)
    (set m)
    (first m))


  ; sidebar #2: how does (allocation->resource ...) work?
  (do
    (def allocs (-> allocations first second))
    (def resource-id (-> allocations first first)))

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
                                                                    :resource/cost]))})))




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


; commit the resources we've selected by removing them from available-resources-view
(comment
  (do
    (def customer-actual-needs [{:resource/id          0
                                 :resource/time-frames [0 1]}])
    ;{:resource/id          1
    ; :resource/time-frames [0 1]}])
    (def t 0)
    (def id 0)
    (def allocations (into {}
                       (map (fn [{:keys [resource/id resource/time-frames]}]
                              {id (into [] (map (fn [time-t]
                                                  (allocate @available-resources-view id time-t))
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

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-sales-commitment
;

(defn- associated-sales-request [order-id]
  (->> @order->sales-request-view
    vals
    (filter #(= order-id (:order/id %)))
    first))


(defn- associated-order [sales-request-id]
  (->> @order->sales-request-view
    vals
    (filter #(= sales-request-id (:sales/request-id %)))
    first))


(defn- add-agreement [{agreement-id :agreement/id
                       order-id     :order/id
                       resources    :agreement/resources
                       :as          agreement}]

  (swap! order->sales-request-view
    assoc order-id (-> @order->sales-request-view
                     (get order-id)
                     (assoc :agreement/id agreement-id)
                     (assoc :agreement/resources resources))))


(defn process-sales-commitment
  "this function takes a :sales/commitment from 'planning' and enriches into
   a :sales/agreement event which we send ot the customer for their approval (or rejection)

   _OR_

   we get a :sales/failure because planning can't fulfil the customer's order, so we should
   tell the customer that we can't do anything, by sending a :sales/failure event"

  [_ _ _ [event-key event]]

  ;(reset! last-event event)

  (if (or (spec/valid? :sales/commitment event)
        (spec/valid? :sales/failure event))

    ; region ; handle a success or failure from planning
    (condp = (:request/status event)
      :request/successful
      (do
        (println "process-sales-commitment SUCCESS" event-key "//" event)

        (let [commitment           event
              agreement-id         (uuid/v1)
              sales-request-id     (:sales/request-id commitment)
              associated-order     (associated-order sales-request-id)
              customer-id          (:customer/id associated-order)
              order-id             (:order/id associated-order)
              order-needs          (:order/needs associated-order)
              agreement-resources  (:commitment/resources commitment)
              agreement-time-frame (:commitment/time-frame commitment)
              commitment-cost      (:commitment/cost commitment)
              agreement-price      (->> order-needs
                                     (mapcat (fn [id]
                                               (filter (fn [r] (= id (:service/id r)))
                                                 @service-catalog-view)))
                                     (map :service/price)
                                     (reduce +))
              agreement-notes      ["note 1" "note 2"]
              sales-agreement      {:agreement/id         agreement-id
                                    :customer/id          customer-id
                                    :order/id             order-id
                                    :order/needs          order-needs
                                    :agreement/resources  agreement-resources
                                    :agreement/time-frame agreement-time-frame
                                    :agreement/price      agreement-price
                                    :agreement/notes      agreement-notes}]

          (println "profit" (- agreement-price commitment-cost)
            "//" agreement-price "//" commitment-cost)

          (println "process-sales-commitment (b) " associated-order)

          ; we should also (assoc) the :agreement/id & :commitment/resources into order->sales-request-view
          (add-agreement sales-agreement)

          (publish! sales-agreement-topic [{:agreement/id agreement-id}
                                           sales-agreement])))

      :request/failed
      (println "process-sales-commitment ******* FAILURE ******" event-key "//" event))
    ; endregion

    (malformed "process-sales-commitment" :sales/commitment event)))





; check :sales/commitment against the spec
(comment
  (spec/explain :sales/commitment {:commitment/id         #uuid"1cce2aa0-3895-11ee-be86-1768c9d0d0e5",
                                   :sales/request-id      #uuid"8f2c8eb0-3882-11ee-be86-1768c9d0d0e5",
                                   :request/status        :request/successful,
                                   :commitment/resources  [{:resource/id 0, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                                           {:resource/id 0, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                                           {:resource/id 0, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                                           {:resource/id 0, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}
                                                           {:resource/id 1, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                                           {:resource/id 1, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                                           {:resource/id 1, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                                           {:resource/id 1, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}],
                                   :commitment/time-frame [0 5],
                                   :commitment/cost       74})

  ()

  ())


; get the :order/id from the :sales/request-id from order->sales-request-view
(comment
  (do
    (def sales-request-id #uuid"96830160-3b04-11ee-9c78-16a8ee85083d"))

  (->> @order->sales-request-view
    vals
    (filter #(= sales-request-id (:sales/request-id %)))
    first)

  ())


; get the :sales/request-id from the :order-id
(comment
  (do
    (def order-id #uuid"75f888c0-3ac3-11ee-8473-e65ce679c38d"))

  @order->sales-request-view

  (associated-sales-request order-id)


  ())


; process-sales-commitment
(comment
  (do
    (def local-view (atom []))
    (def order {:customer/id (uuid/v1)
                :order/id    (uuid/v1)
                :order/needs [0 1]})
    (def event {:commitment/id         #uuid"1cce2aa0-3895-11ee-be86-1768c9d0d0e5",
                :sales/request-id      #uuid"8f2c8eb0-3882-11ee-be86-1768c9d0d0e5",
                :request/status        :request/successful,
                :commitment/resources  [{:resource/id 0, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                        {:resource/id 0, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                        {:resource/id 0, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                        {:resource/id 0, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}
                                        {:resource/id 1, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                        {:resource/id 1, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                        {:resource/id 1, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                        {:resource/id 1, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}],
                :commitment/time-frame [0 5],
                :commitment/cost       74})
    (def commitment event)
    (def agreement-id (uuid/v1))

    (swap! local-view conj (assoc order :sales/request-id
                             (:sales/request-id commitment)))

    (def sales-request-id (:sales/request-id commitment))
    (def associated-order (->> @local-view
                            (filter #(= sales-request-id (:sales/request-id %)))
                            first))
    (def customer-id (:customer/id associated-order))
    (def order-id (:order/id associated-order))
    (def order-needs (:order/needs associated-order))
    (def agreement-resources (:commitment/resources commitment))
    (def agreement-time-frame (:commitment/time-frame commitment))
    (def commitment-cost (:commitment/cost commitment))
    (def agreement-price (->> order-needs
                           (mapcat (fn [id]
                                     (filter (fn [r] (= id (:service/id r)))
                                       @service-catalog-view)))
                           (map :service/price)
                           (reduce +)))
    (def agreement-notes ["note 1" "note 2"]))


  ())


; add-in the :agreement/id to the cache
(comment
  (do
    (def agreement-id (uuid/v1))
    (def order-id #uuid"6d9bc4e3-3a4a-11ee-8473-e65ce679c38d")
    (def customer-id #uuid"6d9bc4e0-3a4a-11ee-8473-e65ce679c38d")
    (def service-request-id #uuid"7139d330-3a4a-11ee-8473-e65ce679c38d")
    (def agreement-resources [{:resource/id       "100" :resource/time-frames [0 1]
                               :resource/provider "alpha" :resource/cost 10}])

    (def local-order->sales-request-view
      (atom {#uuid"6d9bc4e3-3a4a-11ee-8473-e65ce679c38d"
             {:order/id         #uuid"6d9bc4e3-3a4a-11ee-8473-e65ce679c38d",
              :customer/id      #uuid"6d9bc4e0-3a4a-11ee-8473-e65ce679c38d",
              :order/needs      [0 1],
              :sales/request-id #uuid"7139d330-3a4a-11ee-8473-e65ce679c38d"}})))

  @order->sales-request-view


  (-> @local-order->sales-request-view
    (get order-id)
    (assoc :agreement/id agreement-id)
    (assoc :commitment/resources agreement-resources))


  (-> @local-order->sales-request-view
    (get order-id)
    (assoc :agreement/id agreement-id)
    (assoc :commitment/resources agreement-resources))



  (swap! local-order->sales-request-view
    assoc order-id
    (-> @local-order->sales-request-view
      (get order-id)
      (assoc :agreement/id agreement-id)
      (assoc :commitment/resources agreement-resources)))



  ())

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-order-approval
;
(defn process-order-approval
  "this function takes an :order/agreement from the customer and turns it into a 'plan'
  to be submitted for actual 'fulfillment' (as opposed to what planning does, which is sort of
  hypothetical)
  "
  [_ _ _ [{:keys [order/id] :as event-key} {status      :order/status
                                            customer-id :customer/id
                                            order-id    :order/id
                                            :as         approval}]]

  (println "process-order-approval" event-key)

  (if (spec/valid? :order/approval approval)
    (do
      (println "the customer " status " order " order-id)

      (let [sales-request-id (-> order-id associated-sales-request :sales/request-id)
            assoc-agreement  (associated-order sales-request-id)
            resources        (:agreement/resources assoc-agreement)
            plan-id          (uuid/v1)
            plan             {:plan/id              plan-id
                              :customer/id          customer-id
                              :sales/request-id     sales-request-id
                              :commitment/resources resources}]

        (publish! plan-topic [{:plan/id plan-id} plan])))

    (malformed "process-order-approval" :order/approval approval)))





; get the :sales/request associated with this :agreement/id
(comment
  (do
    (def sales-request-id #uuid"def57e90-3b05-11ee-9c78-16a8ee85083d"))

  @order->sales-request-view

  (associated-order sales-request-id)


  ())


; get the :sales/resources for the :sales/request
(comment
  (do
    (def sales-request-id #uuid"def57e90-3b05-11ee-9c78-16a8ee85083d")
    (def assoc-agreement (associated-order sales-request-id)))

  (:agreement/resources assoc-agreement)

  ())


; format the :sales/plan from the relevant data
(comment
  (do
    (def sales-request-id #uuid"def57e90-3b05-11ee-9c78-16a8ee85083d")
    (def assoc-agreement (associated-order sales-request-id))
    (def resources (:agreement/resources assoc-agreement)))

  (map #(dissoc % :resource/cost) resources)

  ())

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-plan
;
(defn process-plan [_ _ _ [{:keys [order/id] :as event-key}
                           {plan-id          :plan/id
                            customer-id      :customer/id
                            sales-request-id :sales/request-id
                            resources        :commitment/resources
                            :as              plan}]]

  (println "process-plan" event-key)

  (if (spec/valid? :sales/plan plan)
    (let [resources     (->> plan
                          :commitment/resources
                          (group-by :provider/id))
          expanded-plan (map (fn [[id r]]
                               {id (map (fn [m]
                                          (dissoc m :provider/id :resource/cost))
                                     r)})
                          resources)]

      ; 1) place orders with the providers
      (doseq [p expanded-plan]
        (doseq [[id r] p]
          (let [order-id       (uuid/v1)
                provider-order {:order/id         order-id
                                :provider/id      id
                                :order/status     :order/purchased
                                :service/elements r}]
            (publish! provider-order-topic [{:order/id order-id}
                                            provider-order])))))

    ; 2) tell "monitoring" to watch for the new orders


    (malformed "process-plan" :sales/plan plan)))




; format a :provider/shipping event to each :provider/id (in the :sales/plan, specifically
; the :commitment/resources key)
(comment
  (do
    (def plan {:plan/id          (uuid/v1)
               :customer/id      (uuid/v1)
               :sales/request-id (uuid/v1)
               :commitment/resources
               [{:resource/id          0 :provider/id "alpha"
                 :resource/time-frames [0 1] :resource/cost 10}
                {:resource/id          0 :provider/id "delta"
                 :resource/time-frames [0 1] :resource/cost 10}
                {:resource/id          1 :provider/id "alpha"
                 :resource/time-frames [0 1] :resource/cost 10}
                {:resource/id          1 :provider/id "bravo"
                 :resource/time-frames [0 1] :resource/cost 10}]}))

  ; so we expect 3 events, one each for: "alpha", "bravo", and "delta"

  (def resources (->> plan
                   :commitment/resources
                   (group-by :provider/id)))

  ; then, drop the :provider/id and :resource/cost keys from each resource
  ; (this is a map inside a map)
  (def expanded-plan (map (fn [[id r]]
                            {id (map (fn [m]
                                       (dissoc m :provider/id :resource/cost))
                                  r)})
                       resources))

  ; and then reformat into :provider/order:
  ;     :order/id
  ;     :provider/id
  ;     :service/elements
  ;     :order/status
  (def provider-order {:order/id     (uuid/v1)
                       :provider/id  "alpha"
                       :order/status :order/purchased
                       :service/elements
                       [{:resource/id 0 :resource/time-frames [0 1]}
                        {:resource/id 1 :resource/time-frames [0 1]}]})

  (spec/explain :provider/order provider-order)

  ; create an event for each provider in expanded-plan
  (doall
    (mapcat (fn [m]
              (map (fn [[id r]]
                     (let [order-id       (uuid/v1)
                           provider-order {:order/id         order-id
                                           :provider/id      id
                                           :order/status     :order/purchased
                                           :service/elements r}]
                       [{:order/id order-id}
                        provider-order]))
                m))
      expanded-plan))

  (doseq [p expanded-plan]
    (doseq [[id r] p]
      (let [order-id       (uuid/v1)
            provider-order {:order/id         order-id
                            :provider/id      id
                            :order/status     :order/purchased
                            :service/elements r}]
        [{:order/id order-id}
         provider-order])))





  ())


; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; process-resource-measurement
;
(defn process-resource-measurement [_ _ _ [{:keys [resoruce/id] :as event-key}
                                           {:keys [resource/id
                                                   resource/time-frame
                                                   measurement/value]
                                            :as measurement}]]

  (println "process-resource-measurement" event-key)

  (if (spec/valid? :resource/measurement measurement)
    (do)


    (malformed "process-resource-measurement" :resource/measurement measurement)))


; endregion
