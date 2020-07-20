(ns resource-alloc.sparse-request-rules
  (:require [loco.core :refer :all]
            [loco.constraints :refer :all]
            [resource-alloc.sparse-grid :as sg]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; deviation from loco-rules-2:
;
;   REQUEST (simple) : {"a" #{[0 0] [1 1]}}
;
;           (full)   : {"a" #{{:channel 0 :timeslot 0} {:channel 1 :timeslot 1}}}
;
;   GRID : {"0-0" {:channel 0 :timeslot 0 :allocated-to #{"b"}}
;           "0-1" {:channel 0 :timeslot 1 :allocated-to #{"b"}}}
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;



;
; these functions provide basic data transformations for grids
; and requests
;

(defn- expand-slot
  ""
  [[ch ts]]
  {:channel ch :timeslot ts})



(defn- expand-request
  "expand a simplified request format into the more complete/complex
   format we will use throughout this namespace"

  [requests]
  (into {}
        (map (fn [[id slots]]
               {id (into #{} (map #(expand-slot %) slots))})
             (seq requests))))



(defn- id-map
  "build a map to convert keyword, used by REQUESTS, into
   integers, which are required by loco and vice versa"

  [requests]
  (merge {:_ 0}
         {0 :_}
         (zipmap (keys requests) (iterate inc 1))
         (zipmap (iterate inc 1) (keys requests))))



(defn req-comb
  ""

  [a b]
  (let [fa (first a)
        fb (first b)]
    #{{:channel (:channel fa)
       :timeslot (:timeslot fa)
       :allocated-to (clojure.set/union
                       (:allocated-to fa)
                       (:allocated-to fb))}}))



(defn- expand-flexible-requests
  "expand the collection of 'flexible' request so each is a separate item.
  we will use this to build all the 'default' $in constraints"

  [requests]
  (apply merge-with req-comb
    (flatten
     (for [id (keys requests)
           g  (get requests id)]
       (let [ch (:channel g)
             ts (:timeslot g)]
         (into {}
           (if (vector? ch)
               (for [c ch]
                 {(sg/gen-id c ts)
                  #{{:channel      c
                     :timeslot     ts
                     :allocated-to #{id}}}})
             {(sg/gen-id ch ts)
              #{{:channel      ch
                 :timeslot     ts
                 :allocated-to #{id}}}})))))))



(defn reqs-from-grid
      "'reverse engineer' a map of requests from the grid that they
       'would have' produced, playing on the functional programming
       adage:

          'looking at the data, you can't prove this isn't how it
          was done' "

  [grid]
  (if (or (nil? grid) (empty? grid))
    {}

    (apply
      merge-with
      clojure.set/union
      (remove nil?
              (flatten
                (for [id (keys grid)]
                  (let [g (get grid id)]
                    {(first (:allocated-to g))
                     #{{:channel  (:channel g)
                        :timeslot (:timeslot g)}}})))))))



(defn cells-from-grid
      "'reverse engineer' a map of cell allocations that would have been
       produced if we had run the requests that are built by
       (reqs-from-grid) thorough (solution)

       this also plays on the functional programming adage:

          'looking at the data, you can't prove this isn't how it
           was done' "
  [grid]

  (into
    {}
    (remove
      nil?
      (flatten
        (for [id (keys grid)]
          (let [g (get grid id)]
            (if (empty? (:allocated-to g))
              ()
              {[:cell (:channel g) (:timeslot g)]
               (first (:allocated-to g))})))))))



;
; these functions clean-up the conversion between requests and the
; cells used inside Loco
;

(defn- remove-unassigned
  "remove any cells that aren't assigned to one of our requestor-ids

   they will ahve a value of zero"

  [cells]

  (into {} (remove (fn [[_ c]] (zero? c)) cells)))



(defn clean-up-requests
      "remove the cells that were originally in the grid so
       we are only dealing with the 'new' stuff"

  [grid-cells requests-cells]
  (let [rfg (cells-from-grid grid-cells)]
    (if (or (empty? rfg) (empty? requests-cells))
      requests-cells

      (into {}
            (apply dissoc requests-cells (keys rfg))))))



(defn- make-request
  "turns a 'solution' back into a REQUEST, using the 'reverse'
  id-mapping.

  NOTE: this function returns a COMPRESSED request,
  not the fully expanded version provided by (expand-request...)"

  [id-map s]

  (->>
    ; {[ch ts] #{:id}} -> {:id #{[ch ts]}}
    (for [[[_ ch ts] x] s]
      {(get id-map x) #{[ch ts]}})

    ; merge the maps, and the value sets (union)
    (apply merge-with clojure.set/union)

    ; filter out the "{:_ 0}" cases Loco added
    (filter #(not (= :_ (key %))))

    ; back into a map
    (into {})))


;
; these are the functions that do all the work
;

(defn- build-flex-constraints
  "build the set of constraints for requests that have flexible channel
   needs"

  [cs ts req-id id-map]
  (apply merge
         (for [_ cs]
           (apply
             $or
             (for [c cs]
               (apply
                 $and
                 (for [r cs]
                   (if (= c r)
                     ($= [:cell c ts] (get id-map req-id))
                     ($!= [:cell r ts] (get id-map req-id))))))))))



(defn- build-fixed-constraints
  "build the constraint for one fixed channel/time-slot request"

  [cs ts req-id id-map]
  ($= [:cell cs ts] (get id-map req-id)))



(defn- build-default-constraints
  "build the 'domain' constraints to include all the overlapping
   requests, converts the keywords into the integers loco needs"

  [id-map requests]
  (let [ex (expand-flexible-requests requests)]
       (for [id (keys ex)]
         (let [rq (first (get ex id))
               ch (:channel rq)
               ts (:timeslot rq)
               req-ids (:allocated-to rq)]
           ($in [:cell ch ts]
                (into []
                      (flatten [0
                                (for [x req-ids]
                                  (get id-map x))])))))))



(defn- build-all-constraints
  "develop the complete set of constraints necessary to describe the
   request problem

   one side problem - we need to convert our requestor-ids into
   integers for loco using the id-map passed in"

  [id-map requests]

  (flatten
    (list
      ;  build the constraints for the flexible and fixed requests
      (for [r (for [req-id (keys requests)
                    rq (get requests req-id)]
                (let [ch (:channel rq)
                      ts (:timeslot rq)]
                  (if (coll? ch)
                    (build-flex-constraints ch ts req-id id-map)
                    (build-fixed-constraints ch ts req-id id-map))))]
        r)

      ; now build the default constraints
      (build-default-constraints id-map requests))))







;;;;;;;;;;;;;;;;;;;;;
;
; PUBLIC

(defn generate-acceptable-requests
      "take a set of requests with possible flexible needs and
       return a set of requests where those needs are locked
       down so that all the requests can work"

  [grid requests]
  (let [all-reqs (merge-with clojure.set/union
                             (expand-request requests)
                             (reqs-from-grid grid))
        ids      (id-map all-reqs)]

    (->>
      ; take the requests
      all-reqs

      ; build all the constraints
      (build-all-constraints ids)

      ; solve the constraints
      solution

      ; remove any cells that aren't assigned (value of 0)
      remove-unassigned

      ; pull out all the slots that came from the original grid
      (clean-up-requests grid)

      ; turn the solution back into a REQUEST
      (make-request ids))))




; TESTS

; REQUEST
(def requests-0 {"b" #{[1 1]}
                 "a" #{[1 1] [1 2]}})

(def requests-1 {"b" #{[0 0] [[0 1 2 3] 1]}
                 "a" #{[1 1] [1 2]}
                 "c" #{[3 3] [3 4] [4 4]}})

(def requests-2 {"b" #{[0 0] [[0 1 2 3] 1]}
                 "a" #{[1 1] [1 2]}
                 "c" #{[[2 3] 1] [3 3] [3 4] [4 4]}})

(def requests-3 {"b" #{[0 0] [[0 1 2 3] 1]}
                 "a" #{[1 1] [1 2]}
                 "c" #{[[2 3] 1] [3 3] [[3 4] 4]}})

(def requests-4 {"b" #{[0 0] [[0 1 2 3] 1]}
                 "a" #{[1 1] [1 2] [[3 4] 4]}
                 "c" #{[[2 3] 1] [3 3] [[3 4] 4]}})

(def requests-5 {"b" #{[0 0] [[0 1 2 3] 1]}
                 "a" #{[1 1] [1 2] [[3 4] 4]}
                 "q" #{[2 2]}
                 "c" #{[[2 3] 1] [3 3] [[3 4] 4]}})

(def requests-6 {"b" #{[0 0] [[0 1 2 3] 1]}
                 "a" #{[1 1] [1 2] [[3 4] 4]}
                 "q" #{[[2 3] 2]}
                 "c" #{[[2 3] 1] [3 3] [[3 4] 4]}})

; GRID
(def used-grid {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
                "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
                "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}})

(def used-grid-2 {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
                  "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
                  "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}
                  "2-2" {:channel 2 :timeslot 2 :allocated-to #{"m"}}})



(generate-acceptable-requests sg/empty-grid requests-0)
(generate-acceptable-requests used-grid requests-0)
(generate-acceptable-requests used-grid-2 requests-0)
; => {}



(generate-acceptable-requests sg/empty-grid requests-1)
(generate-acceptable-requests used-grid requests-1)
(generate-acceptable-requests used-grid-2 requests-1)
  ;{"a" #{[1 1] [1 2]},
  ; "b" #{[0 0] [0 1]},
  ; "c" #{[3 3] [3 4] [4 4]}}



(generate-acceptable-requests sg/empty-grid requests-2)
(generate-acceptable-requests used-grid requests-2)
(generate-acceptable-requests used-grid-2 requests-2)
  ;{"b" #{[0 0] [2 1]},
  ; "a" #{[1 1] [1 2]},
  ; "c" #{[3 3] [3 4] [3 1] [4 4]}}



(generate-acceptable-requests sg/empty-grid requests-3)
(generate-acceptable-requests used-grid requests-3)
(generate-acceptable-requests used-grid-2 requests-3)
  ;{"b" #{[0 0] [2 1]},
  ; "a" #{[1 1] [1 2]},
  ; "c" #{[3 3] [3 1] [4 4]}}



(generate-acceptable-requests sg/empty-grid requests-4)
(generate-acceptable-requests used-grid requests-4)
(generate-acceptable-requests used-grid-2 requests-4)
  ;{"b" #{[0 0] [2 1]},
  ; "a" #{[1 1] [3 4] [1 2]},
  ; "c" #{[3 3] [3 1] [4 4]}}



(generate-acceptable-requests sg/empty-grid requests-5)
(generate-acceptable-requests used-grid requests-5)
  ;{"b" #{[0 0] [2 1]},
  ; "a" #{[1 1] [3 4] [1 2]},
  ; "c" #{[3 3] [3 1] [4 4]},
  ; "q" #{[2 2]}}


(generate-acceptable-requests used-grid-2 requests-5)
  ;{} <- :m is already using [2 2], so :q can't have it



(generate-acceptable-requests sg/empty-grid requests-6)
(generate-acceptable-requests used-grid requests-6)
  ;{"b" #{[0 0] [2 1]},
  ; "a" #{[1 1] [3 4] [1 2]},
  ; "c" #{[3 3] [3 1] [4 4]},
  ; "q" #{[2 2]}}


(generate-acceptable-requests used-grid-2 requests-6)
  ;{"q" #{[3 2]},                <- :m already has [2 2]
  ; "b" #{[0 0] [2 1]},
  ; "a" #{[1 1] [3 4] [1 2]},
  ; "c" #{[3 3] [3 1] [4 4]}}





; repl scratchpad
(comment

  (def request-sim {"a" #{[0 0] [1 1]}})
  (def request-sim-2 {"b" #{[0 1] [1 0]} "a" #{[0 0] [1 1]}})

  (def request-ex {"a" #{{:channel 0 :timeslot 0} {:channel 1 :timeslot 1}}})
  (def request-ex-2 (expand-request request-sim-2))

  (def flex-request {"a" #{[[0 1] 1]}})
  (def flex-request-ex (expand-request flex-request))

  (def flex-request-2 {"a" #{[[0 1] 1]} "b" #{[[0 1] 0]}})
  (def flex-request-ex-2 (expand-request flex-request-2))

  (def flex-request-3 {"a" #{[[0 1] 1]}
                       "b" #{[[0 1] 0]}
                       "c" #{[[0 1] 1]}})
  (def flex-request-ex-3 (expand-request flex-request-3))

  (def flex-request-4 {"q" #{[[0 1] 3]}
                       "r" #{[[0 1 3] 0]}
                       "s" #{[[0 1 75] 54]}})
  (def flex-request-ex-4 (expand-request flex-request-4))

  (def basic-grid {"0-0" {:channel 0, :timeslot 0, :allocated-to #{"b"}},
                   "1-2" {:channel 1, :timeslot 2, :allocated-to #{"a"}},
                   "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}},
                   "3-3" {:channel 3, :timeslot 3, :allocated-to #{"c"}},
                   "3-4" {:channel 3, :timeslot 4, :allocated-to #{"c"}},
                   "4-4" {:channel 4, :timeslot 4, :allocated-to #{"c"}}})

  (def matching-grid {"0-0" {:channel 0, :timeslot 0, :allocated-to #{"b"}},
                      "1-0" {:channel 1, :timeslot 0, :allocated-to #{"b"}},
                      "1-1" {:channel 1, :timeslot 1, :allocated-to #{"a"}},
                      "2-1" {:channel 2, :timeslot 1, :allocated-to #{"a"}},
                      "2-2" {:channel 2, :timeslot 2, :allocated-to #{"m"}},
                      "0-3" {:channel 0, :timeslot 3, :allocated-to #{"q"}}
                      "0-4" {:channel 0, :timeslot 4, :allocated-to #{"q"}}
                      "1-3" {:channel 1, :timeslot 3, :allocated-to #{"q"}}})


  (defn- diff [g1 g2]
    (clojure.set/difference (set (keys g1)) (set (keys g2))))

  (expand-slot [0 0])
  (into #{} (map #(expand-slot %) #{[0 0] [1 1]}))
  (let [[id slots] (first (seq request-sim))]
    id)


  (id-map request-ex-2)

  (reqs-from-grid basic-grid)

  (expand-flexible-requests flex-request-ex)
  (expand-flexible-requests flex-request-ex-2)
  (expand-flexible-requests flex-request-ex-3)
  (expand-flexible-requests all-reqs)

  (reqs-from-grid basic-grid)

  (cells-from-grid basic-grid)

  (def all-reqs (merge-with clojure.set/union
                            flex-request-ex-4
                            (reqs-from-grid matching-grid)))
  (def i-m (id-map all-reqs))

  (build-flex-constraints [0 1 5] 0 i-m "a")
  (build-fixed-constraints 0 0 "q" i-m)
  (build-default-constraints i-m)
  (build-default-constraints i-m all-reqs)
  (build-default-constraints i-match match-reqs)
  (build-all-constraints (id-map all-reqs) all-reqs)
  (build-all-constraints (id-map match-reqs) match-reqs)

  (def match {"0-0" {:channel 0, :timeslot 0, :allocated-to #{"b"}},
              "1-0" {:channel 1, :timeslot 0, :allocated-to #{"b"}}})

  (def request-match {"q" #{[[0 2] 0]}})

  (def match-reqs (merge-with clojure.set/union
                              (expand-request request-match)
                              (reqs-from-grid match)))
  (def i-match (id-map match-reqs))

  (expand-flexible-requests match-reqs)
  (build-default-constraints i-match match-reqs)
  (build-all-constraints i-match match-reqs)


  (generate-acceptable-requests match match-reqs)
  (generate-acceptable-requests basic-grid flex-request-ex-4)


  (clojure.set/union
    (:allocated-to {:channel 0, :timeslot 1, :allocated-to #{"c"}})
    (:allocated-to {:channel 0, :timeslot 1, :allocated-to #{"a"}}))

  (merge-with req-comb
              {"0-1" #{{:channel 0, :timeslot 1, :allocated-to #{"c"}}},
               "1-1" #{{:channel 0, :timeslot 1, :allocated-to #{"c"}}}}

              {"0-1" #{{:channel 0, :timeslot 1, :allocated-to #{"a"}}},
               "1-1" #{{:channel 0, :timeslot 1, :allocated-to #{"a"}}}})


  (apply merge-with req-comb
         '({"0-1" #{{:channel 0, :timeslot 1, :allocated-to #{"c"}}},
            "1-1" #{{:channel 0, :timeslot 1, :allocated-to #{"c"}}}}

           {"0-1" #{{:channel 0, :timeslot 1, :allocated-to #{"a"}}},
            "1-1" #{{:channel 0, :timeslot 1, :allocated-to #{"a"}}}}))

  (apply merge-with req-comb
         '({"0-1" #{{:channel 0, :timeslot 1, :allocated-to #{"a"}}}}
           {"1-1" #{{:channel 1, :timeslot 1, :allocated-to #{"a"}}}}
           {"0-0" #{{:channel 0, :timeslot 0, :allocated-to #{"b"}}}}
           {"1-0" #{{:channel 1, :timeslot 0, :allocated-to #{"b"}}}}
           {"0-1" #{{:channel 0, :timeslot 1, :allocated-to #{"c"}}}}
           {"1-1" #{{:channel 1, :timeslot 1, :allocated-to #{"c"}}}}))



  ())
