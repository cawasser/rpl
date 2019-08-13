(ns loco-rules-2
  (:require [loco.core :refer :all]
            [loco.constraints :refer :all]))


; NOTE: the hardest part of logic programming is being sure your rules
;       produce a correct answer
;
;       the second hardest part is getting the rules correct to actually
;       describe your problem


{:namespace "loco-rules-2"
 :public-api ["generate-acceptable-requests"]
 :effective-sloc 90}


;;;;;;;;;;;;;;;;;;;;;
;
; PROBLEM
;
; develop a simple (really simple) model and algorithm for determining a workable
; resource "demand" set given a set of request with a combination of fixed and
; "flexible" needs
;
; return a REQUEST (see allocation_try_2.clj) data structure with any flexible
;   channels replaced by fixed channels such that all the requests will work
;
;



;;;;;;;;;;;;;;;;;;;;;
;
; SOLUTION
;
; 1) leverage the 'loco' constraint solver Clojure library
;
; 2) develop mechanisms for pragmatically producing the
;    necessary constraints
;
; 3) run the constrains through the colver and grab the first solution
;
; 4) convert the solution back into the REQUEST data structure and
;    return it
;



; TODO: account for existing allocations (using a GRID)
;
; TODO: requestor needs a flexible channel at fixed times
;



;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;
;
; DATA STRUCTURES
;
; REQUESTS - see allocation-try-2 (allocation_try_2.clj)
;
;;;;;;;;;;;;;;;;;;;;
;
; PUBLIC API
;
; generate-acceptable-plan  - takes a REQUEST containing flexible channel
;          needs and returns a REQUEST where the channels are fixed such that
;          the entire set will work. internally it uses a "constraint solver"
;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;



(defn- build-defaults
  "build the 'default' set of constraints for cells that have multiple
   requestors 'needing' them"

  [requests]
  (apply merge-with clojure.set/union
         (for [[req-id reqs] requests
               [cs ts] reqs]
           (if (coll? cs)
             (apply conj
                    (for [c cs]
                      {[c ts] #{req-id}}))
             {[cs ts] #{req-id}}))))

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
                     ($= [:cell c ts] (req-id id-map))
                     ($!= [:cell r ts] (req-id id-map))))))))))

(defn- build-fixed-constraints
  "build the constraint for a fixed cahnnel/time-slot request"

  [cs ts req-id id-map]
  ($= [:cell cs ts] (req-id id-map)))

(defn- id-map [requests]
  "build a map to convert keywords, used by REQUESTS, into integers,
   which are required by loco"

  (merge {:_ 0}
         (zipmap (keys requests)
                 (iterate inc 1))))

(defn- flipped-id-map [requests]
  "build a map to convert integers, which are required by loco into
   keywords, used by REQUESTS"

  (merge {0 :_}
         (zipmap (iterate inc 1)
                 (keys requests))))

(defn- req-grid-2 [requests]
  "develop the complete set of constraints necessary to describe the
   request problem"

  (let [id-map (id-map requests)]
    (flatten
      (list
        (for [[[ch ts] r]  (build-defaults requests)]
          ($in [:cell ch ts] (into []
                                   (flatten [0
                                             (for [x r]
                                               (x id-map))]))))
        (for [x (for [[req-id reqs] requests
                      [cs ts] reqs]
                  (if (coll? cs)
                    (build-flex-constraints cs ts req-id id-map)
                    (build-fixed-constraints cs ts req-id id-map)))]
          x)))))


;;;;;;;;

