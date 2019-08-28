(ns sparse-grid)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; had an interesting idea in the car today: describe the resource
; as a rectangle: [top-left, top-right, bottom-right, bottom-left]
;
; then we can use "line drawing" as the visualization abstraction
;
; in canvases (SVG, etc) we draw actual lines, in grids we treat the
; grid cells as pixels and write a pixel-fitting function!
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def empty-grid {})

(defn gen-id
  ([ch ts]
   (str ch "-" ts))
  ([[ch ts]]
   (str ch "-" ts)))


(defn- prune-allocations
  [grid]
  (remove (fn [[_ v]] (empty? (:allocated-to v)))
          (seq grid)))



(defn- populate
  [grid requestor-id request-cells]
  (reduce (fn [g [ch ts]]
            (assoc g (gen-id ch ts)
                     {:channel      ch
                      :timeslot     ts
                      :allocated-to (into #{}
                                          (concat (get-in g [(gen-id ch ts)
                                                             :allocated-to])
                                                  #{requestor-id}))}))
          grid request-cells))


(defn- apply-requests
  [grid requests]
  (if (empty? requests)
    grid
    (let [[id allocs] (first requests)]
      (recur (populate grid id allocs) (rest requests)))))




(defn- check-satisfied
  [pred-fn grid original-grid]
  (into {}
        (for [alloc (keys grid)]
          (let [val (get-in grid [alloc :allocated-to])]
            (if (and (pred-fn val)
                     (not (= val (get-in original-grid
                                         [alloc :allocated-to]))))
              {alloc (get grid alloc)})))))



(defn- check-rejected
  [pred-fn grid]
  (into {}
        (for [alloc (keys grid)]
          (if (not (pred-fn (get-in grid [alloc :allocated-to])))
            {alloc (get-in grid [alloc :allocated-to])}))))



(defn- remove-rejects
  [original-grid grid rejects]
  (into {}
        (reduce (fn [g [id _]]
                  (assoc g id (get original-grid id)))
                grid rejects)))



(defn- retract-one-requestor
  [grid [requestor-id retraction-cells]]
  (into {}
        (prune-allocations
          (reduce (fn [g id]
                    (assoc-in g [(gen-id id) :allocated-to]
                              (disj (get-in g [(gen-id id) :allocated-to])
                                    requestor-id)))
                  grid retraction-cells))))





(defn test-requests
      [sat-rule rej-rule initial-grid requests]
  (let [g (into {}
                (remove (fn [[_ v]] (nil? v))
                        (seq (apply-requests initial-grid requests))))]
    (let [sat (check-satisfied sat-rule g initial-grid)
          rej (check-rejected rej-rule g)]
      {:before initial-grid
       :after  (into {}
                     (remove (fn [[_ v]] (or (nil? v) (empty? v)))
                             (seq (remove-rejects initial-grid g rej))))
       :sat    sat
       :rej    rej})))



(defn retract-requests
      [grid retractions]
  (if (empty? retractions)
    grid
    (recur
      (retract-one-requestor grid (first retractions))
      (rest retractions))))






(comment
  (use 'sparse-grid :reload)
  (in-ns 'sparse-grid)


  (def overlapping-requests {"b" #{[0 0] [1 1]}
                             "a" #{[0 1] [1 1] [1 2]}
                             "c" #{[3 3] [3 4] [4 4]}})

  (test-requests sat-rule
                 rej-rule
                 empty-grid
                 {"b" #{[0 0] [1 1]}})
  (test-requests sat-rule
                 rej-rule
                 empty-grid
                 {"b" #{[0 0]} "a" #{[0 0]}})

  (populate empty-grid "a" #{[0 0]})
  (populate empty-grid "b" #{[0 0]})


  (apply-requests empty-grid {"b" #{[0 0]} "a" #{[0 0]}})

  (check-satisfied sat-rule
                   {"0-0" {:channel      0,
                           :timeslot     0,
                           :allocated-to #{"a" "b"}}}
                   {})
  (check-rejected rej-rule
                  {"0-0" {:channel      0,
                          :timeslot     0,
                          :allocated-to #{"a" "b"}}})


  (test-requests sat-rule
                 rej-rule
                 empty-grid
                 overlapping-requests)


  ; now try chaining a few requests together. do we lose any pre-existing
  ; allocating when we make the next attempt?
  ;
  (def requests-2 {"d" #{[0 0] [1 1]}
                   "e" #{[2 2] [2 3] [2 4]}
                   "f" #{[1 3] [1 4]}})
  (def current-grid (atom empty-grid))


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

  (def fill-in {"g" #{[0 1] [0 2] [0 3] [0 4]}
                "h" #{[1 0] [2 0] [3 0] [4 0]}})
  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 fill-in)))

  ; some retractions
  ;
  (def retractions {"g" #{[0 2]}})
  (def retractions-2 {"g" #{[0 3] [0 4]}})
  (def retract-3 {"a" #{[0 1]} "c" #{[4 4]}})
  (def retract-4 {"a" #{[70 70]}})

  (defn- diff [g1 g2]
    (clojure.set/difference (set (keys g1)) (set (keys g2))))

  (diff @current-grid
        (retract-requests @current-grid retractions))

  (diff @current-grid
        (retract-requests @current-grid retractions-2))

  (diff @current-grid
        (retract-requests @current-grid retract-3))

  (diff @current-grid
        (retract-requests @current-grid retract-4))

  (diff @current-grid
      (retract-requests @current-grid {}))

  (diff empty-grid
        (retract-requests empty-grid retractions))



  ())





(comment
  (gen-id 0 0)


  (populate {} "a" [[0 0] [1 1] [10 129]])

  (apply-requests {} {"a" #{[0 0]} "b" #{[0 0]}})


  (remove-rejects orig-grid test-grid {"1-1" #{"b" "c"}})


  (def test-grid {"0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}},
                  "0-1" {:channel 0, :timeslot 1, :allocated-to #{"a"}},
                  "1-0" {:channel 1, :timeslot 0, :allocated-to #{"b"}},
                  "1-1" {:channel 1, :timeslot 1, :allocated-to #{"b" "c"}}})
  (def orig-grid {"0-0" {:channel 0, :timeslot 0, :allocated-to #{"a"}},
                  "1-1" {:channel 1, :timeslot 1, :allocated-to #{"b"}}})

  (keys test-grid)
  (def sat-rule #(and (<= (count %) 1)
                      (> (count %) 0)))
  (def rej-rule #(<= (count %) 1))

  (check-satisfied sat-rule test-grid orig-grid)
  (check-rejected rej-rule test-grid)

  (test-requests sat-rule rej-rule {}
                 {"a" #{[0 0] [0 1]} "b" #{[1 0] [1 1]}})

  (def s-g (test-requests sat-rule rej-rule orig-grid
                          {"a" #{[0 0] [0 1]} "b" #{[1 0] [1 1]}}))


  (retract-requests s-g {"a" #{[0 0]}})


  ())



; TESTS
(comment

  (def overlapping-requests {"b" #{[0 0] [1 1]}
                             "a" #{[0 1] [1 1] [1 2]}
                             "c" #{[3 3] [3 4] [4 4]}})

  (def sat-rule #(and (<= (count %) 1)
                      (> (count %) 0)))
  (def rej-rule #(<= (count %) 1))


  ; a very simple case
  ;
  (test-requests sat-rule rej-rule empty-grid overlapping-requests)



  ; now try chaining a few requests together. do we lose any pre-existing
  ; allocating when we make the next attempt?
  ;
  (def requests {"d" #{[0 0] [1 1]}
                 "e" #{[2 2] [2 3] [2 4]}
                 "f" #{[1 3] [1 4]}})
  (def current-grid (atom empty-grid))


  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 overlapping-requests)))


  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 requests)))


  (def fill-in {"g" #{[0 1] [0 2] [0 3] [0 4]}
                "h" #{[1 0] [2 0] [3 0] [4 0]}})
  (reset! current-grid
          (:after (test-requests sat-rule
                                 rej-rule
                                 @current-grid
                                 fill-in)))


  ; some retractions
  ;
  (def retractions {"g" #{[0 2]}})
  (def retractions-2 {"g" #{[0 3] [0 4]}})
  (def retract-3 {"a" #{[0 1]} :c #{[4 4]}})

  (retract-requests @current-grid retractions)

  (retract-requests @current-grid retractions-2)

  (retract-requests @current-grid retract-3)



  ())
