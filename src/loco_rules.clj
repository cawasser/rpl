(ns loco-rules
  (:require [loco.core :refer :all]
            [loco.constraints :refer :all]))


;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; to get a handle on rule programming, we'll start by following an
; article from 2014 by Mark Engleberg
;
; https://programming-puzzler.blogspot.com/2014/03/appointment-scheduling-in-clojure-with.html?m=1
;
; Mark uses the Clojure library "loco" (https://github.com/aengelberg/loco) written
; by his father(?)
;
; I recommend you open the article and follow along in the REPL
;
;

;
; another place to learn:
;   https://www.minizinc.org/doc-2.3.1/en/part_2_tutorial.html
; although the examples aren't written in Clojure
;
; but this version is:
;   http://tanders.github.io/clojure2minizinc/tutorial.html#undefined
;


; this vector represents the acceptable time slots for 4 people
;
(def availability
  [[1 2 3 4]
   [2 3]
   [1 4]
   [1 4]])
  ; => [[1 2 3 4] [2 3] [1 4] [1 4]]



; notice how we can treat "variable" definitions as if we were defining
; functions?
;
; this is because Clojure "functional programming" uses referential
; integrity, which in practical terms, means we can substitute a
; symbol's value for it's name in all other code without changing any
; means
;
(def person-vars
  (for [i (range (count availability))] [:person i]))
  ; => ([:person 0] [:person 1] [:person 2] [:person 3])



; now we tie the two together into a loco "constraint"
;
(def availability-constraints
  (for [i (range (count availability))]
    ($in [:person i] (availability i))))
  ; => ({:type :int-domain, :can-init-var true, :name [:person 0], :domain [1 2 3 4]}
  ;     {:type :int-domain, :can-init-var true, :name [:person 1], :domain [2 3]}
  ;     {:type :int-domain, :can-init-var true, :name [:person 2], :domain [1 4]}
  ;     {:type :int-domain, :can-init-var true, :name [:person 3], :domain [1 4]})



; $all-different? makes sure each :person ends up in a different slot
;
(def all-different-constraint
  (apply $all-different? person-vars))
  ; => {:type :distinct, :args ([:person 0] [:person 1] [:person 2] [:person 3])}


; now we hook both constraints together
;
(def all-constraints
  (conj availability-constraints all-different-constraint))
  ; => ({:type :distinct, :args ([:person 0] [:person 1] [:person 2] [:person 3])}
  ;     {:type :int-domain, :can-init-var true, :name [:person 0], :domain [1 2 3 4]}
  ;     {:type :int-domain, :can-init-var true, :name [:person 1], :domain [2 3]}
  ;     {:type :int-domain, :can-init-var true, :name [:person 2], :domain [1 4]}
  ;     {:type :int-domain, :can-init-var true, :name [:person 3], :domain [1 4]})



; we're ready to solve
;
; sorting makes it easier to read
;
(into (sorted-map) (solution all-constraints))
  ; => {[:person 0] 2, [:person 1] 3, [:person 2] 1, [:person 3] 4}



; let's put it all together into a function that can solve ANY
; availability question
;
(defn schedule [availability]
  (->>
    (solution
      (conj
        (for [i (range (count availability))]
          ($in [:person i] (availability i)))
        ($distinct
          (for [i (range (count availability))] [:person i]))))
    (into (sorted-map))))

; we can solve this problem
;
(schedule
  [[1 3 5]
   [2 4 5]
   [1 3 4]
   [2 3 4]
   [3 4 5]])
  ; => {[:person 0] 1, [:person 1] 4,
  ;     [:person 2] 3, [:person 3] 2,
  ;     [:person 4] 5}


; but NOT this one
;
(schedule
  [[1 2 3 4]
   [1 4]
   [1 4]
   [1 4]])
  ; => {}

; there is no way to find a solution where each person is in a
; different slot given the availability
;


; Mark's second example

; we'll reuse a bunch of code form above, but we need to add a new
; concept
;
(def timeslots (distinct (apply concat availability)))
  ; => (1 2 3 4)

;  we need to keep track of how many people are in each slot
;
(def people-in-timeslot-vars
  (for [i timeslots] [:_num-people-in-timeslot i]))
  ; => ([:num-people-in-timeslot 1]
  ;     [:num-people-in-timeslot 2]
  ;     [:num-people-in-timeslot 3]
  ;     [:num-people-in-timeslot 4])



; so we can create a constraint on that number
;
(def conflict-constraints
  (for [i timeslots]
    ($in [:_num-people-in-timeslot i] 0 2)))
  ; => ({:type :int-domain, :can-init-var true, :name [:num-people-in-timeslot 1], :domain {:min 0, :max 2}}
  ;     {:type :int-domain, :can-init-var true, :name [:num-people-in-timeslot 2], :domain {:min 0, :max 2}}
  ;     {:type :int-domain, :can-init-var true, :name [:num-people-in-timeslot 3], :domain {:min 0, :max 2}}
  ;     {:type :int-domain, :can-init-var true, :name [:num-people-in-timeslot 4], :domain {:min 0, :max 2}})


; loco includes a cardinality constraint to bind each
; [:num-people-in-timeslot i] variable to the number of
; times i occurs among the variables [:person 1], [:person 2]
;
($cardinality [:x :y :z] {1 :number-of-ones})
  ; => {:type :cardinality, :variables [:x :y :z], :values (1), :occurrences (:number-of-ones), :closed nil}


; zipmap () is really cool, is takes 2 vectors and combines then as
; key/value pairs in a hash-map (evaluate each parameter in the repl)
;
(def number-in-timeslots
  ($cardinality person-vars
                (zipmap timeslots people-in-timeslot-vars)))

timeslots
  ; => (1 2 3 4)
people-in-timeslot-vars
  ; => ([:num-people-in-timeslot 1]
  ;     [:num-people-in-timeslot 2]
  ;     [:num-people-in-timeslot 3]
  ;     [:num-people-in-timeslot 4])
(zipmap timeslots people-in-timeslot-vars)
  ; => {1 [:num-people-in-timeslot 1],
  ;     2 [:num-people-in-timeslot 2],
  ;     3 [:num-people-in-timeslot 3],
  ;     4 [:num-people-in-timeslot 4]}


number-in-timeslots
  ; => {:type :cardinality,
  ;     :variables ([:person 0] [:person 1] [:person 2] [:person 3]),
  ;     :values (1 2 3 4),
  ;     :occurrences ([:num-people-in-timeslot 1]
  ;                   [:num-people-in-timeslot 2]
  ;                   [:num-people-in-timeslot 3]
  ;                   [:num-people-in-timeslot 4]),
  ;     :closed nil}



; I'll let Alex explain things from here
;
;    "To minimize the number of conflicts, we need to count the number
;     of conflicts.
;
;     Let the variable :number-of-conflicts stand for the number of
;     timeslot conflicts we have. We need two constraints on
;     :number-of-conflicts.
;
;     The first constraint just sets up the finite domain that the
;     variable could range over (i.e., 0 to the total number of
;     timeslots). We need to do this because in Loco, every variable
;     must be declared somewhere in the model. The second constraint
;     binds :number-of-conflicts to the number of times 2 appears in
;     the variables [:num-people-in-timeslot 1],
;     [:num-people-in-timeslot 2], etc."
;
(def number-of-conflicts
  [($in :number-of-conflicts 0 (count timeslots))
   ($cardinality people-in-timeslot-vars {2 :number-of-conflicts})])
; this just combines the 2 constraints into a single vector...



;    "We built the constraints in parts; now building the model is simply a
;     matter of concat-ing all the constraints together. (Note that
;     number-in-timeslots is a single constraint, so we concatenate
;     [number-in-timeslots] in with the other lists of constraints)."
;
(def all-constraints (concat availability-constraints
                             conflict-constraints
                             [number-in-timeslots]
                             number-of-conflicts))
;;;; IMPORTANT!
;;;;    notice that "number-in-timeslots" is inside a vector!



; now we can solve the model
;
(solution all-constraints :minimize :number-of-conflicts)
  ; => {:number-of-conflicts 0,
  ;     [:person 0] 3,
  ;     [:num-people-in-timeslot 1] 1,
  ;     [:person 1] 2,
  ;     [:person 3] 1,
  ;     [:person 2] 4,
  ;     [:num-people-in-timeslot 2] 1,
  ;     [:num-people-in-timeslot 3] 1,
  ;     [:num-people-in-timeslot 4] 1}
  ;
  ; for some reason, I get the results in a different order than Alex
  ; shows in his blog



; finally, we can combine al this into a nice function
;
; NOTE: we also have loco drop some of the uninteresting results
;       specifically :_num-people-in-timeslot (use '_' as the first character
;       of the name (even as a keyword! eg, ":_name")
;
(defn schedule-with-conflicts [availability]
  (let [timeslots       (distinct (apply concat availability))

        availability-constraints
                        (for [i (range (count availability))]
                          ($in [:person i] (availability i)))

        person-vars
                        (for [i (range (count availability))] [:person i])

        people-in-timeslot-vars
                        (for [i timeslots] [:_num-people-in-timeslot i])

        conflict-constraints
                        (for [i timeslots]
                          ($in [:_num-people-in-timeslot i] 0 2))

        number-in-timeslots
                        ($cardinality person-vars
                                      (zipmap timeslots people-in-timeslot-vars))

        number-of-conflicts
                        [($in :_number-of-conflicts 0 (count timeslots))
                         ($cardinality people-in-timeslot-vars {2 :_number-of-conflicts})]

        all-constraints (concat availability-constraints
                                conflict-constraints
                                [number-in-timeslots]
                                number-of-conflicts)]

    (into (sorted-map)
          (solution all-constraints :minimize :_number-of-conflicts))))



; what answer do we get this time?
;
(schedule-with-conflicts
  [[1 2 3 4]
   [1 4]
   [1 4]
   [1 4]])
  ; => {[:person 0] 3, [:person 1] 1, [:person 2] 4, [:person 3] 4}


