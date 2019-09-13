(ns grid-support
  (:require [sparse-request-rules :as rules]
            [sparse-grid :as grid]
            [clojure.math.combinatorics :as combo]))



;;;;;;;;;;;;;;;;;;
;
;
; try all combinations of requests together to see how each
; combination works with the grid
;

;(def sat-rule #(and (<= (count %) 1)
;                    (> (count %) 0)))
;(def rej-rule #(<= (count %) 1))
;
;
;
;
;(def min-grid {"0-3" {:channel      0
;                      :timeslot     3
;                      :allocated-to #{"q"}}})
;
;(def used-grid {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
;                "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
;                "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}})
;
;(def used-grid-2 {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
;                  "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
;                  "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}
;                  "2-2" {:channel 2 :timeslot 2 :allocated-to #{"m"}}})
;
;(def requests-6 {"b" #{[0 0] [[0 1 2 3] 1]}
;                 "a" #{[1 1] [1 2] [[3 4] 4]}
;                 "q" #{[[2 3] 2]}
;                 "c" #{[[2 3] 1] [3 3] [[3 4] 4]}})
;(def min-req {"b" #{[0 0]}})


;(keys requests-6)
;
;(combo/subsets (keys requests-6))
;
;(map #(into #{} %) (combo/subsets (keys requests-6)))
;
;(for [r (map #(into #{} %) (combo/subsets (keys requests-6)))]
;  (for [e r]
;    {e (get requests-6 e)}))
;
;(for [r (map #(into #{} %) (combo/subsets (keys requests-6)))]
;  (into {}
;        (for [e r]
;          (into {} {e (get requests-6 e)}))))
;
;
;
;
;
;
;(defn null-tx [grid requests]
;  {:before grid
;   :after  grid
;   :sat    {}
;   :rej    {}
;   :reqs   requests
;   :error  "Engine rejects the requests"})
;
;
;(defn check-combination
;      [g r]
;  (let [adjusted-reqs (rules/generate-acceptable-requests g r)]
;
;    {:adjusted-requests adjusted-reqs
;     :tx                (if (empty? adjusted-reqs)
;                          (null-tx g r)
;                          (grid/test-requests
;                            sat-rule
;                            rej-rule
;                            g
;                            adjusted-reqs))}))
;
;
;
;(for [r (map #(into #{} %) (combo/subsets (keys requests-6)))])
;
;
;
;
;
;
;(for [r (map #(into #{} %) (combo/subsets (keys requests-6)))]
;  (for [e r]
;    (check-combination used-grid {e (get requests-6 e)})))
;
;
;
;(for [r (combo/subsets (keys requests-6))]
;  (for [e r]
;    (-> (check-combination used-grid
;                           (into {} {e (get requests-6 e)}))
;        :tx
;        :after)))
;
;(for [r (combo/subsets (keys requests-6))]
;  (for [e r]
;    (-> (check-combination used-grid
;                           (into {} {e (get requests-6 e)}))
;        :tx
;        :after)))
;
;
;
;
;(defn check-all-combination
;  [grid requests]
;
;  (let [combos (map #(into #{} %) (combo/subsets (keys requests)))
;        reqs   (for [r combos]
;                 (into {}
;                       (for [e r]
;                         (into {} {e (get requests e)}))))]
;    (for [each-r reqs]
;      (let [results (-> (check-combination grid each-r) :tx)]
;        {(into #{} (keys each-r))
;         (if (:error results)
;           {}
;           (:after results))}))))
;
;
;
;(check-all-combination used-grid requests-6)
;(check-all-combination used-grid-2 requests-6)
;
;(def min-grid {"0-3" {:channel      0
;                      :timeslot     3
;                      :allocated-to #{"q"}}})
;
;(def min-succ-fail {"b" #{[0 0]} "c" #{[0 3]}})
;
;(check-all-combination min-grid min-req)
;(check-all-combination min-grid min-succ-fail)
;
;(def f (into {} (check-all-combination min-grid min-succ-fail)))
;(defn filter-values [record]
;  (apply dissoc
;         record
;         (for [[k v] record :when (empty? v)] k)))
;
;(filter-values {:a #{"a"} :b #{"b"} :c #{}})
;(filter-values f)
;
;
;(def record {:a #{"a"} :b #{"b"} :c #{}})
;(apply merge (for [[k v] record
;                   :when (not (empty? v))] {k v}))
;
;(apply dissoc
;       record
;       (for [[k v] record :when (empty? v)] k))
;
;
;(defn analyze-combinations
;      [grid requests]
;  (let [r (into {} (check-all-combination grid requests))]
;    (apply dissoc
;           r
;           (for [[k v] r :when (empty? v)] k))))
;
;
;(analyze-combinations min-grid min-req)
;(analyze-combinations min-grid min-succ-fail)
;(analyze-combinations used-grid requests-6)
;(analyze-combinations used-grid-2 requests-6)
;
;
;(def sat-rule #(and (<= (count %) 1)
;                    (> (count %) 0)))
;(def rej-rule #(<= (count %) 1))
;
;
;
;
;(def min-grid {"0-3" {:channel      0
;                      :timeslot     3
;                      :allocated-to #{"q"}}})
;
;(def used-grid {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
;                "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
;                "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}})
;
;(def used-grid-2 {"0-3" {:channel 0 :timeslot 3 :allocated-to #{"q"}}
;                  "0-4" {:channel 0 :timeslot 4 :allocated-to #{"q"}}
;                  "1-4" {:channel 1 :timeslot 4 :allocated-to #{"q"}}
;                  "2-2" {:channel 2 :timeslot 2 :allocated-to #{"m"}}})
;
;(def requests-6 {"b" #{[0 0] [[0 1 2 3] 1]}
;                 "a" #{[1 1] [1 2] [[3 4] 4]}
;                 "q" #{[[2 3] 2]}
;                 "c" #{[[2 3] 1] [3 3] [[3 4] 4]}})
;(def min-req {"b" #{[0 0]}})
;
;(def potential-grid {#{"k"}     {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}},
;                                 "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}},
;                                 "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}},
;                                 "0-2" {:channel 0, :timeslot 2, :allocated-to #{"k"}},
;                                 "3-0" {:channel 3, :timeslot 0, :allocated-to #{"k"}}
;                                 "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}},
;                                 "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}},
;                                 "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}},
;                                 "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}},
;                                 "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}},
;                                 "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}},
;                                 "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}},
;                                 "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}}
;                     #{"l"}     {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
;                                 "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}}
;                                 "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}}
;                                 "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}}
;                                 "3-0" {:channel 3, :timeslot 0, :allocated-to #{"l"}}
;                                 "3-1" {:channel 3, :timeslot 1, :allocated-to #{"l"}}
;                                 "3-2" {:channel 3, :timeslot 2, :allocated-to #{"l"}}
;                                 "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}}
;                                 "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}}
;                                 "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}}
;                                 "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}}
;                                 "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}}
;                                 "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}}
;                                 "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}}
;                     #{"l" "k"} {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
;                                 "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}}
;                                 "2-0" {:channel 2, :timeslot 0, :allocated-to #{"l"}}
;                                 "0-2" {:channel 0, :timeslot 2, :allocated-to #{"k"}}
;                                 "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}}
;                                 "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}}
;                                 "3-0" {:channel 3, :timeslot 0, :allocated-to #{"k"}}
;                                 "3-1" {:channel 3, :timeslot 1, :allocated-to #{"l"}}
;                                 "3-2" {:channel 3, :timeslot 2, :allocated-to #{"l"}}
;                                 "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}}
;                                 "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}}
;                                 "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}}
;                                 "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}}
;                                 "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}}
;                                 "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}}
;                                 "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}}})
;(def alloc-ui-reqs {"j" #{[0 0] [[1 2 3] 1]}
;                    "k" #{[0 2] [3 0]}
;                    "l" #{[[2 3] 0] [[2 3] 1] [[2 3] 2]}})
;(def alloc-ui-grid {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
;                    "2-4" {:channel 2, :timeslot 4, :allocated-to #{"aa"}}
;                    "0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}}
;                    "1-4" {:channel 1, :timeslot 4, :allocated-to #{"aa"}}
;                    "3-4" {:channel 3, :timeslot 4, :allocated-to #{"z"}}
;                    "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}}
;                    "1-3" {:channel 1, :timeslot 3, :allocated-to #{"c"}}
;                    "1-2" {:channel 1, :timeslot 2, :allocated-to #{"b"}}
;                    "2-2" {:channel 2, :timeslot 2, :allocated-to #{"b"}}
;                    "2-3" {:channel 2, :timeslot 3, :allocated-to #{"c"}}
;                    "3-3" {:channel 3, :timeslot 3, :allocated-to #{"z"}}})
;
;(analyze-combinations alloc-ui-grid alloc-ui-reqs)
;
;(= (analyze-combinations alloc-ui-grid alloc-ui-reqs)
;   potential-grid)
;
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


(def potential-grid {#{} {"4-4" {:channel 4, :timeslot 4, :allocated-to #{"z"}}
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


;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;
;
; Another approach which works more directly with the
; request maps themselves, rather than pulling them
; apart and then having to recombine them later
;
; this approach eliminates the need for check-all-combinations
; since we already have that kind of data to start with
;

;remove the 'map-ness'
(seq alloc-ui-reqs)

; remove one layer of "wrapping"
(flatten (seq alloc-ui-reqs))

; partition into lists that mimic the original maps
(partition 2 (flatten (seq alloc-ui-reqs)))

; find all the combinations of our "requests"
(combo/subsets (partition 2 (flatten (seq alloc-ui-reqs))))

; find all the combinations of our "requests"
(apply hash-map (combo/subsets
                  (partition 2
                             (flatten (seq alloc-ui-reqs)))))


; remove a layer of "wrapping"
(map #(flatten %) (combo/subsets
                    (partition 2
                               (flatten (seq alloc-ui-reqs)))))

; re-wrap as hash-maps
(map (fn [x] (apply hash-map x))
     (map #(flatten %) (combo/subsets
                         (partition 2
                                    (flatten (seq alloc-ui-reqs))))))

(def pp
  (map (fn [x] (apply hash-map x))
       (map #(flatten %) (combo/subsets
                           (partition 2
                                      (flatten (seq alloc-ui-reqs)))))))
(def pp
  (map (fn [x] (apply hash-map x))
       (map #(flatten %) (combo/subsets
                           (partition 2
                                      (flatten (seq alloc-ui-reqs)))))))
pp

(->> alloc-ui-reqs
     seq
     flatten
     (partition 2)
     combo/subsets
     (map #(flatten %)))

(def check-combination)

(check-combination alloc-ui-grid (second pp))

(map #(check-combination  alloc-ui-grid %) pp)


(def xp (map #(check-combination alloc-ui-grid %) pp))
xp

(map (fn [x]
       {(into #{} (keys (:adjusted-requests x)))
        (-> x :tx :after)}) xp)


(->> alloc-ui-reqs
     seq
     flatten
     (partition 2)
     combo/subsets
     (map (fn [x] (flatten x)))
     (map (fn [x] (apply hash-map x)))
     (map (fn [x] (check-combination alloc-ui-grid x)))
     (map (fn [x]
            {(into #{} (keys (:adjusted-requests x)))
             (-> x :tx :after)}))
     (into {}))






(defn- null-tx [grid requests]
  {:before grid
   :after grid
   :sat {}
   :rej {}
   :error "Engine rejects the requests"})


(defn apply-requests-to-grid
  "basically what is in alloc-ui.routes.grid-support"

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


(defn analyze-combinations
  "analyses all possible combinations of the individual requests
  and results into a format for the client to use"

  [grid requests]

  (->> requests

       ; rearrange the request collection so we can use a request
       ; as a single datum (rather than a k-v)
       seq
       flatten
       (partition 2)

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

