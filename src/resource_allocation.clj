(ns resource-allocation
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))


;;;;;;;;;;;;;;;;;;;;;
; PROBLEM
;
; develop a simple (really simple) model and algorithm for resource "demand"
; requests made by a set of requestors
;






;;;;;;;;;;;;;;;;;;;;;
; Additional activities
;
; develop a simple UI to develop "requests"
;
; develop a simple UI to visualize the "demand" modeled by the algorithm
;
; determine what makes sense to LOG (intermediates? params?)
;
;







(def requests {:a [[0 0] [0 1]]
               :b [[1 0] [2 0]]
               :c [[1 1] [2 1] [2 2]]
               :d [[0 2] [0 3] [1 2] [1 3]]})


; the very simplest approach possible: fixed unit grid, specif exactly
; which cells, plan ID is assigned ot the cell, last plan wins the cell
; (no overlaps)
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
  ; simple algorithm to apply the resources: stick the requestor-id into the slot
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
  ; something that can apply a whole collection of requests
  (defn apply-plans [requests grid]
    "apply a map of plans to the grid, updating recursively
        NOTE: last one wins - i.e., only 1 plan can occupy a slot in
        the grid"
    (if (empty? requests)
      grid
      (let [[p coordinates] (first requests)]
        (recur (rest requests) (populate grid p coordinates)))))


  (def request-2 {:e [[0 4] [1 4] [2 4]]
                  :f [[4 4] [5 4] [4 5] [5 5] [6 4] [6 5]]})

  (apply-plans requests (fixed-unit-grid 3 4))
  (apply-plans (merge requests request-2) (fixed-unit-grid 10 10))



  ;
  ;let's check to see if we get the result we expect (for this single case)
  ;    (we'll use test.check for generative testing later)
  ;
  (def sample-grid [[:a :b :b]
                    [:a :c :c]
                    [:d :d :c]
                    [:d :d :_]])

  (= (apply-plans plans (fixed-unit-grid 3 4))
     sample-grid)


  ())


