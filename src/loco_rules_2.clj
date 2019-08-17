(ns loco-rules-2
  (:require [loco.core :refer :all]
            [loco.constraints :refer :all]))


; NOTE: the hardest part of logic programming is being sure your rules
;       produce a correct answer
;
;       the second hardest part is getting the rules correct to actually
;       describe your problem
;
;       the third hardest part is figuring out if an empty answer is because
;       you did the constraints wrong, or there really is no answer
;


{:namespace      "loco-rules-2"
 :public-api     ["generate-acceptable-requests"]
 :effective-sloc 100}


;;;;;;;;;;;;;;;;;;;;;
;
; PROBLEM
;
; develop a simple (really simple) model and algorithm for determining a workable
; resource "demand" set given a set of request with a combination of "fixed" and
; "flexible" needs
;
; return a REQUEST (see allocation_try_2.clj) data structure with any flexible
;   channels replaced by fixed channels such that all the requests will work
;
;
;
; This one requires some explanation. Some familiarity with allocation-try-2 is required
;
;
;
; Assume a simple 2x2 GRID and two requestors :a and :b
;
;   :a needs cells [0 0] and [1 1]
;
;   :b needs 1 channel at time-slot 0, but it can be either channel 0 or channel 1
;
; We can express this in a REQUEST like this:
;
;      {:a #{[  0   0]   [1 1]}
;       :b #{[[0 1] 0]        }}
;
; :a need 2 channels, and has no flexibility in them. it needs [0 0] AND [1 1].  we refer to
; these as "fixed requests"
;
; Notice how the channel for :b is expressed as a vector of the channels that :b
; could use, as long as it gets one of them. In essence, :b needs [0 0] OR [1 0],
; but not both. we refer to this as a "flexible request"
;
; We can also see from inspection that there is exactly 1 possible solution: :b can
; only have channel 1 at time-slot 0, since :a can ONLY work with [0 0]
;
;      {:a #{[0 0] [1 1]}
;       :b #{[1 0]      }}
;
; For something so simple, it's easy to figure out a workable answer, but when things
; get even a little bit more complicated... we need to call in the big guns:
;
;                   Logic Programming
;
; Specifically the subset called "constraint solvers." By formulating a set of constraints
; that describe the problem and "constrain" the possible solutions, we can have the
; computer find us a solution
;



;;;;;;;;;;;;;;;;;;;;;
;
; SOLUTION
;
; 0) leverage the 'loco' constraint solver Clojure library
;
;
;
; 1) pragmatically produce the necessary constraints to describe the
;    problem
;
; 2) run the constraints through the solver and grab the solution
;
; 3) convert the solution back into a REQUEST data structure and
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
; generate-acceptable-requests  - takes a REQUEST containing flexible channel
;          needs and returns a REQUEST where the channels are fixed such that
;          the entire set will work. internally it uses a "constraint solver"
;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;
;
; NS PRIVATE

;
; these are the helper functions
;

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