(defn generate-acceptable-requests
      "take a set of requests with possible flexible needs and
       return a set of requests  where those slots are locked
       down so that all the requests can work"

  [requests]

  (into {}
        (filter
          #(not (= :_ (key %)))
          (apply merge-with clojure.set/union
                 (let [id-map (flipped-id-map requests)]
                   (for [[[_ ch ts] x] (into (sorted-map)
                                             (solution (req-grid-2
                                                         requests)))]
                     {(get id-map x) #{[ch ts]}}))))))



; TESTS
;

(def requests-1 {:b #{[0 0] [[0 1 2 3] 1]}
                 :a #{[1 1] [1 2]}
                 :c #{[3 3] [3 4] [4 4]}})
(def requests-2 {:b #{[0 0] [[0 1 2 3] 1]}
                 :a #{[1 1] [1 2]}
                 :c #{[[2 3] 1] [3 3] [3 4] [4 4]}})
(def requests-3 {:b #{[0 0] [[0 1 2 3] 1]}
                 :a #{[1 1] [1 2]}
                 :c #{[[2 3] 1] [3 3] [[3 4] 4]}})
(def requests-4 {:b #{[0 0] [[0 1 2 3] 1]}
                 :a #{[1 1] [1 2] [[3 4] 4]}
                 :c #{[[2 3] 1] [3 3] [[3 4] 4]}})




(into (sorted-map) (solution (req-grid-2 requests-1)))
(into (sorted-map) (solution (req-grid-2 requests-2)))
(into (sorted-map) (solution (req-grid-2 requests-3)))
(into (sorted-map) (solution (req-grid-2 requests-4)))




(generate-acceptable-requests requests-1)
;=> {:b #{[0 0] [0 1]},
;    :a #{[1 1] [1 2]},
;    :c #{[3 3] [3 4] [4 4]}}

(generate-acceptable-requests requests-2)
;=> {:b #{[0 0] [2 1]},
;    :a #{[1 1] [1 2]},
;    :c #{[3 3] [3 4] [3 1] [4 4]}}


(generate-acceptable-requests requests-3)
;=> {:b #{[0 0] [2 1]},
;    :a #{[1 1] [1 2]},
;    :c #{[3 3] [3 1] [4 4]}}


(generate-acceptable-requests requests-4)
;=> {:b #{[0 0] [2 1]},
;    :a #{[1 1] [3 4] [1 2]},
;    :c #{[3 3] [3 1] [4 4]}}










; we need some kind of constraint that makes [[0 1 2] 1] pick only
; ONE slot rather than multiples, which is what it looks like is
; happening

;
; would $or work?
;
; combining $or with disjoint $and's seems to work
; in a very simple case
;


(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)
                ($= [:cell 4 4] 3)]

      defaults [($in [:cell 0 0] [0 1 2 3])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 1 1] [0 1 2 3])
                ($in [:cell 1 2] [0 1 2 3])
                ($in [:cell 2 1] [0 1])
                ($in [:cell 3 1] [0 1])
                ($in [:cell 3 3] [0 1 2 3])
                ($in [:cell 3 4] [0 1 2 3])
                ($in [:cell 4 4] [0 1 2 3])]

      flex     [($or ($and ($= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($= [:cell 3 1] 1)))]]

  (into (sorted-map)
        (solutions (concat defaults
                           flex fixed)
                   :timeout 1000)))


;
; what if :b and :c are flexible, and overlap in their ranges?
;

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)
                ($= [:cell 4 4] 3)]

      defaults [($in [:cell 0 0] [0 1 2 3])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 1 1] [0 1 2 3])
                ($in [:cell 1 2] [0 1 2 3])
                ($in [:cell 2 1] [0 1 3])
                ($in [:cell 3 1] [0 1 3])
                ($in [:cell 3 3] [0 1 2 3])
                ($in [:cell 3 4] [0 1 2 3])
                ($in [:cell 4 4] [0 1 2 3])]

      flex     [($or ($and ($= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($= [:cell 3 1] 1)))
                ($or ($and ($= [:cell 2 1] 3)
                           ($!= [:cell 3 1] 3))
                     ($and ($!= [:cell 2 1] 3)
                           ($= [:cell 3 1] 3)))]]

  (into (sorted-map)
        (solutions (concat defaults flex fixed)
                   :timeout 1000)))

;
; what if :c adds a 2nd request that is flexible?
;

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)]

      defaults [($in [:cell 0 0] [0 1 2 3])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 1 1] [0 1 2 3])
                ($in [:cell 1 2] [0 1 2 3])
                ($in [:cell 2 1] [0 1 3])
                ($in [:cell 3 1] [0 1 3])
                ($in [:cell 3 3] [0 1 2 3])
                ($in [:cell 3 4] [0 3])
                ($in [:cell 4 4] [0 3])]

      flex     [ ; :b [0 1] or [1 1] 0r [2 1] or [3 1]
                ($or ($and ($= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($= [:cell 3 1] 1)))

                ; :c [2 1] or [3 1]
                ($or ($and ($= [:cell 2 1] 3)
                           ($!= [:cell 3 1] 3))
                     ($and ($!= [:cell 2 1] 3)
                           ($= [:cell 3 1] 3)))

                ; :c [3 4] or [4 4]
                ($or ($and ($= [:cell 3 4] 3)
                           ($!= [:cell 4 4] 3))
                     ($and ($!= [:cell 3 4] 3)
                           ($= [:cell 4 4] 3)))]]

  (into (sorted-map)
        (solutions (concat defaults flex fixed)
                   :timeout 1000)))



;
; what if :a adds a flexible request that overlaps :c?
;

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)]

      defaults [($in [:cell 0 0] [0 1 2 3])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 1 1] [0 1 2 3])
                ($in [:cell 1 2] [0 1 2 3])
                ($in [:cell 2 1] [0 1 3])
                ($in [:cell 3 1] [0 1 3])
                ($in [:cell 3 3] [0 1 2 3])
                ($in [:cell 3 4] [0 2 3])
                ($in [:cell 4 4] [0 2 3])]

      flex     [ ; :b [0 1] or [1 1] 0r [2 1] or [3 1]
                ($or ($and ($= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($= [:cell 2 1] 1)
                           ($!= [:cell 3 1] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 2 1] 1)
                           ($= [:cell 3 1] 1)))

                ; :c [2 1] or [3 1]
                ($or ($and ($= [:cell 2 1] 3)
                           ($!= [:cell 3 1] 3))
                     ($and ($!= [:cell 2 1] 3)
                           ($= [:cell 3 1] 3)))

                ; :c [3 4] or [4 4]
                ($or ($and ($= [:cell 3 4] 3)
                           ($!= [:cell 4 4] 3))
                     ($and ($!= [:cell 3 4] 3)
                           ($= [:cell 4 4] 3)))

                ; :a [3 4] or [4 4]
                ($or ($and ($= [:cell 3 4] 2)
                           ($!= [:cell 4 4] 2))
                     ($and ($!= [:cell 3 4] 2)
                           ($= [:cell 4 4] 2)))]]
  (into (sorted-map)
        (solutions (concat defaults flex fixed)
                   :timeout 1000)))



