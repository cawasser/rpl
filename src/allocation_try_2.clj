(ns allocation-try-2)

;;;;;;;;;;;;;;;;;;;;;
; PROBLEM
;
; develop a simple (really simple) model and algorithm for resource "demand"
; requests made by a set of requestors
;
; return a data structure like Datomic, :before, :after, :satisfied, and :rejected
; for each invocation
;
;


;;;;;;;;;;;;;;;;;;;;;
; A working solution
;
; 1) apply the requests to the grid
;
; 2) then scan for requests that overlap
;
;      a) requests that do are marked as "rejected"
;
;      b) requests that don't are marked as "satisfied"
;
; 3) back out any contested slots, so the final grid is acceptable



;;;;;;;;;;;;;;;;;;;;
; Remaining issues
;
;    a) subsequent invocations don't preserve existing allocations found
;       in the 'input' grid - this due to how check-rejected works: it doesn't
;       have enough info to figure this out
;
;       RESOLVED? - change remove-rejects to set any bad slots back
;       to the original value. works in one case, but needs lots more testing
;
;
;    b) subsequent invocations prove not just the most recent set of
;       "satisfactions", but ALL of them, which isn't exactly what I want
;
;       RESOLVED? - change check-satisfied to not include slots that haven't
;       changed from the original
;
;    c) do we really want to make (test-requests...) threadable? ie, do we
;       want a 'single' parameter in last position that is actually a tuple
;       of the input grid and the requests, just so we can thread?
;
;       how realistic it that is actual practice?
;
;      RESOLVED - no threading!
;
;    d) let's get some spec going, so we can generate some tests
;


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


(defn- populate-2
  "Assigns each of the cells specified as [channel time-unit]
  coordinates to the given val


      returns - an updated grid"
  [grid requestor-id request-cells]
  (reduce (fn [g [ch t]]
            (assoc-in g [t ch] (merge (get-in g [t ch]) requestor-id)))
          grid request-cells))


(defn- apply-requests-2 [pop-fn grid requests]
  "apply a map of plans to the grid, updating recursively
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
  @current-grid

  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 requests-2)))

  (def fill-in {:g #{[0 1] [0 2] [0 3] [0 4]}
                :h #{[1 0] [2 0] [3 0] [4 0]}})
  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 fill-in)))


  ())





;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;


(comment
  (in-ns 'allocation-try-2)


  (populate-2 (fixed-unit-grid-2 3 4 #{}) :a #{[0 0] [1 2]})
  (def empty-grid-5-5 (ra/fixed-unit-grid-2 5 5 #{}))
  (def overlapping-requests {:b #{[0 0] [1 1]}
                             :a #{[0 1] [1 1] [1 2]}
                             :c #{[3 3] [3 4] [4 4]}})
  (apply-requests-2 populate-2 empty-grid-5-5 overlapping-requests)
  (def grid-ex [[#{:b} #{} #{} #{} #{}]
                [#{:a} #{:b :a} #{} #{} #{}]
                [#{} #{:a} #{} #{} #{}]
                [#{} #{} #{} #{:c} #{}]
                [#{} #{} #{} #{:c} #{:c}]])
  (def sat-ex {[0 0] #{:b}, [0 1] #{:a}, [1 2] #{:a},
               [3 3] #{:c}, [3 4] #{:c}, [4 4] #{:c}})
  (def rej-ex {[1 1] #{:b :a}})
  (def rej-ex {[1 1] #{:b :a} [2 2] #{:c :d}})
  (for [[[x y] req-set] rej-ex]
    (assoc-in grid-ex [x y] #{}))
  (reduce (fn [g [[ch t] _]]
            (assoc-in g [t ch] #{}))
          grid-ex rej-ex)
  (check-satisfied sat-rule (apply-requests-2 populate-2
                                              empty-grid-5-5
                                              overlapping-requests))
  (check-rejected rej-rule (apply-requests-2 populate-2
                                             empty-grid-5-5
                                             overlapping-requests))

  (def rejs (check-rejected rej-rule (apply-requests-2 populate-2
                                                       empty-grid-5-5
                                                       overlapping-requests)))
  (let [g (apply-requests-2 populate-2
                            empty-grid-5-5
                            overlapping-requests)]
    (remove-rejects g (check-rejected rej-rule g)))
  (let [g (apply-requests-2 populate-2 empty-grid-5-5 overlapping-requests)]
    (let [sat (check-satisfied sat-rule g)
          rej (check-rejected rej-rule g)]
      {:before empty-grid-5-5 :after (remove-rejects g rej) :sat sat :rej rej}))
  (test-requests empty-grid-5-5 overlapping-requests)


  ())