(defn- expand-flexible-requests
  "expand the collection of 'flexible' request so each is a separate item.
   we will use this to build all the 'default' $in constraints"

  [requests]
  (apply merge-with clojure.set/union
         (for [[req-id reqs] requests
               [cs ts] reqs]
           (if (coll? cs)
             (apply conj
                    (for [c cs]
                      {[c ts] #{req-id}}))
             {[cs ts] #{req-id}}))))

(defn reqs-from-grid [grid]
  (apply merge-with
         clojure.set/union
         (remove nil?
                 (flatten
                   (for [ch (range (count (first grid)))
                         ts (range (count grid))]
                     (if (empty? (get-in grid [ch ts]))
                       ()
                       {(first (get-in grid [ch ts])) #{[ch ts]}}))))))


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
                     ($= [:cell c ts] (req-id id-map))
                     ($!= [:cell r ts] (req-id id-map))))))))))

(defn- build-fixed-constraints
  "build the constraint for one fixed channel/time-slot request"

  [cs ts req-id id-map]
  ($= [:cell cs ts] (req-id id-map)))

(defn- build-default-constraints
  "build the 'domain' constraints to include all the overlapping
   requests, converts the keywords into the integers loco needs"

  [id-map requests]
  (for [[[ch ts] r] (expand-flexible-requests requests)]
    ($in [:cell ch ts]
         (into []
               (flatten [0
                         (for [x r]
                           (x id-map))])))))

(defn- build-all-constraints
  "develop the complete set of constraints necessary to describe the
   request problem

   one side problem - we need to convert our keyword requestor-ids into
   integers for loco"

  [requests]
  (let [id-map (id-map requests)]
    (flatten
      (list
        ;  build the constraints for the flexible and fixed requests
        (for [r (for [[req-id reqs] requests
                      [cs ts] reqs]
                  (if (coll? cs)
                    (build-flex-constraints cs ts req-id id-map)
                    (build-fixed-constraints cs ts req-id id-map)))]
          r)

        ; now build the default constraints
        (build-default-constraints id-map requests)))))

(defn- make-request
  "turns a 'solution' back into a REQUEST, using the 'reverse' id-mapping"

  [id-map s]
  ; make sure we return a map (this is a Clojure thing)

  ;(->> (for [[[_ ch ts] x] s]
  ;       {(get id-map x) #{[ch ts]}})
  ;
  ;     apply merge-with clojure.set/union
  ;
  ;     #(filter #(not (= :_ (key %))))
  ;
  ;     into {}))


  (into {}
        ; filter out the "empty requests" loco added
        (filter #(not (= :_ (key %)))
                ; merge all the little parts into 1 map
                (apply merge-with clojure.set/union
                       ; convert the format to be a REQUEST
                       (for [[[_ ch ts] x] s]
                         {(get id-map x) #{[ch ts]}})))))



;;;;;;;;;;;;;;;;;;;;;
;
; PUBLIC

(defn generate-acceptable-requests
      "take a set of requests with possible flexible needs and
       return a set of requests where those needs are locked
       down so that all the requests can work"

  [requests]

  (->>
    ; take the requests
    requests

    ; build all the constraints
    build-all-constraints

    ; solve the constraints
    solution

    ; turn the solution back into a set of individual REQUESTs
    (make-request (flipped-id-map requests))))





; TESTS
;

(def requests-0 {:b #{[1 1]}
                 :a #{[1 1] [1 2]}})

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


(generate-acceptable-requests requests-0)
; => {}


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




(def grid [[#{:a} #{:a}]
           [#{} #{:b}]])

(def grid-reqs {:a #{[0 0] [0 1]} :b #{[1 1]}})

(def ch (count (first grid)))
(def ts (count grid))

(range ch)


(first (get-in grid [0 0]))
(first (get-in grid [1 1]))

(remove nil?
       (flatten (for [ch (range (count (first grid)))
                      ts (range (count grid))]
                  [(first (get-in grid [ch ts]))])))

(merge '({:a #{[0 0]}} {:b #{[1 1]}}))
(apply merge '({:a #{[0 0]}} {:b #{[1 1]}}))

(apply merge-with clojure.set/union '({:a #{[0 0]}} {:a #{[0 1]}}))

(= {:a #{[0 0] [0 1]}, :b #{[1 1]}}
  {:a #{[0 0] [0 1]} :b #{[1 1]}})




(= grid-reqs
  (apply merge-with clojure.set/union
    (remove nil?
      (flatten (for [ch (range (count (first grid)))
                     ts (range (count grid))]
                 (if (empty? (get-in grid [ch ts]))
                   ()
                   {(first (get-in grid [ch ts])) #{[ch ts]}}))))))



(= grid-reqs (reqs-from-grid grid))


; "long hand"

{:b #{[0 0] [[0 1 2 3] 1]}
 :a #{[1 1] [1 2]}
 :c #{[3 3] [3 4] [4 4]}}

; loco only works with integers, so we need to change our
; requestor IDs into numbers...

(let [fixed    [($= [:cell 0 0] 1)                          ; :b = 1
                ($= [:cell 1 1] 2)                          ; :a = 2
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)                          ; :c = 3
                ($= [:cell 3 4] 3)
                ($= [:cell 4 4] 3)]

      flex     [($or                                        ; any one of the following

                  ($and ($= [:cell 0 1] 1)                  ; [0 1] but not [1 1], [2 1] or [3 1]
                        ($!= [:cell 1 1] 1)
                        ($!= [:cell 2 1] 1)
                        ($!= [:cell 3 1] 1))

                  ($and ($!= [:cell 0 1] 1)                 ; [1 1] but not [0 1], [2 1] or [3 1]
                        ($= [:cell 1 1] 1)
                        ($!= [:cell 2 1] 1)
                        ($!= [:cell 3 1] 1))

                  ($and ($!= [:cell 0 1] 1)                 ; [2 1] but not [0 1], [1 1] or [3 1]
                        ($!= [:cell 1 1] 1)
                        ($= [:cell 2 1] 1)
                        ($!= [:cell 3 1] 1))

                  ($and ($!= [:cell 0 1] 1)                 ; [3 1] but not [0 1], [1 1] or [2 1]
                        ($!= [:cell 1 1] 1)
                        ($!= [:cell 2 1] 1)
                        ($= [:cell 3 1] 1)))]

      ; loco requires all $= and $!= constraints have a matching $in constraint
      ;
      ; we use 0 to mean 'no one gets this cell'
      ;
      defaults [($in [:cell 0 0] [0 1])                     ; only :b wants this
                ($in [:cell 0 1] [0 1])                     ; part of :b's flexible request
                ($in [:cell 1 1] [0 1 2])                   ; :a AND :b want this
                ($in [:cell 1 2] [0 2])                     ; only :a wants this
                ($in [:cell 2 1] [0 1])                     ; part of :b's flexible request
                ($in [:cell 3 1] [0 1])                     ; part of :b's flexible request
                ($in [:cell 3 3] [0 3])                     ; only :c wants this
                ($in [:cell 3 4] [0 3])                     ; only :c wants this
                ($in [:cell 4 4] [0 3])]]                   ; only :c wants this
  (into (sorted-map)
        (solutions (concat defaults
                           flex fixed)
                   :timeout 1000)))


;
; what if :b and :c are flexible, and overlap in their ranges?
;

{:b #{[0 0] [[0 1 2 3] 1]}
 :a #{[1 1] [1 2]}
 :c #{[[2 3] 1] [3 3] [3 4] [4 4]}}

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)
                ($= [:cell 3 4] 3)
                ($= [:cell 4 4] 3)]

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
                           ($= [:cell 3 1] 3)))]

      defaults [($in [:cell 0 0] [0 1])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 1 1] [0 2])
                ($in [:cell 1 2] [0 1 2])
                ($in [:cell 2 1] [0 1 3])
                ($in [:cell 3 1] [0 1 3])
                ($in [:cell 3 3] [0 3])
                ($in [:cell 3 4] [0 3])
                ($in [:cell 4 4] [0 3])]]

  (into (sorted-map)
        (solutions (concat defaults flex fixed)
                   :timeout 1000)))

;
; what if :c adds a 2nd request that is flexible?
;

{:b #{[0 0] [[0 1 2 3] 1]}
 :a #{[1 1] [1 2]}
 :c #{[[2 3] 1] [3 3] [[3 4] 4]}}

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)]

      flex     [; :b [0 1] or [1 1] 0r [2 1] or [3 1]
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
                           ($= [:cell 4 4] 3)))]

      defaults [($in [:cell 0 0] [0 1])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 1 1] [0 1 2])
                ($in [:cell 1 2] [0 1 2])
                ($in [:cell 2 1] [0 1 3])
                ($in [:cell 3 1] [0 1 3])
                ($in [:cell 3 3] [0 3])
                ($in [:cell 3 4] [0 3])
                ($in [:cell 4 4] [0 3])]]

  (into (sorted-map)
        (solutions (concat defaults flex fixed)
                   :timeout 1000)))



;
; what if :a adds a flexible request that overlaps :c?
;

{:b #{[0 0] [[0 1 2 3] 1]}
 :a #{[1 1] [1 2] [[3 4] 4]}
 :c #{[[2 3] 1] [3 3] [[3 4] 4]}}

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)
                ($= [:cell 3 3] 3)]

      flex     [; :b [0 1] or [1 1] 0r [2 1] or [3 1]
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
                           ($= [:cell 4 4] 2)))]

      defaults [($in [:cell 0 0] [0 1])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 1 1] [0 1 2])
                ($in [:cell 1 2] [0 1 2])
                ($in [:cell 2 1] [0 1 3])
                ($in [:cell 3 1] [0 1 3])
                ($in [:cell 3 3] [0 2 3])
                ($in [:cell 3 4] [0 2 3])
                ($in [:cell 4 4] [0 2 3])]]

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
; logically, this is a situation like:
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
; also need ot look at situation where the channel is fixed and the
; time-slots are grouped:
;
;     {:a #{[0 #{1 2}}}
;
; technically, this is the effectively {:a #{[0 1] [0 2}} but more
; compact
;
;
;
; then we can stretch to:
;
;     {:a #{[#{0 1} #{1 2}}} -> {:a #{[0 1] [0 2] [1 1] [1 2]}}
;
; and maybe even:
;
;     {:a #{[#{0 1} [1 2]}} -> {:a #{[0 1] [0 2]} OR {:a #{[1 1] [1 2]}}
;




; just to make out example a bit smaller, only use :b and :a
;
;     this is still pretty hard to get right 'long-hand'
;
(def requests-5 {:b #{[0 0] [[0 1 2 3] #{1 2}]}
                 :a #{[1 1] [1 2]}})

(let [fixed    [($= [:cell 0 0] 1)
                ($= [:cell 1 1] 2)
                ($= [:cell 1 2] 2)]

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
                           ($= [:cell 3 2] 1)))]

      defaults [($in [:cell 0 0] [0 1])
                ($in [:cell 0 1] [0 1])
                ($in [:cell 0 2] [0 1])
                ($in [:cell 1 1] [0 1 2])
                ($in [:cell 1 2] [0 1 2])
                ($in [:cell 2 1] [0 1])
                ($in [:cell 2 2] [0 1])
                ($in [:cell 3 1] [0 1])
                ($in [:cell 3 2] [0 1])]]
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