(comment

  ;;;;;;;;;;;;;;;;;;;;;;;;
  ; let's change (populate ...) to show if plans overlap
  ;    we'll change (apply-plans ...) to take a function (for populate)
  ;    as the first param

  ;
  ; we also need to change (fixed-unit-grid ...) to pass in the "empty value"
  ; so we have a solid foundation
  ;
  (defn fixed-unit-grid-2 [channels time-periods empty-val]
    (vec (repeat time-periods (vec (repeat channels empty-val)))))

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

  (populate-2 (fixed-unit-grid-2 3 4 #{}) :a [[0 0] [1 2]])

  ;
  ; all we need to do now is pass all the new params into the new populate-2
  ; function
  ;
  (defn apply-plans-2 [pop-fn grid requests]
    "apply a map of plans to the grid, updating recursively
        NOTE: last one wins - i.e., only 1 plan can occupy a slot in
        the grid"
    (if (empty? requests)
      grid
      (let [[p coordinates] (first requests)]
        (recur pop-fn (pop-fn grid p coordinates) (rest requests)))))

  ;
  ; we'll do that by making each cell be a set #{} of the plans that
  ; want to use it
  ;
  (apply-plans-2 populate-2 (fixed-unit-grid-2 3 4 #{}) requests)

  (def sample-grid-2 [[#{:a} #{:b} #{:b}]
                      [#{:a} #{:c} #{:c}]
                      [#{:d} #{:d} #{:c}]
                      [#{:d} #{:d} #{}]])
  (= (apply-plans-2 populate-2 (fixed-unit-grid-2 3 4 #{}) plans)
     sample-grid-2)

  ; this plan overlaps both :a and :d
  (def overlapping-plan {:g [[0 1] [0 2] [0 3]]})

  ; this should return FALSE!!!!
  (= (apply-plans-2 populate-2
                    (merge requests overlapping-plan)
                    (fixed-unit-grid-2 3 4 #{}))
     sample-grid-2)


  ;
  ; we can tell if the requests "work" if there are no allocation "sets"
  ; with more than 1 id in it
  ;
  ; (we'll figure out if there are ever situations where this invariant
  ;  might not hold)
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
  ; now some channel assignments (vector of int)
  ;
  (def gen-chan-req (gen/tuple gen/nat gen/nat))
  (gen/sample gen-chan-req)

  ;
  ; sets of requests
  ;
  (def gen-requests (gen/not-empty (gen/vector gen-chan-req)))
  (gen/sample gen-requests)

  ;
  ; and now hook a requestor name onto it
  ;
  (def gen-plan-reqs (gen/map gen-requestor-name gen-requests))
  (gen/sample gen-plan-reqs)


  ;
  ; find the max number of time-slots we need to fit all a plan
  ;    we can use this to size a "fixed grid" to fit all the plans
  ;
  ; not sure yet how (or where) this might be useful...
  ;
  (defn max-ts [request]
    "find the max number of time-slots we need to fit all a plan"
    (if (empty? request)
      0
      (apply max
             (first
               (for [[p reqs] request]
                 (if (empty? reqs)
                   '(0)
                   (for [[_ ts] reqs]
                     ts)))))))

  (max-ts {:q+ [[1 1]]})
  (max-ts {:q []})
  (map max-ts '({:q+ [[1 1]]} {:q+ [[1 1]]}))
  (map max-ts '({} {:. [[1 1]]} {:q+ [[1 1]]}))
  (map max-ts '({}
                {:z? []}
                {:G [[1 1] [1 2]]}
                {}))
  (map max-ts '({}
                {:z? []}
                {:G [[1 1] [1 2]]}
                {}
                {:hm [[1 2] [3 3] [0 2] [2 4]],
                 :+D [[1 2]], :?!L [], :j [[1 2]]}))
  (max-ts {:N_  [],
           :J   [[5 2] [0 4] [7 7]],
           :+   [[3 2] [1 3] [5 6] [7 2] [6 7] [6 7] [3 1] [0 4]],
           :bI? [[4 6] [2 4] [4 7] [6 3] [4 8] [7 7] [1 2] [2 3]],
           :K   [[2 5] [4 4] [3 4] [3 6] [5 3]],
           :-I  [[3 1] [1 2] [1 2] [1 4] [6 4] [3 3]]})



  ; generating large plans with lots of overlap will be a good test for when
  ; we allow request to not specify the channel and have the populate function
  ; "find" and open channel(set) to put the request into
  ;
  ; "RULES" here we come!




  ; now a checker

  (map
    (fn [plan]
      (->> plan
           (apply-plans-2 populate-2 (fixed-unit-grid-2 3 4 #{}))
           (validate-grid #(> % 1) #(count %))))
    [{:q+ [[1 1]]} {:q+ [[1 1]]}])

  (map
    (fn [plan]
      (->> plan
           (apply-plans-2 populate-2 (fixed-unit-grid-2 5 10 #{}))
           (validate-grid #(> % 1) #(count %))))
    '({:sR1 [[1 1] [3 4] [4 2] [3 4]]
       :gr0 [[3 3] [4 3] [3 2]]
       :o?  [[1 4] [1 0] [3 3]]
       :+X4 [[3 4] [3 1] [2 3]]}))





  (map
    (fn [plan]
      (->> plan
           (apply-plans-2 populate-2 (fixed-unit-grid-2 10 10 #{}))
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
  ; from Stu's "Clojure in 10 Big Ideas" (Chicago 2017)

  (defn foo
        [n]
    (cond (> n 40) (+ n 20)
          (> n 20) (- (first n) 20)
          :else 0))


  (def n 24)



  ())



;;;;;;;;;;;;;;;;;;;;
; FUTURE EXPERIMENTS
;       - less important than getting the mechanism correct
;
; 1) infinite time slots - we plan for the future and it is infinite
;
; 2) non-unit resources - "channels" may be fixed size and position, but not
;                         all resources are such
;
; 3) plans that don't specify a channel(set), but could work "anywhere"
;
; 4) "bumping" existing allocations for a "later" plan
;        a. priorities
;
; 5)
