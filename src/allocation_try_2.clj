(ns allocation-try-2)

;;;;;;;;;;;;;;;;;;;;;
;
; PROBLEM
;
; develop a simple (really simple) model and algorithm for handling resource "demand"
; requests made by a set of requestors
;
; return a data structure like Datomic, :before, :after, :satisfied, and :rejected
; for each invocation
;
;

{:namespace "allocation-try-2"
 :public-api ["fixed-unit-grid-2" "test-requests" "retract-requests"]
 :effective-sloc 110}

;;;;;;;;;;;;;;;;;;;;;
;
; SOLUTION
;
; 1) apply the requests to the grid
;
; 2) then scan for grid for requests that overlap
;
;      a) requests that do overlap are marked as "rejected"
;
;      b) requests that don't overlap are marked as "satisfied"
;
; 3) back out any contested slots, so the final grid is acceptable
;
; 4) we also need to retract requests from the grid, because conditions
;    may change and resources are no longer needed
;




;;;;;;;;;;;;;;;;;;;;
;
; ISSUES DURING EXPERIMENTATION
;
;    a) subsequent invocations don't preserve existing allocations found
;       in the 'input' grid - this due to how check-rejected works: it doesn't
;       have enough info to figure this out
;
;       RESOLVED - change remove-rejects to set any bad slots back
;       to the original value. works in one case, but needs lots more testing
;
;    b) subsequent invocations prove not just the most recent set of
;       "satisfactions", but ALL of them, which isn't exactly what I want
;
;       RESOLVED - change check-satisfied to not include slots that haven't
;       changed from the original
;
;    c) do we really want to make (test-requests...) threadable? ie, do we
;       want a 'single' parameter in last position that is actually a tuple
;       of the input grid and the requests, just so we can thread?
;
;       how realistic it that is actual practice?
;
;       RESOLVED - no threading!
;
; TODO: any modification cause by the rules engine approach in loco-rules.clj
;
; TODO: sparse matrix for the resource GRID
;
; TODO: let's get some spec going, so we can generate some tests
;
; TODO: develop a simple UI to visualize the "demand" modeled by the algorithm
;
; TODO: develop a simple UI to develop "requests"
;
; TODO: determine what makes sense to LOG (intermediates? params?)
;
; TODO: determine METRICS for performance
;
;


