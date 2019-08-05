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









(def plans {:a [[0 0] [0 1]]
            :b [[1 0] [2 0]]
            :c [[1 1] [2 1] [2 2]]
            :d [[0 2] [0 3] [1 2] [1 3]]})


; the very simplest approach possible: fixed unit grid, specif exactly
; which cells, plan ID is assigned ot the cell, last plan wins the cell
; (no overlaps)
(comment

  ()
  (in-ns 'resource-allocation)


  (defn fixed-unit-grid [channels time-periods]
    (vec (repeat time-periods (vec (repeat channels :_)))))

  (defn populate
        "Assigns each of the cells specified as [channel time-unit]
        coordinates to the given val."
    [grid val request-cells]
    (reduce (fn [g [ch t]]
              (assoc-in g [t ch] val))
            grid request-cells))

  (populate (fixed-unit-grid 3 4) :a [[0 0] [1 2]])


  (defn apply-plans [plans grid]
    "apply a map of plans to the grid, updating recursively
        NOTE: last one wins - i.e., only 1 plan can occupy a slot in
        the grid"
    (if (empty? plans)
      grid
      (let [[p coordinates] (first plans)]
        (recur (rest plans) (populate grid p coordinates)))))




  (def plan-2 {:e [[0 4] [1 4] [2 4]]
               :f [[4 4] [5 4] [4 5] [5 5] [6 4] [6 5]]})

  (apply-plans plans (fixed-unit-grid 3 4))
  (apply-plans (merge plans plan-2) (fixed-unit-grid 10 10))



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

  ; we also need to change (fixed-unit-grid ...) to pass in the "empty value"
  ; so we have a solid foundation
  (defn fixed-unit-grid-2 [channels time-periods empty-val]
    (vec (repeat time-periods (vec (repeat channels empty-val)))))

  (fixed-unit-grid-2 3 4 #{})

  (defn populate-2
        "Assigns each of the cells specified as [channel time-unit]
        coordinates to the given val."
    [grid val request-cells]
    (reduce (fn [g [ch t]]
              (assoc-in g [t ch] (merge (get-in g [t ch]) val)))
            grid request-cells))

  (populate-2 (fixed-unit-grid-2 3 4 #{}) :a [[0 0] [1 2]])


  (defn apply-plans-2 [pop-fn grid plans]
    "apply a map of plans to the grid, updating recursively
        NOTE: last one wins - i.e., only 1 plan can occupy a slot in
        the grid"
    (if (empty? plans)
      grid
      (let [[p coordinates] (first plans)]
        (recur pop-fn (pop-fn grid p coordinates) (rest plans)))))

  ; we'll do that by making each cell be a set #{} of the plans that
  ; want to use it
  (apply-plans-2 populate-2 (fixed-unit-grid-2 3 4 #{}) plans)

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
                    (merge plans overlapping-plan)
                    (fixed-unit-grid-2 3 4 #{}))
     sample-grid-2)


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

  ; make some plan names (keyword)

  (def gen-plan-name gen/keyword)
  (gen/sample gen-plan-name)

  ; now some channel assignments (vector of int)
  (def gen-chan-req (gen/tuple gen/nat gen/nat))
  (gen/sample gen-chan-req)

  (def gen-requests (gen/not-empty (gen/vector gen-chan-req)))
  (gen/sample gen-requests)

  (def gen-plan-reqs (gen/map gen-plan-name gen-requests))
  (gen/sample gen-plan-reqs)


  ; find the max number of time-slots we need to fit all a plan
  ;    we can use this to size a "fixed grid" to fit all the plans
  (defn max-ts [plan]
    "find the max number of time-slots we need to fit all a plan"
    (if (empty? plan)
      0
      (apply max
             (first
               (for [[p reqs] plan]
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






(comment                                                    ; re-thinking this whole approach (thanks Rich)

  (defn check-plans [apply-fn plans grid-fn channels empty-marker pop-fn max-value]
    (let [ts   (max-ts plans)
          grid (grid-fn channels ts empty-marker)]
      (map (fn [p]
             (->> p
                  (apply-fn pop-fn grid)
                  (validate-grid max-value)))
           plans)))


  (let [ts   (max-ts [{} {:q+ [[1 3]]}])
        grid (fixed-unit-grid-2 10 ts #{})]
    (map (fn [p]
           (->> p
                (apply-plans-2 populate-2 grid)
                (validate-grid 1)))
         [{} {:q+ [[1 3]]}]))


  (check-plans apply-plans-2
               [{} {:q+ [[1 1]]}]
               fixed-unit-grid-2 10 #{}
               populate-2
               1)

  ; note: we can only handle 10 generated plans, as they start getting too big
  ; for the 10x10 grid we are using
  (check-plans apply-plans-2
               (gen/sample gen-plan-reqs 10)
               (fixed-unit-grid-2 10 10 #{})
               populate-2
               1)



  ; we should test a grid to see if it passes some predicate

  (defn check-grid [pred grid])





  ())



(comment

  (defn foo
        [n]
    (cond (> n 40) (+ n 20)
          (> n 20) (- (first n) 20)
          :else 0))


  (def n 24)



  ())

;;;;;;;;;;;;;;;;;;;;
; FUTURE EXPERIMENTS

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
