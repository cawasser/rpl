(ns resource-alloc.grid-support
  (:require [resource-alloc.sparse-request-rules :as rules]
            [resource-alloc.sparse-grid :as grid]
            [clojure.math.combinatorics :as combo]))

;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;
;
; definitions of things we'll need later
;
; skip to "START HERE"
;

(def sat-rule #(and (<= (count %) 1)
                    (> (count %) 0)))
(def rej-rule #(<= (count %) 1))



(def min-grid {"0-3" {:channel      0
                      :timeslot     3
                      :allocated-to #{"q"}}})

(def used-grid {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
                "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
                "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}})

(def used-grid-2 {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
                  "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
                  "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}
                  "2-2" {:channel 2 :timeslot 2 :allocated-to #{"m"}}})

(def requests-6 {"b" #{[0 0] [[0 1 2 3] 1]}
                 "a" #{[1 1] [1 2] [[3 4] 4]}
                 "q" #{[[2 3] 2]}
                 "c" #{[[2 3] 1] [3 3] [[3 4] 4]}})
(def min-req {"b" #{[0 0]}})
(def min-succ-fail {"b" #{[0 0]} "c" #{[0 3]}})


(def alloc-ui-reqs {"j" #{[0 0] [[1 2 3] 1]}
                    "k" #{[0 2] [3 0]}
                    "l" #{[[2 3] 0] [[2 3] 1] [[2 3] 2]}})
(def alloc-ui-grid {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
                    "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}}
                    "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}}
                    "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}}
                    "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}}
                    "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}}
                    "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}}
                    "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}}
                    "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}}
                    "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}}
                    "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}})

















(def potential-grid {#{}        {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
                                 "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}}
                                 "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}}
                                 "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}}
                                 "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}}
                                 "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}}
                                 "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}}
                                 "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}}
                                 "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}}
                                 "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}}
                                 "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}}
                     #{"k"}     {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}},
                                 "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}},
                                 "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}},
                                 "0-2" {:channel 0, :timeslot 2, :allocated-to #{"k"}},
                                 "3-0" {:channel 3, :timeslot 0, :allocated-to #{"k"}}
                                 "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}},
                                 "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}},
                                 "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}},
                                 "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}},
                                 "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}},
                                 "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}},
                                 "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}},
                                 "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}}
                     #{"l"}     {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
                                 "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}}
                                 "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}}
                                 "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}}
                                 "3-0" {:channel 3, :timeslot 0, :allocated-to #{"l"}}
                                 "3-1" {:channel 3, :timeslot 1, :allocated-to #{"l"}}
                                 "3-2" {:channel 3, :timeslot 2, :allocated-to #{"l"}}
                                 "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}}
                                 "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}}
                                 "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}}
                                 "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}}
                                 "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}}
                                 "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}}
                                 "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}}
                     #{"l" "k"} {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
                                 "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}}
                                 "2-0" {:channel 2, :timeslot 0, :allocated-to #{"l"}}
                                 "0-2" {:channel 0, :timeslot 2, :allocated-to #{"k"}}
                                 "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}}
                                 "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}}
                                 "3-0" {:channel 3, :timeslot 0, :allocated-to #{"k"}}
                                 "3-1" {:channel 3, :timeslot 1, :allocated-to #{"l"}}
                                 "3-2" {:channel 3, :timeslot 2, :allocated-to #{"l"}}
                                 "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}}
                                 "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}}
                                 "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}}
                                 "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}}
                                 "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}}
                                 "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}}
                                 "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}}})






;
; two functions from alloc-ui.routes.grid-support
;

(defn- null-tx [grid requests]
  {:before grid
   :after  grid
   :sat    {}
   :rej    {}
   :error  "Engine rejects the requests"})


(defn apply-requests-to-grid
      "basically what is in alloc-ui.routes.grid-support"

  ;
  ; we need to comment this out since we don't have a database
  ;
  ;([requests]
  ; (apply-requests-to-grid requests (-> (db/get-current-grid)
  ;                                      first
  ;                                      :cells
  ;                                      clojure.edn/read-string)))

  [requests grid]
  (let [adjusted-reqs (rules/generate-acceptable-requests grid requests)]

    {:adjusted-requests adjusted-reqs
     :tx                (if (empty? adjusted-reqs)
                          (null-tx grid requests)
                          (grid/test-requests
                            sat-rule
                            rej-rule
                            grid
                            adjusted-reqs))}))



;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;
;
; START HERE
;


;
; This approach works more directly with the
; request maps themselves, rather than pulling them
; apart and then having to recombine them later
;

;remove the 'map-ness' and treat each k/v as a single value
(seq alloc-ui-reqs)

; find all the combinations of our "requests"
(combo/subsets (seq alloc-ui-reqs))

; find all the combinations of our "requests"
(apply hash-map (combo/subsets (seq alloc-ui-reqs)))


; remove a layer of "wrapping"
(map #(flatten %) (combo/subsets (seq alloc-ui-reqs)))

; re-wrap as hash-maps
(map (fn [x] (apply hash-map x))
     (map #(flatten %) (combo/subsets (seq alloc-ui-reqs))))

(def pp
  (map (fn [x] (apply hash-map x))
       (map #(flatten %) (combo/subsets (seq alloc-ui-reqs)))))
pp

(->> alloc-ui-reqs
     seq
     combo/subsets
     (map #(flatten %)))

(->> alloc-ui-reqs
     seq
     combo/subsets
     (map #(flatten %))
     (map (fn [x] (apply hash-map x))))


(apply-requests-to-grid (second pp) alloc-ui-grid)

(map #(apply-requests-to-grid % alloc-ui-grid) pp)


(def xp (map #(apply-requests-to-grid % alloc-ui-grid) pp))
xp

(map (fn [x]
       {(into #{} (keys (:adjusted-requests x)))
        (-> x :tx :after)}) xp)


(->> alloc-ui-reqs
     seq
     combo/subsets
     (map (fn [x] (flatten x)))
     (map (fn [x] (apply hash-map x)))
     (map (fn [x] (apply-requests-to-grid x alloc-ui-grid)))
     (map (fn [x]
            {(into #{} (keys (:adjusted-requests x)))
             (-> x :tx :after)})))


(->> alloc-ui-reqs
     seq
     combo/subsets
     (map (fn [x] (flatten x)))
     (map (fn [x] (apply hash-map x)))
     (map (fn [x] (apply-requests-to-grid x alloc-ui-grid)))
     (map (fn [x]
            {(into #{} (keys (:adjusted-requests x)))
             (-> x :tx :after)}))
     (into {}))



;
; the actual function we should use in alloc-ui
;

(defn analyze-combinations
      "analyses all possible combinations of the individual requests
      and results into a format for the client to use"

  [grid requests]

  (->> requests

       ; convert the request map to a collection of vectors so we
       ; can use a request as a single datum (rather than a k-v)
       seq

       ;  gets all the possible combinations of requests
       combo/subsets

       ; turn the results back into a map for further processing
       (map (fn [x] (flatten x)))
       (map (fn [x] (apply hash-map x)))

       ; run each through the solver
       (map (fn [x] (apply-requests-to-grid x grid)))

       ; pick out the results
       (map (fn [x]
              {(into #{} (keys (:adjusted-requests x)))
               (-> x :tx :after)}))

       ; and put everything into a single map (this serves to compress
       ; out the multiple copies of the #{} key...)
       (into {})))




(analyze-combinations alloc-ui-grid alloc-ui-reqs)

(= (analyze-combinations alloc-ui-grid alloc-ui-reqs)
   potential-grid)


(analyze-combinations min-grid min-req)
(analyze-combinations min-grid min-succ-fail)
(analyze-combinations used-grid requests-6)
(analyze-combinations used-grid-2 requests-6)


; how do we figure out if the given request-set is one of the
; valid combinations?
;
(contains? #{#{"k"} #{"l"} #{"m"} #{"k" "l"} #{"k" "m"}}
           #{"m" "k" "l"})

(contains? (into #{} '(#{"k"} #{"l"} #{"m"} #{"k" "l"} #{"k" "m"}))
           #{"m" "k" "l"})

(contains? #{#{"k"} #{"l"} #{"m"} #{"k" "l"} #{"k" "m"}}
           #{"l" "k"})
