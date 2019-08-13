(ns scratch)


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
          (for [req-id    (keys demands)
                slot      (req-id demands)]
            (let [[channels time-slot] slot]
              (if (= 1 (count channels))
                ($= [:requestor req-id time-slot (first channels)]
                          (first channels)))))))

(request-fixed-constraints fill-in)


(defn request-flex-constraints [demands]
  (remove nil?
          (for [req-id    (keys demands)
                slot      (req-id demands)]
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
          s2id (for [req-id    (keys requests)
                     slot      (req-id requests)]
                 (let [[ch time-slot] slot]
                   [[ch time-slot] req-id]))
          re (remove nil?
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




(for [[[ch ts] r]  (build-defaults requests)]
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