;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;
;
; DATA STRUCTURES
;
; GRID
;    vector of vectors (2d rectangular matrix) of sets
;
;    eg. (5x5)
;
;      [[#{} #{} #{} #{} #{}]
;       [#{} #{} #{} #{} #{}]
;       [#{} #{} #{} #{} #{}]
;       [#{} #{} #{} #{} #{}]
;       [#{} #{} #{} #{} #{}]]
;
; REQUESTS
;
;    map of requestor-id set of vectors of request-slot (ie. channel and time-slot)
;
;    eg.
;
;      {:a #{[0 0] [1 1]}
;       :b #{[0 1] [1 0]}}
;
; GRID-TEST-TX
;
;   map of 4-tuple
;
;      :before    - GRID before any valid requests were added
;
;      :after     - GRID after all valid requests were added
;
;      :satisfied - map of request-slot to requestor-id
;
;      :rejected  - map of request-slot to set of requestor-ids
;
;   eg.
;
;      {:before [[#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}]],
;       :after  [[#{:b} #{} #{} #{} #{}]
;                [#{:a} #{} #{} #{} #{}]
;                [#{} #{:a} #{} #{} #{}]
;                [#{} #{} #{} #{:c} #{}]
;                [#{} #{} #{} #{:c} #{:c}]],
;       :sat    {[0 0] #{:b}, [0 1] #{:a},
;                [1 2] #{:a}, [3 3] #{:c},
;                [3 4] #{:c}, [4 4] #{:c}},
;       :rej    {[1 1] #{:b :a}}}
;
;
; TODO: request-slot (ie. channel and time-slot) may need to be modified to support rules engine
;
;;;;;;;;;;;;;;;;;;;;
;
; PUBLIC API
;
; fixed-unit-grid-2  - returns an empty GRID, a rectangular data structure of empty
;                      sets to be used as resource 'slots
;
; test-requests    - given a GRID and a REQUESTS, apply the requests
;
;                    returns a map of:
;                       1) the GRID 'before' (:before)
;                       2) the GRID 'after' (:after)
;                       3) the set of requests that were applied (:satisfied)
;                       4) the set of requests that were NOT applied (:rejected)
;
; retract-requests - returns an updated grid that does NOT include and of the
;                    allocations provided in the 'rejects' parameter, which is
;                    formatted as REQUESTS
;
;                    returns - an updated 'clean' grid
;
;                    note: in true LISP fashion, passing in items that don't exist
;                          does NOT cause an error. "Make sure these aren't in there." "Okay"
;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;



(defn fixed-unit-grid-2
      "builds a rectangular data structure of empty sets to be used as
      resource 'slots'

      a 5x5 grid looks like:

      [[#{} #{} #{} #{} #{}]
       [#{} #{} #{} #{} #{}]
       [#{} #{} #{} #{} #{}]
       [#{} #{} #{} #{} #{}]
       [#{} #{} #{} #{} #{}]]"
  [channels time-periods empty-val]
  (vec (repeat time-periods
               (vec (repeat channels empty-val)))))

; TODO: may need to modify (populate-2...) to support rules engine
; TODO: modify (populate-2) to support [] as grid
(defn- populate-2
  "Assigns each of the cells specified as [channel time-unit]
  coordinates to the given val

      returns - an updated grid"
  [grid requestor-id request-cells]
  (reduce (fn [g [ch t]]
            (assoc-in g [t ch] (merge (get-in g [t ch]) requestor-id)))
          grid request-cells))

(defn- apply-requests-2 [pop-fn grid requests]
  "apply a map of requests to the grid, updating recursively
      NOTE: last one wins - i.e., only 1 plan can occupy a slot in
      the grid

      returns - an updated grid"
  (if (empty? requests)
    grid
    (let [[p coordinates] (first requests)]
      (recur pop-fn (pop-fn grid p coordinates) (rest requests)))))

(defn- check-satisfied
  "return a map of {[slot] #{requestor}} that can be applyed to the grid without
  violating the (sat-rule) predicate, ie, 'we should keep these'

      returns - {[slot] #{requestors}}"
  [pred-fn grid original-grid]
  (into {}
        (for [ch (range (count (first grid)))
              ts (range (count grid))]
          (let [val (get-in grid [ts ch])]
            (if (and (pred-fn val)
                     (not (= val (get-in original-grid [ts ch]))))
              {[ch ts] (get-in grid [ts ch])})))))

(defn- check-rejected
  "return a map of {[slot] #{requestor}} that can be applyed to the grid that
  pass the the (rej-rule) predicate, ie, 'we should reject these'

      returns - {[slot] #{requestors}}"
  [pred-fn grid]
  (into {}
        (for [ch (range (count (first grid)))
              ts (range (count grid))]
          (if (not (pred-fn (get-in grid [ts ch])))
            {[ch ts] (get-in grid [ts ch])}))))

(defn- remove-rejects
  "returns an updated grid that does NOT include and of the allocations provided
  in the 'rejects' parameter, ie, the resulting grid is 'clean'

      returns - an updated grid"
  [original-grid grid rejects]
  (reduce (fn [g [[ch t] _]]
            (assoc-in g [t ch] (get-in original-grid [t ch])))
          grid rejects))

(defn- retract-one-requestor
  "removes the requestor-id from each cell specified by retraction-cells -
     side benefit: it works even if you pass it invalid slots (ie, the requestor
     doesn't have an allocation

      returns - an updated grid"
  [grid requestor-id retraction-cells]
  (reduce (fn [g [ch t]]
            (assoc-in g [t ch] (disj (get-in g [t ch]) requestor-id)))
          grid retraction-cells))


;PUBLIC
(defn test-requests [sat-rule rej-rule initial-grid requests]
  "given a grid and a set of requests, apply the requests

      returns a map of:
         1) the grid 'before' (:before)
         2) the grid 'after' (:after)
         3) the set of requests that were applied (:satisfied)
         4) the set of requests that were NOT applied (:rejected)"
  (let [g (apply-requests-2 populate-2 initial-grid requests)]
    (let [sat (check-satisfied sat-rule g initial-grid)
          rej (check-rejected rej-rule g)]
      {:before initial-grid
       :after  (remove-rejects initial-grid g rej)
       :sat    sat
       :rej    rej})))


(defn retract-requests [grid requests]
  "apply a map of retractions to the grid, updating recursively

      returns - an updated grid"
  (if (empty? requests)
    grid
    (let [[p coordinates] (first requests)]
      (recur
        (retract-one-requestor grid p coordinates)
        (rest requests)))))




; TESTS
(comment
  (use 'allocation-try-2 :reload)
  (in-ns 'allocation-try-2)

  (def empty-grid-5-5 (fixed-unit-grid-2 5 5 #{}))
  (def overlapping-requests {:b #{[0 0] [1 1]}
                             :a #{[0 1] [1 1] [1 2]}
                             :c #{[3 3] [3 4] [4 4]}})

  (def sat-rule #(and (<= (count %) 1)
                      (> (count %) 0)))
  (def rej-rule #(<= (count %) 1))


  ; a very simple case
  ;
  (test-requests sat-rule rej-rule empty-grid-5-5 overlapping-requests)
     ; => {:before [[#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}] [#{} #{} #{} #{} #{}]],
     ;     :after  [[#{:b} #{} #{} #{} #{}]
     ;              [#{:a} #{} #{} #{} #{}]
     ;              [#{} #{:a} #{} #{} #{}]
     ;              [#{} #{} #{} #{:c} #{}]
     ;              [#{} #{} #{} #{:c} #{:c}]],
     ;     :sat    {[0 0] #{:b}, [0 1] #{:a},
     ;              [1 2] #{:a}, [3 3] #{:c},
     ;              [3 4] #{:c}, [4 4] #{:c}},
     ;      :rej   {[1 1] #{:b :a}}}


  ; now try chaining a few requests together. do we lose any pre-existing
  ; allocating when we make the next attempt?
  ;
  (def requests-2 {:d #{[0 0] [1 1]}
                   :e #{[2 2] [2 3] [2 4]}
                   :f #{[1 3] [1 4]}})
  (def current-grid (atom empty-grid-5-5))


  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 overlapping-requests)))
      ; => [[#{:b} #{} #{} #{} #{}]
      ;     [#{:a} #{} #{} #{} #{}]
      ;     [#{} #{:a} #{} #{} #{}]
      ;     [#{} #{} #{} #{:c} #{}]
      ;     [#{} #{} #{} #{:c} #{:c}]]
  @current-grid

  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 requests-2)))
      ; => [[#{:b} #{} #{} #{} #{}]
      ;     [#{:a} #{:d} #{} #{} #{}]
      ;     [#{} #{:a} #{:e} #{} #{}]
      ;     [#{} #{:f} #{:e} #{:c} #{}]
      ;     [#{} #{:f} #{:e} #{:c} #{:c}]]

  (def fill-in {:g #{[0 1] [0 2] [0 3] [0 4]}
                :h #{[1 0] [2 0] [3 0] [4 0]}})
  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 fill-in)))
      ; => [[#{:b} #{:h} #{:h} #{:h} #{:h}]
      ;     [#{:a} #{:d} #{} #{} #{}]
      ;     [#{:g} #{:a} #{:e} #{} #{}]
      ;     [#{:g} #{:f} #{:e} #{:c} #{}]
      ;     [#{:g} #{:f} #{:e} #{:c} #{:c}]]

  ; some retractions
  ;
  (def retractions {:g #{[0 2]}})
  (def retractions-2 {:g #{[0 3] [0 4]}})
  (def retract-3 {:a #{[0 1]} :c #{[4 4]}})

  (retract-requests @current-grid retractions)
      ; => [[#{:b} #{:h} #{:h} #{:h} #{:h}]
      ;     [#{:a} #{:d} #{} #{} #{}]
      ;     [#{} #{:a} #{:e} #{} #{}]
      ;     [#{:g} #{:f} #{:e} #{:c} #{}]
      ;     [#{:g} #{:f} #{:e} #{:c} #{:c}]]

  (retract-requests @current-grid retractions-2)
      ; => [[#{:b} #{:h} #{:h} #{:h} #{:h}]
      ;     [#{:a} #{:d} #{} #{} #{}]
      ;     [#{:g} #{:a} #{:e} #{} #{}]
      ;     [#{} #{:f} #{:e} #{:c} #{}]
      ;     [#{} #{:f} #{:e} #{:c} #{:c}]]

  (retract-requests @current-grid retract-3)
      ; => [[#{:b} #{:h} #{:h} #{:h} #{:h}]
      ;     [#{} #{:d} #{} #{} #{}]
      ;     [#{:g} #{:a} #{:e} #{} #{}]
      ;     [#{:g} #{:f} #{:e} #{:c} #{}]
      ;     [#{:g} #{:f} #{:e} #{:c} #{}]]



  ())






;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; need to make GRID be "sparse"
;
; example: 128 channels for 1 month (hourly)
;
; grid -> 128 x (24*30) -> 128 x 720 -> 92,160 cells!
;
; many might be empty
;
; we may need a more compressed format
;            (see https://en.wikipedia.org/wiki/Run-length_encoding)
;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;


(comment
  (defn fixed-unit-grid-3
        [channels time-periods empty-val]
    (into (sorted-map)
          (apply merge-with conj
                 (for [x (range channels)
                       y (range time-periods)]
                   {[x y] empty-val}))))

  (fixed-unit-grid-3 2 2 #{})
  (fixed-unit-grid-3 5 5 #{})


  (test-requests-2 sat-rule
                   rej-rule
                   []
                   overlapping-requests)
  ())