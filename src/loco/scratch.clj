(ns loco.scratch
  (:require [loco.core :refer :all]
    [loco.constraints :refer :all]))


; https://github.com/flybot-sg/loco is the most up-to-date library



(defn generate-acceptable-requests-2
      "take a set of requests with possible flexible needs and
       return a set of requests where those needs are locked
       down so that all the requests can work"

  [grid requests]

  (let [comb (merge requests (reqs-from-grid grid))
        ids (id-map comb)]

    (->>
      ; take all the requests
      comb

      ; build all the constraints
      (build-all-constraints ids)

      ; solve the constraints
      solution

      ; turn the solution back into a REQUEST
      (make-request ids))))




(defn- id-map
  "build a map to convert keywords, used by REQUESTS, into integers,
   which are required by loco; and vice versa

   in Clojure, this is perfectly acceptable - maps can be keyed by
   type of value, even in the same map!"

  [requests]
  (merge {:_ 0}
         { 0 :_}
         (zipmap (keys requests) (iterate inc 1))
         (zipmap (iterate inc 1) (keys requests))))




(def empty-grid [[#{} #{} #{} #{} #{}]
                 [#{} #{} #{} #{} #{}]
                 [#{} #{} #{} #{} #{}]
                 [#{} #{} #{} #{} #{}]
                 [#{} #{} #{} #{} #{}]])

(def used-grid [[#{} #{} #{} #{} #{}]
                [#{} #{} #{} #{} #{}]
                [#{} #{} #{} #{} #{}]
                [#{:q} #{} #{} #{} #{}]
                [#{:q} #{:q} #{} #{} #{}]])




(generate-acceptable-requests-2 empty-grid requests-0)
(generate-acceptable-requests-2 empty-grid requests-1)
(generate-acceptable-requests-2 empty-grid requests-2)
(generate-acceptable-requests-2 empty-grid requests-3)
(generate-acceptable-requests-2 empty-grid requests-4)

(generate-acceptable-requests-2 used-grid requests-0)
(generate-acceptable-requests-2 used-grid requests-1)
(generate-acceptable-requests-2 used-grid requests-2)
(generate-acceptable-requests-2 used-grid requests-3)
(generate-acceptable-requests-2 used-grid requests-4)













(def grid [[#{:b} #{:h} #{:h} #{:h} #{:h}]
           [#{:a} #{:d} #{} #{} #{}]
           [#{:g} #{:a} #{:e} #{} #{}]
           [#{:g} #{:f} #{:e} #{:c} #{}]
           [#{:g} #{:f} #{:e} #{:c} #{:c}]])

(def id-map {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6})


(defn- build-pre-existing-constraints

  [id-map grid]
  (remove
    nil?
    (flatten
      (for [ch (range (count (first grid)))
            ts (range (count grid))]
        (list
          ($in [:cell ch ts] [0 (get id-map (first (get-in grid [ts ch])) 0)])
          (if (not (empty? (get-in [ts ch] grid)))
            ($= [:cell ch ts] (get id-map (first (get-in grid [ts ch])) 0))))))))

(get (get grid 0 ) 0)
(get id-map (first (get-in grid [0 0])))

(for [ch (range (count (first grid)))
      ts (range (count grid))]
  [ts ch (get-in grid [ts ch])])


(build-pre-existing-constraints id-map grid)



















(first #{[[0] 0] [[0 1 2 3] 1]})
(vals overlapping-requests)
(first (vals overlapping-requests))
(ffirst (vals overlapping-requests))
(first (ffirst (vals overlapping-requests)))
(map (fn [s]
       (for [x s
             y (rest x)]
         y))
     (vals overlapping-requests))

(flatten (map (fn [s]
                (for [x s
                      y (first x)]
                  y))
              (vals overlapping-requests)))
(into #{} (flatten (map (fn [s]
                          (for [x s
                                y (first x)]
                            y))
                        (vals overlapping-requests))))

(def i-range (into #{} (flatten (map (fn [s]
                                       (for [x s
                                             y (first x)]
                                         y))
                                     (vals overlapping-requests)))))












; time to think some more
;
; we need a different data structure. in Clojure, we'll say this a lot
;











(def fill-in {:g #{[[0] 1] [[0] 2] [[0] 3] [[0] 4]}
              :h #{[[1] 0] [[2] 0] [[3] 0] [[4] 0]}})


(defn request-fixed-constraints [demands]
  (remove nil?
          (for [req-id (keys demands)
                slot   (req-id demands)]
            (let [[channels time-slot] slot]
              (if (= 1 (count channels))
                ($= [:requestor req-id time-slot (first channels)]
                    (first channels)))))))

(request-fixed-constraints fill-in)


(defn request-flex-constraints [demands]
  (remove nil?
          (for [req-id (keys demands)
                slot   (req-id demands)]
            (let [[channels time-slot] slot]
              (if (< 1 (count channels))
                ($in [:requestor req-id time-slot] channels)
                ($in [:requestor req-id time-slot] (keys demands)))))))

(request-flex-constraints fill-in)



(defn solve-all [demands]
  (into (sorted-map) (solutions
                       (concat
                         (request-flex-constraints demands)
                         (request-fixed-constraints demands)))))

(solutions
  (concat
    (request-flex-constraints fill-in)
    (request-fixed-constraints fill-in))
  :timeout 1000)

(solve-all fill-in)











(flatten '(({[0 0] #{:b}})
           ({[0 1] #{:b}} {[1 1] #{:b}} {[2 1] #{:b}} {[3 1] #{:b}})))

(merge-with clojure.set/union
            (flatten '(({[0 0] #{:b}})
                       ({[0 1] #{:b}} {[1 1] #{:b}}
                        {[2 1] #{:b}} {[3 1] #{:b}}))))

(for [x '(({[0 0] #{:b}})
          ({[0 1] #{:b}} {[1 1] #{:b}}
           {[2 1] #{:b}} {[3 1] #{:b}}))]
  (apply merge-with clojure.set/union x))


(apply merge-with clojure.set/union
       (for [x '(({[0 0] #{:b}})
                 ({[0 1] #{:b}} {[1 1] #{:b}}
                  {[2 1] #{:b}} {[3 1] #{:b}}))]
         (apply merge-with clojure.set/union x)))











(defn req-grid-3 [requests]
  (flatten
    (let [id-map (merge {:_ 0}
                        (zipmap (keys requests) (iterate inc 1)))
          s2id   (for [req-id (keys requests)
                       slot   (req-id requests)]
                   (let [[ch time-slot] slot]
                     [[ch time-slot] req-id]))
          re     (remove nil?
                         (for [p slots2req-id]
                           (let [[[x ts] id] p]
                             (if (>= 1 (count x))
                               [(first x) ts id]))))]
      (for [[[ch ts] req-ids] s2id]
        (if (< 1 (count req-ids))
          (let [ids (into [] (for [k req-ids]
                               (k id-map)))]
            ($in [:cell ch ts] ids))))
      (for [[ch ts id] re]
        (list
          ($= [:cell ch ts] (id id-map)))))))











(def slot2req-idSet
  (apply merge-with clojure.set/union
         (for [x (for [[req-id reqs] requests-1
                       [cs ts] reqs]
                   (if (<= 1 (count cs))
                     ;true
                     (for [c cs]
                       (let [ids (into [] (for [k req-ids]
                                            (k id-map)))]
                         ($in [:cell c ts] ids)))
                     ;false
                     (list {cs cs})))]
           (apply merge-with clojure.set/union x))))






(apply merge-with clojure.set/union
       (for [[req-id reqs] {:b #{[[0 1] 0]}
                            :a #{[1 0] [1 1]}}
             [cs ts] reqs]
         (if (coll? cs)
           (for [c cs]
             {[c ts] #{req-id}})
           {[cs ts] #{req-id}

            (for [[req-id reqs] {:b #{[[0 1] 0]}
                                 :a #{[1 0] [1 1]}}
                  [cs ts] reqs
                  c cs]
              {[c ts] #{req-id}})})))




(for [[[ch ts] r] (build-defaults requests)]
  ($in [:cell ch ts] (into []
                           (flatten [0
                                     (for [x r]
                                       (x id-map))]))))





(def id-map (merge {:_ 0}
                   (zipmap (keys requests-1)
                           (iterate inc 1))))

(for [x (for [[req-id reqs] requests-1
              [cs ts] reqs]
          (if (coll? cs)
            (for [c cs]
              {[c ts] #{req-id}})
            {[cs cs] #{req-id}}))]
  x)

(flatten (for [x (for [[req-id reqs] requests-1
                       [cs ts] reqs]
                   (if (coll? cs)
                     (apply merge (for [c cs]
                                    ($in [:cell c ts] (req-id id-map))))
                     (list
                       ($= [:cell cs ts] (req-id id-map))
                       ($in [:cell cs ts] [0 (req-id id-map)]))))]
           x))


(def cs [0 1 2 3])
(def c 0)

(apply $or
       (for [c cs]
         (apply $and
                (for [r (range (count cs))]
                  (if (= c r)
                    ($= [:cell c 1] :b)
                    ($!= [:cell r 1] :b))))))




(for [[req-id reqs] {:b #{[[0 1] 0]}
                     :a #{[1 0] [1 1]}}
      [cs ts] reqs]
  (if (coll? cs)
    (for [c cs]
      {[c ts] #{req-id}})
    {[cs ts] #{req-id}}))



(merge-with clojure.set/union
            {[0 0] #{:b}} {[1 0] #{:b}} {[1 0] #{:a}} {[1 1] #{:a}})



(apply merge-with clojure.set/union
       (for [[req-id reqs] {:b #{[[0 1] 0]}
                            :a #{[1 0] [1 1]}}
             [cs ts] reqs]
         (if (coll? cs)
           (apply conj
                  (for [c cs]
                    {[c ts] #{req-id}}))
           {[cs ts] #{req-id}})))



(into {} (filter #(not (= :_ (key %))) {:b #{[0 0] [0 1]},
                                        :a #{[1 1] [1 2]},
                                        :_ #{[3 1] [2 1]},
                                        :c #{[3 3] [3 4] [4 4]}}))








;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; Okay, the first Engelberg example could be basically assigning to
; channel slots in a single time (the data structure makes this obvious).
; So we need to figure out how to extend the example to a 2d matrix
;
; The loco GitHub includes a sudoku example in the test suite, maybe this
; will give us some hints.
;


(def sudoku-base-model
  "Base board constraints, without the puzzle-specific hints."
  (concat
    ;; every cell is between 1-9
    (for [i (range 9)
          j (range 9)]
      ($in [:cell i j] 1 9))
    ;; each row contains 1-9 exactly once
    (for [i (range 9)]
      ($distinct (for [j (range 9)] [:cell i j])))
    ;; each column contains 1-9 exactly once
    (for [j (range 9)]
      ($distinct (for [i (range 9)] [:cell i j])))
    ;; each 3x3 section contains 1-9 exactly once
    (for [x (range 3)
          y (range 3)]
      ($distinct (for [i (range (* x 3) (* (inc x) 3))
                       j (range (* y 3) (* (inc y) 3))]
                   [:cell i j])))))

(defn sudoku-puzzle->model
      "Takes a starting board, a vector of vectors.
    If numbers are found in cells, they will be given as \"hints\"
    to further define the specific puzzle."

  [starting-board]

  (let [hints (for [i (range 9)
                    j (range 9)
                    :when (integer? (get-in starting-board [i j]))]
                ($= [:cell i j] (get-in starting-board [i j])))]
    (concat sudoku-base-model hints)))

(defn solution->board
      "Given a solution map, prettifies it into a vector of vectors"
  [sol]
  (mapv vec
        (for [i (range 9)]
          (for [j (range 9)]
            (sol [:cell i j])))))

(def sample-puzzle
  '[[- - - - - - - - -]
    [- - - - - 3 - 8 5]
    [- - 1 - 2 - - - -]

    [- - - 5 - 7 - - -]
    [- - 4 - - - 1 - -]
    [- 9 - - - - - - -]

    [5 - - - - - - 7 3]
    [- - 2 - 1 - - - -]
    [- - - - 4 - - - 9]])


(->> sample-puzzle
     sudoku-puzzle->model
     solutions
     (map solution->board))




(def sample-solution
  [[9 8 7 6 5 4 3 2 1]
   [2 4 6 1 7 3 9 8 5]
   [3 5 1 9 2 8 7 4 6]

   [1 2 8 5 3 7 6 9 4]
   [6 3 4 8 9 2 1 5 7]
   [7 9 5 4 6 1 8 3 2]

   [5 1 9 2 8 6 4 7 3]
   [4 7 2 3 1 9 5 6 8]
   [8 6 3 7 4 5 2 1 9]])

(= (->> sample-puzzle
        sudoku-puzzle->model
        solutions
        (map solution->board))
   (list sample-solution))



;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; So where does that leave us? Can we develop an approach to our
; resource allocation problem?
;
; First, I think we need to figure out what problem we are solving,
; namely - are we trying to develop a working plan from a set of flexible
; requests, or we trying to make the set of requests work?
;
; These are actually different problems. Developing a working plan
; means we may not have to change any of our existing code, as a
; "working plan" can be put directly into the grid using our existing
; code.
;
; "Making the requests work" probably requires changing some (or all)
; of the code in allocation-try-2.clj...
;
; The "clojurist" in me thinks we want the former - its just another
; data transformation:
;
;        flexible-request -> fixed-request -> working-grid
;
;
;
; In this case, I think we can just go back to the original constraint
; example, as we really DO only want 1 requestor per slot
;



;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; We can also look at the loco work I did years ago
;
; see baking.clj and coloring.clj
;
;


; and Mark wrote another post:
;     https://programming-puzzler.blogspot.com/2014/03/optimization-with-loco.html
;
; that might help

;
; one thing I see - let's do this "long hand" first
;   and figure out how to generalize it once we understand the constraints
;
; see also loco_rules_2.clj
;


;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; :a = 0, :b = 1
; {:a #{[[0  ] 0] [[0 1] 1]}  <- :a [0 0], :a [0 1] or [1 1]
;
;  :b #{[[0 1] 0] [[1  ] 1]}} <- :b [0 0] or [1 0], :b [1 1]
;
; grid [[#{} #{}]
;       [#{} #{}]]

(let [model [($in [:cell 0 0] (range 2))                    ; :a or :b
             ($in [:cell 0 1] (range 2))                    ; :a or :b
             ($in [:cell 1 0] (range 2))                    ; :a or :b
             ($in [:cell 1 1] (range 2))                    ; :a or :b
             ($= [:cell 0 0] 0)                             ; :a
             ($= [:cell 1 1] 1)]]                           ; :b

  (into (sorted-map)
        (solutions model)))

;
; 1) all cells can take on every :request-id value as a default
;
; 2) cells with a few requestors map just that collection
;
; 3) lock down cells that are required by a certain requestor
;

(into [] #{1 2 3})

;;;;;;;;;;;;;;;;;;;;;

; :a = 0, :b = 1, :c = 2
;
; =>  '!' indicated a fixed request (must be fulfilled)
;
; {:b #{[[0  ] 0] [[0 1 2] 1]}  <- :b [0,0], :b [0,1] or [1,1] or [2,1]
;
;  :a #{[[0 1] 0] [[1    ] 1]}  <- :a [0,0] or [1,0], :a [1,1]
;
;  :c #{[[1 2] 0] [[1 2  ] 1]}} <- :c [1,0] or [2,0], :c [1,1] or [2,1]
;
; sort of...
;
;    grid [[#{:a! :b} #{:b :c    } #{:c    }]
;          [#{:a    } #{:a :b! :c} #{:a :c }]]

;
; Note: we can add multiple $in clauses, each more
; restrictive than the last, and the evaluation still works!
;


(let [model [; defaults
             ($in [:cell 0 0] [0 1 2])
             ($in [:cell 0 1] [0 1 2])
             ($in [:cell 1 0] [0 1 2])
             ($in [:cell 1 1] [0 1 2])
             ($in [:cell 2 0] [0 1 2])

             ; flex-ranges
             ($in [:cell 0 0] [0 1])
             ($in [:cell 2 1] [0 2])
             ($in [:cell 1 0] [1 2])
             ($in [:cell 2 0] [2])

             ; fixed requests
             ($= [:cell 0 0] 0)
             ($= [:cell 1 1] 1)]]
  (into (sorted-map)
        (solution model)))
; => {[:cell 0 0] 0, [:cell 1 0] 1, [:cell 2 0] 2,
;     [:cell 0 1] 0, [:cell 1 1] 1, [:cell 2 1] 0} <- reformatted
;
; OR
;
;  [[#{:a} #{:b} #{:c}]
;   [#{:a} #{:b} #{:a}]]

; but there are many possible solutions:
;
(let [defaults [; defaults
                ($in [:cell 0 0] [0 1 2])
                ($in [:cell 0 1] [0 1 2])
                ($in [:cell 1 0] [0 1 2])
                ($in [:cell 1 1] [0 1 2])
                ($in [:cell 2 0] [0 1 2])]

      flex     [; flex-ranges
                ($in [:cell 0 0] [0 1])
                ($in [:cell 2 1] [0 2])
                ($in [:cell 1 0] [1 2])
                ($in [:cell 2 0] [2])]

      fixed    [; fixed requests
                ($= [:cell 0 0] 0)
                ($= [:cell 1 1] 1)]]

  (solutions (concat defaults flex fixed)))



; some notion of scoring would be good (which one is "best")
;
; we will also need a function to convert the solution(s) back into our
; planning format
;


; MAYBE

; try with overlapping-requests

; :b 1 :a 2 :c 3, we need 0 -> not assigned
;
; x->  0       1        2      3     4
;
; 0 [[#{:b!} #{}       #{}   #{}    #{}]
; 1  [#{:b}  #{:a! :b} #{:b} #{:b}  #{}]
; 2  [#{}    #{:a!}    #{}   #{}    #{}]
; 3  [#{}    #{}       #{}   #{:c!} #{}]
; 4  [#{}    #{}       #{}   #{:c!} #{:c!}]]

; should produce:
;  [0 0] 1/:b
;  [0 1] 1/:b =or= [1 2] 1/:b =or= [1 3] 1/:b
;  [1 1] 2/:a
;  [2 1] 1/:a
;  [3 3] 3/:c
;  [3 4] 3/:c
;  [4 4] 3/:c
;

;  [:cell 0 0] 1/:b
;  [:cell 0 1] 1/:b
;  [:cell 1 1] 2/:a
;  [:cell 1 2] 2/:a <- 1/:b
;  [:cell 2 1] 1/:b <- 2
;  [:cell 3 1] 1/:b
;  [:cell 3 3] 3/:c
;  [:cell 3 4] 3/:c
;  [:cell 4 4] 3/:c


(def overlapping-requests {:b #{[[0] 0] [[0 1 2 3] 1]}
                           :a #{[[1] 1] [[1] 2]}
                           :c #{[[3] 3] [[3] 4] [[4] 4]}})

(let [defaults [(for [i (range 5)
                      j (range 5)]
                  ($in [:cell i j] [0 1 2 3]))]

      flex     [($in [:cell 0 1] [1])
                ($in [:cell 1 1] [1 2])
                ($in [:cell 2 1] [1 2])
                ($in [:cell 3 1] [1])]

      fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)
                ($= [:cell 4 4] 3)]]

  (into (sorted-map)
        (solution (concat (first defaults) flex fixed) :timeout 1000)))

; => {[:cell 0 0] 2, [:cell 1 0] 0, [:cell 2 0] 0, [:cell 3 0] 0, [:cell 4 0] 0,
;     [:cell 0 1] 2, [:cell 1 1] 1, [:cell 2 1] 1, [:cell 3 1] 1, [:cell 4 1] 0,
;     [:cell 0 2] 0, [:cell 1 2] 1, [:cell 2 2] 0, [:cell 3 2] 0, [:cell 4 2] 0,
;     [:cell 0 3] 0, [:cell 1 3] 0, [:cell 2 3] 0, [:cell 3 3] 3, [:cell 4 3] 0,
;     [:cell 0 4] 0, [:cell 1 4] 0, [:cell 2 4] 0, [:cell 3 4] 3, [:cell 4 4] 3}
;
; or
;
;  [[#{:b}  #{}   #{} #{}   #{}]
;   [#{:b}  #{:a} #{} #{:b} #{}]
;   [#{}    #{:a} #{} #{}   #{}]
;   [#{}    #{:b} #{} #{:c} #{}]
;   [#{}    #{}   #{} #{:c} #{:c}]]


;;;;;;
; WEIRD
;
;  letting the solver run longer returns this:

;=>
;{[:cell 0 0] 2,[:cell 1 0] 0,[:cell 2 0] 0,[:cell 3 0] 1,[:cell 4 0] 0,
; [:cell 0 1] 2,[:cell 1 1] 1,[:cell 2 1] 1,[:cell 3 1] 2,[:cell 4 1] 1,
; [:cell 0 2] 0,[:cell 1 2] 1,[:cell 2 2] 2,[:cell 3 2] 0,[:cell 4 2] 2,
; [:cell 0 3] 0,[:cell 1 3] 0,[:cell 2 3] 0,[:cell 3 3] 3,[:cell 4 3] 0,
; [:cell 0 4] 1,[:cell 1 4] 0,[:cell 2 4] 2,[:cell 3 4] 3,[:cell 4 4] 3)

;  [[#{:b}  #{}   #{}   #{:a} #{}]
;   [#{:b}  #{:a} #{:a} #{:b} #{:a}]
;   [#{}    #{:a} #{:b} #{}   #{:b}]
;   [#{}    #{}   #{}   #{:c} #{}]
;   [#{:a}  #{}   #{:b} #{:c} #{:c}]]
;
; maybe this means we need to trim down "defaults" to only the cells in
; play?


(let [defaults [($in [:cell 0 0] [0 1 2 3])
                ($in [:cell 1 1] [0 1 2 3])
                ($in [:cell 1 2] [0 1 2 3])
                ($in [:cell 3 3] [0 1 2 3])
                ($in [:cell 3 4] [0 1 2 3])
                ($in [:cell 4 4] [0 1 2 3])]

      flex     [($in [:cell 0 1] [1])
                ($in [:cell 1 1] [1 2])
                ($in [:cell 2 1] [1 2])
                ($in [:cell 3 1] [1])]

      fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)
                ($= [:cell 4 4] 3)]]
  (into (sorted-map)
        (solutions (concat defaults
                           flex fixed)
                   :timeout 1000)))

;({:type :int-domain, :can-init-var true, :name [:cell 0 0], :domain [0 1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 1 1], :domain [0 1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 1 2], :domain [0 1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 3 3], :domain [0 1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 3 4], :domain [0 1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 4 4], :domain [0 1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 0 1], :domain [1]}
; {:type :int-domain, :can-init-var true, :name [:cell 1 1], :domain [1 2]}
; {:type :int-domain, :can-init-var true, :name [:cell 2 1], :domain [1 2]}
; {:type :int-domain, :can-init-var true, :name [:cell 3 1], :domain [1]}
; {:arg2 1, :eq "=", :type :arithm-eq, :arg1 [:cell 0 0]}
; {:arg2 2, :eq "=", :type :arithm-eq, :arg1 [:cell 1 1]}
; {:arg2 2, :eq "=", :type :arithm-eq, :arg1 [:cell 1 2]}
; {:arg2 3, :eq "=", :type :arithm-eq, :arg1 [:cell 3 3]}
; {:arg2 3, :eq "=", :type :arithm-eq, :arg1 [:cell 3 4]}
; {:arg2 3, :eq "=", :type :arithm-eq, :arg1 [:cell 4 4]})

;(
; {:type :int-domain, :can-init-var true, :name [:cell 0 0], :domain [1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 0 1], :domain [1 2 3]})
; {:type :int-domain, :can-init-var true, :name [:cell 1 1], :domain [1 2]}
; {:type :int-domain, :can-init-var true, :name [:cell 1 2], :domain [1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 2 1], :domain [1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 3 3], :domain [1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 3 4], :domain [1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 3 1], :domain [1 2 3]}
; {:type :int-domain, :can-init-var true, :name [:cell 4 4], :domain [1 2 3]}
; {:arg2 1, :eq "=", :type :arithm-eq, :arg1 [:cell 0 0]}
; {:arg2 1, :eq "=", :type :arithm-eq, :arg1 [:cell 0 1]}
; {:arg2 2, :eq "=", :type :arithm-eq, :arg1 [:cell 1 2]}
; {:arg2 1, :eq "=", :type :arithm-eq, :arg1 [:cell 2 1]}
; {:arg2 1, :eq "=", :type :arithm-eq, :arg1 [:cell 3 1]}
; {:arg2 3, :eq "=", :type :arithm-eq, :arg1 [:cell 3 3]}
; {:arg2 3, :eq "=", :type :arithm-eq, :arg1 [:cell 3 4]}
; {:arg2 3, :eq "=", :type :arithm-eq, :arg1 [:cell 4 4]}




;{[:cell 0 0] 1,
; [:cell 0 1] 1,
; [:cell 1 1] 2,
; [:cell 1 2] 2,
; [:cell 2 1] 2,
; [:cell 3 1] 1,
; [:cell 3 3] 3,
; [:cell 3 4] 3,
; [:cell 4 4] 3}

;{[:cell 0 0] 1, :b
; [:cell 0 1] 1, :b
; [:cell 1 1] 2, :a
; [:cell 1 2] 2, :a
; [:cell 2 1] 1, :b <- 2
; [:cell 3 1] 1, :b
; [:cell 3 3] 3, :c
; [:cell 3 4] 3, :c
; [:cell 4 4] 3} :c

(defn req-grid [demands]
  (flatten
    (let [id-map (zipmap (keys demands) (iterate inc 1))
          re     (apply merge-with clojure.set/union
                        (for [[req-id reqs] demands
                              [cs ts] reqs
                              c cs]
                          {[c ts] #{req-id}}))]
      (for [[[ch ts] req-ids] re]
        (if (< 1 (count req-ids))
          (let [ids (into [] (for [k req-ids]
                               (k id-map)))]
            ($in [:cell ch ts] ids))
          (list
            ($= [:cell ch ts] ((first req-ids) id-map))
            ($in [:cell ch ts] (into [] (vals id-map)))))))))



(let [defaults [($in [:cell 0 0] [1])
                ($in [:cell 1 1] [2])
                ($in [:cell 1 2] [2])
                ($in [:cell 3 3] [3])
                ($in [:cell 3 4] [3])
                ($in [:cell 4 4] [3])]

      flex     [($in [:cell 0 1] [1])
                ($in [:cell 1 1] [1 2])
                ($in [:cell 2 1] [1 2])
                ($in [:cell 3 1] [1])]

      fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)
                ($= [:cell 4 4] 3)]]
  (into (sorted-map)
        (solutions (concat defaults
                           flex fixed)
                   :timeout 1000)))



(if (< 1 (count ch))
  ($= [:cell ch ts] (ch id-map)))

(def requests {:b #{[[0] 0] [[0 1 2 3] 1]}
               :a #{[[0] 0] [[1] 2]}
               :c #{[[3] 3] [[3] 4] [[4] 4]}})

(into (sorted-map) (solution (req-grid requests) :timeout 1000))
; =>
; {[:cell 0 0] 1,
;  [:cell 0 1] 1, [:cell 1 1] 1, [:cell 2 1] 1, [:cell 3 1] 1,
;                 [:cell 1 2] 2,
;                                               [:cell 3 3] 3,
;                                               [:cell 3 4] 3, [:cell 4 4] 3}
;
;
;  [[#{:b}  #{}   #{}   #{}   #{}]
;   [#{:b}  #{:a} #{:a} #{}   #{}]
;   [#{}    #{:b} #{}   #{}   #{}]
;   [#{}    #{}   #{}   #{:c} #{}]
;   [#{}    #{}   #{}   #{:c} #{:c}]]

;  [[#{:b}  #{}   #{} #{}   #{}]
;   [#{:b}  #{:a} #{} #{:b} #{}]
;   [#{}    #{:a} #{} #{}   #{}]
;   [#{}    #{:b} #{} #{:c} #{}]
;   [#{}    #{}   #{} #{:c} #{:c}]]



(into (sorted-map) (solutions (req-grid fill-in) :timeout 1000))





(def requests {:b #{[[0] 0] [[0 1 2 3] 1]}
               :a #{[[0] 0] [[1] 2]}
               :c #{[[3] 3] [[3] 4] [[4] 4]}})

(def slot2req-idSet (apply merge-with clojure.set/union
                           (for [[req-id reqs] requests
                                 [cs ts] reqs
                                 c cs]
                             {[c ts] #{req-id}})))


(def slots2req-id (for [req-id (keys requests)
                        slot   (req-id requests)]
                    (let [[ch time-slot] slot]
                      [[ch time-slot] req-id])))

(remove nil?
        (for [p slots2req-id]
          (let [[[x ts] id] p]
            (if (>= 1 (count x))
              [(first x) ts id]))))


(defn req-grid-2 [requests]
  (flatten
    (let [id-map (merge {:_ 0}
                        (zipmap (keys requests) (iterate inc 1)))
          s2id   (for [req-id (keys requests)
                       slot   (req-id requests)]
                   (let [[ch time-slot] slot]
                     [[ch time-slot] req-id]))
          re     (remove nil?
                         (for [p slots2req-id]
                           (let [[[x ts] id] p]
                             (if (>= 1 (count x))
                               [(first x) ts id]))))]
      (for [[[ch ts] req-ids] s2id]
        (if (< 1 (count req-ids))
          (let [ids (into [] (for [k req-ids]
                               (k id-map)))]
            ($in [:cell ch ts] ids))))
      (for [[ch ts id] re]
        (list
          ($= [:cell ch ts] (id id-map)))))))


(into (sorted-map)
      (solutions (req-grid overlapping-requests)
                 :timeout 1000))






;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;


; This stuff happened BEFORE I did the static modeling above
;        |
;        V

; some requests
;
;   note: we have to make the "channel" part be a vector since it provides
;         the "flexibility" we want to have
;
(def overlapping-requests {:b #{[[0] 0] [[0 1 2 3] 1]}
                           :a #{[[1] 1] [[1] 2]}
                           :c #{[[3] 3] [[3] 4] [[4] 4]}})


(def demands {:a [[[3 4 5 9] 7]] :b [[[1] 1]]})


(defn requestor-vars [demands]
  (for [req-id (keys demands)
        slot   (req-id demands)]
    (let [[_ time-slot] slot]
      [:requestor req-id time-slot])))
; => ([:requestor :b] [:requestor :a] [:requestor :c])


; figure out what the channel ranges are for each time-slot
;
;    this actually seems to work! (p.s. it doesn't [1])
;
(defn request-constraints [demands]
  (for [req-id (keys demands)
        slot   (req-id demands)]
    (let [[channels time-slot] slot]
      ($in [:requestor req-id time-slot] channels))))


; so we know what channels each requestor would like to have for a given
; time-slot. what's next?
;
; in the original example, we added a constraint that each "person"
; had to be in a different slot, but that doesn't make sense here. Instead
; we need a constraint that each "slot" can only have 1 requestor
;


; $all-different? makes sure each :requestor ends up in a different slot
;
; (p.s. looks like we DON'T need this [1])
;
(def all-different-constraint
  (apply $all-different? (requestor-vars overlapping-requests)))


; hook both constraints together
;
(def all-constraints
  (conj (request-constraints overlapping-requests)
        all-different-constraint))

; does this work?
;
(into (sorted-map) (solution all-constraints))              ; => no


; [1] we don't actually want the :all-different-constraint; a
; requestor CAN get more than 1 slot
;
(defn solve [demands]
  (into (sorted-map) (solutions
                       (request-constraints demands))))

(solve demands)
; => {[:requestor :a 7] 9, [:requestor :b 1] 1}

(solve overlapping-requests)
; => {[:requestor :a 1] 1,
;     [:requestor :a 2] 1,
;     [:requestor :b 0] 0,
;     [:requestor :b 1] 0,
;     [:requestor :c 3] 4,
;     [:requestor :c 4] 4}    <- yay!



(def fill-in {:g #{[[0] 1] [[0] 2] [[0] 3] [[0] 4]}
              :h #{[[1] 0] [[2] 0] [[3] 0] [[4] 0]}})

(solve fill-in)
; => {[:requestor :g 1] 0,
;     [:requestor :g 2] 0,
;     [:requestor :g 3] 0,
;     [:requestor :g 4] 0,
;     [:requestor :h 0] 1}   <- oops!

;
; [2] this currently doesn't work, specifically :h, because :h requests
; more than 1 channel at the same time-slot and our constraint builder
; doesn't do that right.
;
(request-constraints fill-in)
; => ({:type :int-domain, :can-init-var true, :name [:requestor :g 2], :domain [0]}
;     {:type :int-domain, :can-init-var true, :name [:requestor :g 4], :domain [0]}
;     {:type :int-domain, :can-init-var true, :name [:requestor :g 1], :domain [0]}
;     {:type :int-domain, :can-init-var true, :name [:requestor :g 3], :domain [0]}
;     {:type :int-domain, :can-init-var true, :name [:requestor :h 0], :domain [1]}
;     {:type :int-domain, :can-init-var true, :name [:requestor :h 0], :domain [1]}
;     {:type :int-domain, :can-init-var true, :name [:requestor :h 0], :domain [1]}
;     {:type :int-domain, :can-init-var true, :name [:requestor :h 0], :domain [1]})



;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; from Mark's blog
; https://programming-puzzler.blogspot.com/2014/03/optimization-with-loco.html
;


(def demands
  {:12am-4am 8
   :4am-8am  10
   :8am-12pm 7
   :12pm-4pm 12
   :4pm-8pm  4
   :8pm-12am 4})


(solution
  [($in :bus-12am-8am 0 12)
   ($in :bus-4am-12pm 0 12)
   ($in :bus-8am-4pm 0 12)
   ($in :bus-12pm-8pm 0 12)
   ($in :bus-4pm-12am 0 12)
   ($in :bus-8pm-4am 0 12)
   ($>= ($+ :bus-8pm-4am :bus-12am-8am) (demands :12am-4am))
   ($>= ($+ :bus-12am-8am :bus-4am-12pm) (demands :4am-8am))
   ($>= ($+ :bus-4am-12pm :bus-8am-4pm) (demands :8am-12pm))
   ($>= ($+ :bus-8am-4pm :bus-12pm-8pm) (demands :12pm-4pm))
   ($>= ($+ :bus-12pm-8pm :bus-4pm-12am) (demands :4pm-8pm))
   ($>= ($+ :bus-4pm-12am :bus-8pm-4am) (demands :8pm-12am))]
  :minimize
  ($+ :bus-12am-8am :bus-4am-12pm :bus-8am-4pm
      :bus-12pm-8pm :bus-4pm-12am :bus-8pm-4am))

; => {:bus-12am-8am 4, :bus-4am-12pm 6, :bus-8am-4pm 1,
;     :bus-12pm-8pm 11, :bus-4pm-12am 0, :bus-8pm-4am 4}


; personally, I find the use of :buses in the constraints confusing,
; as we are actually talking about the time-slots, it's the value that
; is the number of buses
;
(defn minimize-buses
      "Takes a vector of the demands for any number of equally-spaced time slots.
       span is the number of time slots that a bus's operating time spans"
  [demands span]
  (let [time-slots (count demands),
        max-demand (apply max demands),

        declarations
                   (for [i (range time-slots)]
                     ($in [:buses i] 0 max-demand))

        constraints
                   (for [i (range time-slots)]
                     ($>=
                       (apply $+ (for [j (range (inc (- i span)) (inc i))]
                                   [:buses (mod j time-slots)]))
                       (demands i)))]

    (solution
      (concat declarations constraints)
      :minimize (apply $+ (for [i (range time-slots)] [:buses i])))))


(minimize-buses [8 10 7 12 4 4] 2)
; => {[:buses 0] 4, [:buses 1] 6, [:buses 2] 1,
;     [:buses 3] 11, [:buses 4] 0, [:buses 5] 4}


; notice that this one takes a noticeable amount of time to resolve.
; the blog mentions that we can pass a :timeout property to return in a
; reasonable time, even if the constraints don't resolve
;
(into (sorted-map)
      (minimize-buses [1 5 7 9 11 12 18 17 15 13 4 2] 4))
; => {[:buses 0] 1, [:buses 1] 4, [:buses 2] 3,
;     [:buses 3] 1, [:buses 4] 6, [:buses 5] 2,
;     [:buses 6] 9, [:buses 7] 0, [:buses 8] 4,
;     [:buses 9] 0, [:buses 10] 0, [:buses 11] 0


;
; maybe we need to consider the grid and not the requestors...
;
;

(def requests {:b #{[[0] 0] [[0 1 2 3] 1]}
               :a #{[[1] 1] [[1] 2]}
               :c #{[[3] 3] [[3] 4] [[4] 4]}})


(def demands {:a [[[3 4 5 9] 7]] :b [[[1] 1]]})


(defn grid [demands]
  (let [channels   (into #{}
                         (for [[ch ts] (requests (first (keys demands)))]
                           ts))

        time-slots (into #{}
                         (flatten
                           (into []
                                 (for [[ch ts] (requests
                                                 (first (keys demands)))]
                                   ch))))]
    (for [ch channels
          ts time-slots]
      [:cell ch ts])))


; now we can associate the possible requestors for each cell
;




(let [re (apply merge-with clojure.set/union
                (for [[req-id reqs] requests
                      [cs ts] reqs
                      c cs]
                  {[c ts] #{req-id}}))]
  (for [[[ch ts] req-ids] re]
    {[ch ts] req-ids}))

(let [id-map (zipmap (keys requests) (range))
      re     (apply merge-with clojure.set/union
                    (for [[req-id reqs] requests
                          [cs ts] reqs
                          c cs]
                      {[c ts] #{req-id}}))]
  (for [[[ch ts] req-ids] re]
    (let [ids (for [k req-ids]
                (k id-map))]
      {[ch ts] ids})))


(let [id-map (zipmap (keys requests) (iterate inc 1))
      re     (apply merge-with clojure.set/union
                    (for [[req-id reqs] requests
                          [cs ts] reqs
                          c cs]
                      {[c ts] #{req-id}}))]
  (for [[[ch ts] req-ids] re]
    (if (< 1 (count req-ids))
      (let [ids (into [] (for [k req-ids]
                           (k id-map)))]
        ($in [:cell ch ts] ids))
      ($= [:cell ch ts] ((first req-ids) id-map)))))


(let [id-map (zipmap (keys requests) (iterate inc 1))
      re     (apply merge-with clojure.set/union
                    (for [[req-id reqs] requests
                          [cs ts] reqs
                          c cs]
                      {[c ts] #{req-id}}))]
  (for [[[ch ts] req-ids] re]
    (let [ids (into [] (for [k req-ids]
                         (k id-map)))]
      ($in [:cell ch ts] ids))))



