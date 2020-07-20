(ns resource-alloc.resource-allocation
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.rpl.specter :as s]))


;;;;;;;;;;;;;;;;;;;;;
; PROBLEM
;
; develop a simple (really simple) model and algorithm for resource "demand"
; requests made by a set of requestors
;













(def requests {:a #{[0 0] [0 1]}
               :b #{[1 0] [2 0]}
               :c #{[1 1] [2 1] [2 2]}
               :d #{[0 2] [0 3] [1 2] [1 3]}})


; the very simplest approach possible: fixed unit grid, specify exactly
; which cells, request ID is assigned to the cells, last request wins the cell
; (no overlaps)
;
(comment

  ()
  (in-ns 'resource-allocation)

  ;
  ; simplest model I can think of: fixed grid of unit elements
  ; indexed by "x & y", in this can "channels" and "time-slots"
  ;
  (defn fixed-unit-grid [channels time-periods]
    (vec (repeat time-periods (vec (repeat channels :_)))))

  ;
  ; simple algorithm to apply the resources: stick the requestor-id
  ; into the slot
  ;
  ;       "the grid as a reduction over the collection of requests"
  ;
  (defn populate
        "Assigns each of the cells specified as [channel time-unit]
        coordinates to the given val."
    [grid requestor-id request-cells]
    (reduce (fn [g [ch t]]
              (assoc-in g [t ch] requestor-id))
            grid request-cells))

  (populate (fixed-unit-grid 3 4) :a [[0 0] [1 2]])


  ;
  ; something that can apply a whole collection of requests to one grid
  ;
  (defn apply-requests [requests grid]
    "apply a map of plans to the grid, updating recursively
        NOTE: last one wins - i.e., only 1 plan can occupy a slot in
        the grid"
    (if (empty? requests)
      grid
      (let [[p coordinates] (first requests)]
        (recur (rest requests) (populate grid p coordinates)))))


  (def request-2 {:e #{[0 4] [1 4] [2 4]}
                  :f #{[4 4] [5 4] [4 5] [5 5] [6 4] [6 5]}})

  (apply-requests requests (fixed-unit-grid 3 4))
  (apply-requests (merge requests request-2) (fixed-unit-grid 10 10))



  ;
  ;let's check to see if we get the result we expect (for this single case)
  ;    (we'll use test.check for generative testing later)
  ;
  (def sample-grid [[:a :b :b]
                    [:a :c :c]
                    [:d :d :c]
                    [:d :d :_]])

  (= (apply-requests requests (fixed-unit-grid 3 4))
     sample-grid)


  ())


(comment

  ;;;;;;;;;;;;;;;;;;;;;;;;
  ; let's change (populate ...) to to handle requests that overlap
  ;
  ;
  ; we also need to change (fixed-unit-grid ...) to pass in the "empty value"
  ; so we have a solid foundation
  ;
  (defn fixed-unit-grid-2 [channels time-periods empty-val]
    (vec (repeat time-periods (vec (repeat channels empty-val)))))

  ; TESTS
  (fixed-unit-grid-2 3 4 #{})

  ;
  ; fixing populate to add the requestor-id into a set with the id's
  ; of other requestors as the means of allocation
  ;
  (defn populate-2
        "Assigns each of the cells specified as [channel time-unit]
        coordinates to the given val."
    [grid requestor-id request-cells]
    (reduce (fn [g [ch t]]
              (assoc-in g [t ch] (merge (get-in g [t ch]) requestor-id)))
            grid request-cells))

  ; TESTS
  (populate-2 (fixed-unit-grid-2 3 4 #{}) :a #{[0 0] [1 2]})

  ;
  ; let's change (apply-plans ...) to take a function (for populate)
  ;    as the first param
  ;
  ;    NOT: this means (apply-requests-2 ...) can work with BOTH populate fns
  ;
  (defn apply-requests-2 [pop-fn grid requests]
    "apply a map of plans to the grid, updating recursively
        NOTE: last one wins - i.e., only 1 plan can occupy a slot in
        the grid"
    (if (empty? requests)
      grid
      (let [[p coordinates] (first requests)]
        (recur pop-fn (pop-fn grid p coordinates) (rest requests)))))

  ; TESTS
  ;
  ; we also need to make each cell be a set #{} of the plans that
  ; want to use it via (fixed-unit-grid-2 ...)
  ;
  (apply-requests-2 populate-2 (fixed-unit-grid-2 3 4 #{}) requests)

  ; still works with the older versions...
  (apply-requests-2 populate (fixed-unit-grid 3 4) requests)

  ; do we get the right answer?
  (def sample-grid-2 [[#{:a} #{:b} #{:b}]
                      [#{:a} #{:c} #{:c}]
                      [#{:d} #{:d} #{:c}]
                      [#{:d} #{:d} #{}]])
  (= (apply-requests-2 populate-2 (fixed-unit-grid-2 3 4 #{}) requests)
     sample-grid-2)

  ; this plan overlaps both :a and :d
  (def overlapping-plan {:g [[0 1] [0 2] [0 3]]})

  ; this should return FALSE!!!!
  (= (apply-requests-2 populate-2
                       (merge requests overlapping-plan)
                       (fixed-unit-grid-2 3 4 #{}))
     sample-grid-2)


  ;
  ; we can tell if the requests "work" if there are no allocation "sets"
  ; with more than 1 id in it
  ;
  ;
  ; TODO: determine if there are ever situations where the "only 1" invariant doesn't hold
  ;
  (defn validate-grid [pred-fn extract-fn grid]
    "validate that the given grid
    works, i.e., no sets that violate the pred(icate) function"
    (not
      (some #{true}
            (map pred-fn
                 (for [time-slot grid
                       ch        time-slot]
                   (extract-fn ch))))))

  ; TESTS
  (validate-grid #(> % 1) #(count %) [[#{} #{}] [#{:a} #{}]])
  (validate-grid #(> % 1) #(count %) [[#{} #{}] [#{:a :b} #{}]])

  (= true (validate-grid #(> % 1) #(count %) [[#{} #{}] [#{:a} #{}]]))
  (= false (validate-grid #(> % 1) #(count %) [[#{} #{}] [#{:a :b} #{}]]))
  (= true (validate-grid #(> % 1) #(count %) [[#{:a} #{}] [#{:a} #{:b}]]))

  (= true (validate-grid #(> % 2) #(count %) [[#{} #{}] [#{:a :b} #{}]]))



  ())




;;;;;;;;;;;;;;;;;;;;;;;;
; let's see if we develop some generative testing using test.check?
(comment

  ; from https://clojure.org/guides/test_check_beginner
  (def sort-idempotent-prop
    (prop/for-all [v (gen/vector gen/int)]
                  (= (sort v) (sort (sort v)))))

  (tc/quick-check 100 sort-idempotent-prop)
  ;; => {:result true,
  ;; =>  :pass? true,
  ;; =>  :num-tests 100,
  ;; =>  :time-elapsed-ms <x>,
  ;; =>  :seed <x>>}

  ;;;;;;;;;;;;;;;;;;;;;;;;
  ; now we can start looking at our request problem

  ;
  ; make some requestor names (keyword)
  ;
  (def gen-requestor-name gen/keyword)
  (gen/sample gen-requestor-name)

  ;
  ; now some channel assignments (set of [in int])
  ;
  (def gen-chan-req (gen/tuple gen/nat gen/nat))
  (gen/sample gen-chan-req)

  ;
  ; sets of requests
  ;
  (def gen-requests (gen/not-empty (gen/set gen-chan-req)))
  (gen/sample gen-requests)

  ;
  ; and now hook a requestor name onto it
  ;
  (def gen-plan-reqs (gen/map gen-requestor-name gen-requests))
  (gen/sample gen-plan-reqs)


  ;
  ; find the max number of time-slots we need to fit all requests
  ;    we can use this to size a "fixed grid" to fit all the requests
  ;
  ; not sure yet how (or where) this might be useful...
  ;
  ;(defn max-ts [request]
  ;  "find the max number of time-slots we need to fit all a request"
  ;  (if (empty? request)
  ;    0
  ;    (apply max
  ;           (first
  ;             (for [[p reqs] request]
  ;               (if (empty? reqs)
  ;                 '(0)
  ;                 (for [[_ ts] reqs]
  ;                   ts)))))))
  ;
  ;; TESTS
  ;(max-ts {:q+ [[1 1]]})
  ;(max-ts {:q []})
  ;(map max-ts '({:q+ [[1 1]]} {:q+ [[1 1]]}))
  ;(map max-ts '({} {:. [[1 1]]} {:q+ [[1 1]]}))
  ;(map max-ts '({}
  ;              {:z? []}
  ;              {:G [[1 1] [1 2]]}
  ;              {}))
  ;(map max-ts '({}
  ;              {:z? []}
  ;              {:G [[1 1] [1 2]]}
  ;              {}
  ;              {:hm [[1 2] [3 3] [0 2] [2 4]],
  ;               :+D [[1 2]], :?!L [], :j [[1 2]]}))
  ;(max-ts {:N_  [],
  ;         :J   [[5 2] [0 4] [7 7]],
  ;         :+   [[3 2] [1 3] [5 6] [7 2] [6 7] [6 7] [3 1] [0 4]],
  ;         :bI? [[4 6] [2 4] [4 7] [6 3] [4 8] [7 7] [1 2] [2 3]],
  ;         :K   [[2 5] [4 4] [3 4] [3 6] [5 3]],
  ;         :-I  [[3 1] [1 2] [1 2] [1 4] [6 4] [3 3]]})



  ; generating large requests with lots of overlap will be a good test for
  ; when we allow request to not specify the channel and have the populate
  ; function "find" and open channel(set) to put the request into
  ;
  ; "RULES" here we come!




  ; now a checker

  (map
    (fn [plan]
      (->> plan
           (apply-requests-2 populate-2 (fixed-unit-grid-2 3 4 #{}))
           (validate-grid #(> % 1) #(count %))))
    [{:q+ #{[1 1]}} {:q+ #{[1 1]}}])

  (map
    (fn [plan]
      (->> plan
           (apply-requests-2 populate-2 (fixed-unit-grid-2 5 10 #{}))
           (validate-grid #(> % 1) #(count %))))
    '({:sR1 #{[1 1] [3 4] [4 2] [2 4]}
       :gr0 #{[3 3] [4 3] [3 2]}
       :o?  #{[1 4] [1 0] [3 3]}
       :+X4 #{[3 4] [3 1] [2 3]}}))



  (map
    (fn [plan]
      (->> plan
           (apply-requests-2 populate-2 (fixed-unit-grid-2 10 10 #{}))
           (validate-grid #(> % 1) #(count %))))
    (gen/sample gen-plan-reqs 10))






  ; still need to work out how to handle "invalid" plans





  ())






;;;;;;;;;;;;;;;;;;;;;
; next considerations
;
; take a more 'Datomic' approach and return not just the updated grid, but
;      1) the :before-grid
;      2) the :after-grid
;      3) requests that were satisfied (and what slots they were assigned[1])
;      4) requests that were NOT satisfied AND why
;
; [1] in cases where a request is flexible as to the resource[2], we may want
;     to know where it actually got assigned
;
; [2] we need a way to describe this "flexibility" such that a request could
;     have multiple channel(sets) with this flexibility (so a simple '_' won't
;     work as we need to distinguish between the 'groups'


(comment

  ;(defn populate-3
  ;      "Assigns each of the cells specified as [channel time-unit]
  ;            coordinates to the given request-id, but reject any request that
  ;            would overlap an existing allocation."
  ;  [grid request-id request-cells]
  ;  (let [satisfied (atom [])
  ;        rejected  (atom [])
  ;        after     (reduce (fn [g [ch t]]
  ;                            (if (empty? (get-in g [t ch]))
  ;                              (do
  ;                                (prn request-id t ch)
  ;                                (swap! satisfied conj [request-id [t ch]])
  ;                                (assoc-in g [t ch] (merge (get-in g [t ch]) request-id)))
  ;                              (do
  ;                                (swap! rejected conj [request-id [t ch]])
  ;                                g)))
  ;                          grid request-cells)]
  ;    {:before grid :after after :satisfied satisfied :rejected rejected}))
  ;
  ;
  ;(populate-3 (fixed-unit-grid-2 2 2 #{}) :a [[0 0]])
  ;(populate-3 (fixed-unit-grid-2 3 4 #{}) :a [[0 0] [1 1] [2 2]])
  ;
  ;(populate-3 [[#{:a} #{} #{}]
  ;             [#{} #{} #{}]
  ;             [#{} #{} #{}]
  ;             [#{} #{} #{}]]
  ;            :b [[0 0]])
  ;
  ;
  ;(defn populate-4 [{:keys [after before satisfied rejected]}
  ;                  request-id request-cells]
  ;  {:before after :satisfied satisfied :rejected rejected
  ;   :after  (reduce
  ;             (fn [g [ch t]]
  ;               (if (empty? (get-in g [t ch]))
  ;                 (do
  ;                   (prn "sat" request-id t ch)
  ;                   (swap! satisfied conj [request-id [t ch]])
  ;                   (assoc-in g [t ch] (merge (get-in g [t ch]) request-id)))
  ;                 (do
  ;                   (prn "rej" request-id t ch)
  ;                   (swap! rejected conj [request-id [t ch]])
  ;                   g)))
  ;             after request-cells)})
  ;
  ;(defn prep-grid [g]
  ;  {:before g :after [] :satisfied (atom []) :rejected (atom [])})
  ;
  ;
  ;
  ;(def tx1 (populate-4 (prep-grid (fixed-unit-grid-2 3 4 #{}))
  ;                     :a [[0 0]]))
  ;tx1
  ;
  ;(def tx2 (populate-4 tx1 :b [[0 1] [0 2]]))
  ;tx2
  ;
  ;(def tx3 (populate-4 tx2 :c [[0 1] [0 2]]))               ; reject
  ;tx3
  ;
  ;(def tx4 (populate-4 tx3 :d [[1 1] [1 2]]))
  ;tx4
  ;
  ;(def tx5 (populate-4 tx4 :e [[2 1] [1 2]]))
  ;tx5


  ;;;;;;;;;;;;;;;;;;;;;;;;
  ;
  ; "Good news everyone!"
  ;
  ; ...well, not really. I've crewed up (populate-3 ...). How, you say?
  ;
  ; By complecting updating the grid with deciding is a request CAN
  ; update the grid. Worse, i did it in such a was as to embed the logic
  ; for deciding if a request could be satisfied inside the function, so
  ; it can't be changed. (sad face)
  ;
  ; to fix things:
  ;
  ;  0) "delete" populate-3 AND populate-4; revert back to populate-2
  ;
  ;  1) write a new function that determines if a request can be satisfied
  ;
  ;  2) removed rejected requests from a set of requests
  ;
  ;

  (defn check-request
        "this function determines if the slots can be satisfied (pred-fn)

        returns a map of 2 keys:

        :satisfied - map of requested slots that CAN be satisfied
        :rejected  - map of requested slots that CANNOT be satisfied

            NOTE: this still assumes 'first in wins'"

    [request-id request-cells pred-fn grid]

    (let [satisfied (atom {}) rejected (atom {}) g (atom grid)]
      (reset! g
              (reduce
                (fn [g [ch t]]
                  (if (pred-fn (get-in g [t ch]))
                    (do
                      (swap! satisfied assoc request-id (conj (if (nil? (request-id @satisfied))
                                                                #{}
                                                                (request-id @satisfied))
                                                              [t ch]))
                      (assoc-in g [t ch] (merge (get-in g [t ch]) request-id)))
                    (do
                      (swap! rejected assoc request-id (conj (if (nil? (request-id @satisfied))
                                                               #{}
                                                               (request-id @rejected))
                                                             [t ch]))
                      g)))
                grid request-cells))
      {:satisfied @satisfied :rejected @rejected :grid @g}))


  (def sat (atom {:a [[2 2]]}))
  (assoc @sat :a [(conj (:a @sat) [1 1])])
  (swap! sat assoc :a [(conj (:a @sat) [1 1])])
  @sat

  (nil? (:a {}))

  ; TESTS
  (check-request :a #{[0 0]} empty? (fixed-unit-grid-2 3 4 #{}))
  (check-request :f #{[0 0] [1 1]} empty? [[#{:a} #{:b} #{:b}]
                                           [#{:a} #{:c} #{:c}]
                                           [#{:d} #{:d} #{:c}]
                                           [#{:d} #{:d} #{}]]) ; rejects both
  (check-request :f #{[2 3] [1 1]} empty? [[#{:a} #{:b} #{:b}]
                                           [#{:a} #{:c} #{:c}]
                                           [#{:d} #{:d} #{:c}] ; satisfies one,
                                           [#{:d} #{:d} #{}]]) ; rejects the other

  ; changing the predicate means we get different answers (both work now)
  (check-request :f #{[2 3] [1 1]} #(< (count %) 2) [[#{:a} #{:b} #{:b}]
                                                     [#{:a} #{:c} #{:c}]
                                                     [#{:d} #{:d} #{:c}]
                                                     [#{:d} #{:d} #{}]])
  ; [1 1] rejected, [2 3] satisfied:
  (check-request :f #{[2 3] [1 1]} #(< (count %) 2) [[#{:a} #{:b} #{:b}]
                                                     [#{:a} #{:c :q} #{:c}]
                                                     [#{:d} #{:d} #{:c}]
                                                     [#{:d} #{:d} #{:q}]])
  (check-request :f #{[2 3] [1 1] [2 0]} empty? [[#{:a} #{:b} #{}]
                                                 [#{:a} #{:c} #{:c}]
                                                 [#{:d} #{:d} #{:c}]
                                                 [#{:d} #{:d} #{}]])


  ;
  ; I'm no longer sure we need this...
  ;
  (defn remove-rejects
        "remove the offending requests from the collection provided

        return: requests with the offending items removed"

    [rejects requests-w]

    (let [ident (first (keys rejects))
          rejs  (first (vals rejects))]
      (assoc requests-w ident (apply disj
                                     (ident requests-w)
                                     (apply concat rejs)))))

  ; TESTS
  (remove-rejects {:f [[[1 1]]]} {:f #{[2 3] [1 1]}})
  (remove-rejects {:f [[[1 1] [2 3]]]} {:f #{[2 3] [1 1]}})
  (remove-rejects {:f [[[5 1] [2 3]]]} {:f #{[2 3] [1 1]}})


  ; thread data through the chain
  (def x (remove-rejects {:f [[[5 1] [2 3]]]} {:a #{[0 0]}
                                               :b #{[1 1] [3 4]}
                                               :f #{[2 3] [3 1]}}))
  x
  (def request-2 {:e #{[0 4] [1 4] [2 4]}
                  :f #{[4 4] [5 4] [4 5] [5 5] [6 4] [6 5]}})

  (apply-requests-2 populate-2 (fixed-unit-grid-2 10 10 #{}) x)



  ;
  ; lets go the whole path
  (def req-with-overlaps {:a #{[0 0] [0 1] [0 2]}
                          :b #{[0 0] [1 1] [1 2]}})

  (check-all-requests pred-fn my-grid req-with-overlaps)


  (def pred-fn empty?)
  (def my-grid (fixed-unit-grid-2 10 10 #{}))
  (defn ident [x] (first (keys x)))
  (defn reqs [x] (first (vals x)))

  (check-request (ident req-with-overlaps)
                 (reqs req-with-overlaps)
                 pred-fn my-grid)

  ;
  ; from SO (https://stackoverflow.com/questions/9408846/in-clojure-how-to-merge-several-maps-combining-mappings-with-same-key-into-a-li#10256000)
  ;
  ; with a little tweak (fnil conj []) -> (fnil conj {})
  ;
  (defn merge-lists [& maps]
    (reduce (fn [m1 m2]
              (reduce (fn [m [k v]]
                        (update-in m [k] (fnil conj {}) v))
                      m1 m2))
            {}
            maps))


  ; TESTS
  (def a {:sat {:a [[[0 0]]]} :rej {}})
  (def b {:sat {:b [[[0 1]]]} :rej {:b [[[3 4]]]}})
  (def c {:sat {:c [[[1 1]]]} :rej {:c [[[10 10]]]}})

  (merge-lists a b)
  (merge-lists a b c)

  ;
  ; and it WORKS!
  ;
  (apply merge-lists re)


  (defn check-request-2
        [request-id request-cells pred-fn grid]

    (let [satisfied (atom {}) rejected (atom {}) grid-a (atom [])]
      (reset! grid-a (reduce
                       (fn [g [ch t]]
                         (if (pred-fn (get-in g [t ch]))
                           (do
                             (swap! satisfied assoc request-id (conj (if (nil? (request-id @satisfied))
                                                                       #{}
                                                                       (request-id @satisfied))
                                                                     [ch t]))
                             (assoc-in g [t ch] (merge (get-in g [t ch]) request-id)))
                           (do
                             (swap! rejected assoc request-id (conj (if (nil? (request-id @rejected))
                                                                      #{}
                                                                      (request-id @rejected))
                                                                    [ch t]))
                             g)))
                       grid request-cells))
      {:satisfied @satisfied :rejected @rejected :grid @grid-a}))


  ; TESTS
  (check-request-2 :a #{[0 0]} empty? (fixed-unit-grid-2 3 4 #{}))
  (check-request-2 :f #{[0 0] [1 1]} empty? [[#{:a} #{:b} #{:b}]
                                             [#{:a} #{:c} #{:c}]
                                             [#{:d} #{:d} #{:c}]
                                             [#{:d} #{:d} #{}]]) ; rejects both
  (check-request-2 :f #{[2 3] [1 1]} empty? [[#{:a} #{:b} #{:b}]
                                             [#{:a} #{:c} #{:c}]
                                             [#{:d} #{:d} #{:c}] ; satisfies one,
                                             [#{:d} #{:d} #{}]]) ; rejects the other

  ; changing the predicate means we get different answers (both work now)
  (check-request-2 :f #{[2 3] [1 1]} #(< (count %) 2) [[#{:a} #{:b} #{:b}]
                                                       [#{:a} #{:c} #{:c}]
                                                       [#{:d} #{:d} #{:c}]
                                                       [#{:d} #{:d} #{}]])
  ; [1 1] rejected, [2 3] satisfied:
  (check-request-2 :f #{[2 3] [1 1]} #(< (count %) 2) [[#{:a} #{:b} #{:b}]
                                                       [#{:a} #{:c :q} #{:c}]
                                                       [#{:d} #{:d} #{:c}]
                                                       [#{:d} #{:d} #{:q}]])
  (check-request-2 :f #{[2 3] [1 1] [2 0]} empty? [[#{:a} #{:b} #{}]
                                                   [#{:a} #{:c} #{:c}]
                                                   [#{:d} #{:d} #{:c}]
                                                   [#{:d} #{:d} #{}]])


  (defn check-requests-3
        [satisfied rejected grid pred-fn request-id request-cells]
    (reset! grid (reduce
                   (fn [g [ch t]]
                     (if (pred-fn (get-in g [t ch]))
                       (do
                         ;(prn "sat" request-id t ch)
                         (swap! satisfied
                                assoc request-id (conj (if (nil? (request-id @satisfied))
                                                         #{}
                                                         (request-id @satisfied))
                                                       [ch t]))
                         (assoc-in g [t ch] (merge (get-in g [t ch]) request-id)))
                       (do
                         ;(prn "rej" request-id t ch)
                         (swap! rejected
                                assoc request-id (conj (if (nil? (request-id @rejected))
                                                         #{}
                                                         (request-id @rejected))
                                                       [ch t]))
                         g)))
                   @grid request-cells)))


  (check-requests-3 (atom {})
                    (atom {})
                    (atom (fixed-unit-grid-2 3 4 #{}))
                    empty?
                    :a
                    #{[1 1] [2 2] [2 3]})

  (def a (atom {:al ""}))
  ((fn atom-test [a]
     (reset! a {:al "this worked"})) a)
  @a

  (let [satisfied (atom {})
        rejected  (atom {})
        grid      (atom (fixed-unit-grid-2 3 4 #{}))]
    (check-requests-3 satisfied rejected grid
                      empty?
                      :a
                      #{[1 1] [2 3]})
    {:satisfied @satisfied :rejected @rejected :grid @grid})

  (map #(prn (first %) (first (rest %))) req-with-overlaps)

  (def satisfied (atom {}))
  (def rejected  (atom {}))
  (def grid      (atom (fixed-unit-grid-2 3 4 #{})))

  @satisfied
  @rejected
  @grid

  (check-requests-3 satisfied rejected grid
                    empty?
                    :a
                    #{[0 0] [0 2]})
  (check-requests-3 satisfied rejected grid
                    empty?
                    :b
                    #{[0 0] [1 1] [1 2]})

  (do
    (reset! satisfied {})
    (reset! rejected {})
    (reset! grid (fixed-unit-grid-2 3 4 #{})))

  (let []
    (map #(check-requests-3 satisfied rejected grid
                            empty?
                            (first %)
                            (first (rest %)))
         req-with-overlaps)
    {:satisfied @satisfied :rejected @rejected :grid @grid})


  (def req-with-overlaps {:a #{[0 0] [0 1] [0 2]}
                          :b #{[0 0] [1 1] [1 2]}})


  (defn check-all-requests [requests]
    (let [satisfied (atom {})
          rejected (atom {})
          grid (atom (fixed-unit-grid-2 3 4 #{}))]
      (for [r requests]
        (check-requests-3 satisfied rejected grid
                          empty?
                          (first r)
                          (first (rest r))))
      {:satisfied @satisfied :rejected @rejected :grid @grid}))


  (check-all-requests req-with-overlaps)


  (for [a req-with-overlaps]
    (prn a))




  ;
  ; lots of fits and starts, but not complete success
  ;
  ; what we need is a function that modifies 3 different accumulators:
  ;  1) requests satisfied
  ;
  ;  2) requests rejected
  ;
  ;  3) the grid so far, as we apply each request
  ;       which we need to be the input to the next check















  ;
  ; this is NOT working yet - doesn't pass the grid from prior loops
  ; into the next one...
  ;
  (defn check-all-requests
        ""
    [pred-fn grid requests
     (into []
           (for [r requests]
             (let [id   (first r)
                   reqs (first (rest r))]
               (check-request id reqs pred-fn g))))])



  (def re (check-all-requests pred-fn my-grid req-with-overlaps))


  ;
  ; chaining through everything
  ; -> requests
  ;       -> (check-all-requests %)
  ;             -> (merge-lists)
  ;                  ->(apply-requests-2 (:satisfied %))
  ;
  (->> req-with-overlaps
       (check-all-requests pred-fn my-grid)
       (apply merge-lists)
       :satisfied
       (apply-requests-2 populate-2 my-grid))




















  ())



;;;;;;;;;;;;;;;;;;;;
; TODO: FUTURE EXPERIMENTS
;       - less important than getting the mechanism correct
;
; 1) infinite time slots - we plan for the future and it is infinite
;
; 2) non-unit resources - "channels" may be fixed size and position, but not
;                         all resources are such
;
; 3) plans that don't specify a channel(set), but could work "anywhere"
;        a. need encoding for such 'groups' of "any channel"
;
; 4) "bumping" existing allocations for a "later" plan
;        a. need encoding for priorities
;
; 5)