;
; now we just need to work out how to build this programmatically
;



;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;
;
; Something for the future
;
; currently this approach can't describe a requests that needs a
; channel over a set of time-slots, but the requestor doesn't
; care which channel as long as it is the SAME ONE at each time
;
; logically, this is
;
;      ":a needs one channel out of 0, 1, OR 2 at all time-slots
;         0, 1, AND 2"
;
; perhaps this could be the data structure:
;
;     {:a #{[0 1 2] #{0 1 2}]}}
;
; where the [vector] defines the OR-set and the #{set} defines
; the AND-set
;
;
; (this is a more flexible approach if we can make it work for both the
;  channel and the time-slot)
;




; just to make it a bit smaller, only use :b and :a
;
;     this is still pretty hard to get right 'long-hand'
;
(def requests-5 {:b #{[0 0] [[0 1 2 3] #{1 2}]}
                 :a #{[1 1] [1 2]}})

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)]

      defaults [($in [:cell 0 0] [0 1])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 0 2] [0 1])
                ($in [:cell 1 1] [0 1 2])
                ($in [:cell 1 2] [0 1 2])
                ($in [:cell 2 1] [0 1])
                ($in [:cell 2 2] [0 1])
                ($in [:cell 3 1] [0 1])
                ($in [:cell 3 2] [0 1])]

      flex     [($or ($and ($= [:cell 0 1] 1)
                           ($= [:cell 0 2] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 1 2] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 2 2] 1)
                           ($!= [:cell 3 1] 1)
                           ($!= [:cell 3 2] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 0 2] 1)
                           ($= [:cell 1 1] 1)
                           ($= [:cell 1 2] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 2 2] 1)
                           ($!= [:cell 3 1] 1)
                           ($!= [:cell 3 2] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 0 2] 1)
                           ($= [:cell 2 1] 1)
                           ($= [:cell 2 2] 1)
                           ($!= [:cell 3 1] 1)
                           ($!= [:cell 3 2] 1))
                     ($and ($!= [:cell 0 1] 1)
                           ($!= [:cell 0 2] 1)
                           ($!= [:cell 1 1] 1)
                           ($!= [:cell 1 2] 1)
                           ($!= [:cell 2 1] 1)
                           ($!= [:cell 2 2] 1)
                           ($= [:cell 3 1] 1)
                           ($= [:cell 3 2] 1)))]]
  (into (sorted-map)
        (solutions (concat defaults
                           flex fixed)
                   :timeout 1000)))

;
;    {[:cell 0 0] 1,
;     [:cell 0 1] 0,
;     [:cell 0 2] 0,
;     [:cell 1 1] 2,
;     [:cell 1 2] 2,
;     [:cell 2 1] 0,
;     [:cell 2 2] 0,
;     [:cell 3 1] 1, <- in this case, loco picked channel 3
;     [:cell 3 2] 1}))
;
; could have picked [0 1] and [0 2] OR [2 1] and [2 2]
;


