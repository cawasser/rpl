(ns x-duce
  (:require [clojure.core.async :as async]
            [jackdaw.admin :as ja]
            [jackdaw.client :as jc]
            [jackdaw.client.log :as jcl]
            [jackdaw.serdes.edn :refer [serde]]
            [jackdaw.streams :as js]
            [willa.streams :as ws]
            [willa.core :as w]))


;; region ; let's start simple (Brian Sunter) NOTE: this page is now missing (as of 202230322)
; see https://briansunter.com/blog/transducers-clojure/


(+ 1 2 3 4 5 6 7 8 9 10)

(apply + '(1 2 3 4 5 6 7 8 9 10))

(reduce + 0 '(1 2 3 4 5 6 7 8 9 10))

(+ (+ (+ (+ (+ (+ (+ (+ (+ (+ 0 1) 2) 3) 4) 5) 6) 7) 8) 9) 10)



(defn- my-reduce [f acum coll]
  (if (empty? coll)
    acum
    (my-reduce f (f acum (first coll)) (rest coll))))

(my-reduce + 0 '())
(my-reduce + 0 '(1))
(my-reduce + 0 '(1 2))
(my-reduce + 0 '(1 2 3 4 5 6 7 8 9 10))
(my-reduce + 10 '(1 2 3 4 5 6 7 8 9 10))

(reduce + 10 '(1 2 3 4 5 6 7 8 9 10))

(reduce + 0 (range 1 11))


(reduce + 0 (filter even? (range 1 11)))

(->> (range 1 11)
  (filter even?)
  (reduce + 0))


(reduce + 0 (map #(* 2 %) (filter even? (range 1 11))))

(->> (range 1 11)                                           ; ->10
  (filter even?)                                            ; 10->5
  (map #(* 2 %))                                            ; 5->5
  (reduce + 0))                                             ; 5->1


; sum the doubles of all the even values
(reduce (fn [a i]
          (if (even? i)
            (+ a (* 2 i))
            a))
  0
  (range 1 11))




; using (comp ...)
(def some-maps-data [{:one {:two "alpha"}}
                     {:one {:two "bravo"}}
                     {:one {:two "charlie"}}])
(def numbers-data (range 1 11))

(map :one some-maps-data)

(map :two (map :one some-maps-data))
(->> some-maps-data
  (map :one)
  (map :two))

(map (fn [x] (:two (:one x))) some-maps-data)

(def my-fn (comp :two :one))

(map my-fn some-maps-data)

; comp applies the function right-to-left (most of the time...)
(map #(-> % :one :two) some-maps-data)


(str (+ 8 8 8))
((comp str +) 8 8 8)
((comp clojure.string/reverse str +) 8 8 8)


; back to double the evens
(def double-even-xforms (comp (filter even?) (map #(* 2 %))))

; kind of like
(partial map #(* 2 %))

(reduce + 0 (into [] double-even-xforms numbers-data))

(reduce + 0
  ((fn [x]
     (->> x
       (filter even?)
       (map #(* 2 %))))
   numbers-data))

; why is this comp working left-to-right? transducers!
; filter and map are both transducers out of the box...

;; endregion


;; region ; https://dev.solita.fi/2021/10/14/grokking-clojure-transducers.html
;       starts with reducers, like "conj", "reduce", etc.

(conj [1 2] 3)
;;          ^ input
;;    ^^^^^ accumulated result
;;=> [1 2 3]
;;   ^^^^^^^ new result

(reduce conj #{} [1 2 2 3 1])
(conj #{} 1)
(conj #{1} 2)
(conj #{1 2} 2)
(conj #{1 2} 3)
(conj #{1 3 2} 1)


(defn inc-transducer
  "Given a reducing function rf, return a new reducing function that increments
  every input it receives, then calls rf with the result and the incremented
  input."
  ;; rf stands for "reducing function"
  [rf]
  ;; this here's a new reducing function
  (fn [result input]
    ;; here we call the original reducing function
    (rf result (inc input))))

(def inc-then-conj (inc-transducer conj))

(conj [1 2] 3)
(conj [1 2] (inc 2))

(inc-then-conj [1 2] 3)

(reduce inc-then-conj [] [1 2 3 4 5])

(reduce + 0 [1 2 3 4 5])
(reduce (inc-transducer +) 0 [1 2 3 4 5])

(reduce #(conj %1 (inc %2)) [] [1 2 3 4 5])
(reduce #(+ %1 (inc %2)) 0 [1 2 3 4 5])
(reduce (fn [result input] (+ result (inc input))) 0 [1 2 3 4 5])



(defn mapping
  "Given function f, return a transducer that calls f on every input it
  receives."
  [f]
  (fn [rf]
    (fn [result input]
      (rf result (f input)))))

(def inc-mapper
  "Given a reducing function rf, return a reducing function that increments its
  input before calling rf."
  (mapping inc))

(def inc-rf
  "A reducing function that increments its input, then adds it into the
  accumulated result."
  (inc-mapper conj))

(reduce inc-rf [] [1 2 3 4 5])

(reduce ((mapping inc) conj) [] [1 2 3 4 5])
(reduce ((mapping inc) +) 0 [1 2 3 4 5])

(map inc [1 2 3 4 5])


(reduce inc-rf '() [1 2 3 4 5])
(reduce inc-rf #{} [1 2 3 4 5])

(= (reduce inc-rf [] [1 2 3 4 5])
  (map inc [1 2 3 4 5]))


(reduce
  ((mapping inc) conj)
  []
  [1 2 3 4 5])
(map inc [1 2 3 4 5])

(reduce
  ((mapping inc) +)
  0
  [1 2 3 4 5])

(reduce
  ((mapping inc) -)
  0
  [1 2 3 4 5])

(reduce
  ((mapping inc) *)
  0
  [1 2 3 4 5])

(reduce
  ((mapping #(+ % 5)) conj)
  []
  [1 2 3 4 5])

(reduce
  ((mapping #(+ % 5)) +)
  0
  [1 2 3 4 5])


; but how is this better than just -> & ->>?

(->>
  [1 2 3 4 5]
  (filter even?)                                            ; intermediate collection (2 4)
  (map inc))

(defn filtering
  "Given a predicate function pred, return a transducer that only retains items
  for which pred returns true."
  [pred]
  (fn [rf]
    (fn [result input]
      (if (pred input)
        (rf result input)
        result))))

(def rf
  "A reducing function that filters for even numbers, increments any positive
  result, then conjoins it into the result."
  ((comp (filtering even?) (mapping inc)) conj))

(reduce rf [] [1 2 3 4 5])



(reduce
  ; |-- transforming fn             |-- reducing fn
  ; v                               v
  ((comp (filter even?) (map inc)) conj)                    ;; <- reducing fn (awesome conj)
  []                                                        ;; <- initial value
  [1 2 3 4 5])                                              ;; <- input collection

; 'reduce' does have issues with some transducers, so...
(transduce
  (comp (filter even?) (map inc))                           ; <-- transforming fn
  conj                                                      ; <-- reducing fn
  []
  [1 2 3 4 5])


;; endregion


;; region ; https://flexiana.com/2022/02/investigating-clojures-transducer-composition
;       starts by comparing comp and ->>

; the examples mainly just reiterate those from Solita, above

;; endregion


;; region ; see https://www.grammarly.com/blog/engineering/building-etl-pipelines-with-clojure-and-transducers/

(def double-even-xforms (comp (filter even?) (map #(* 2 %))))

(reduce + 0 (into [] double-even-xforms numbers-data))
;(def x-form (comp (mapcat :parse-json-file-reducible)
;              (filter :valid-entry?)
;              (keep :transform-entry-if-relevant)
;              (partition-all 1000)
;              (map :save-into-database)))

(transduce double-even-xforms + 0 numbers-data)
;(transduce x-form             (constantly nil) nil :files)

(transduce double-even-xforms conj [] numbers-data)

(def c (async/chan 1 double-even-xforms))

(async/go
  (async/onto-chan! c [5 6 8 12 15]))


(loop [n (async/<!! c)]
  (when n
    (println n)
    (recur (async/<!! c))))

;; endregion


;; region ; how do we make our own functions into transducers? (Abhinav Omprakash)

; see https://www.abhinavomprakash.com/posts/writing-transducer-friendly-code/

(->> (range 10)
  (map inc)
  (filter even?)
  (map #(* 2 %)))

;; (4 8 12 16 20)



(def transducer
  (comp
    (map inc)
    (filter even?)
    (map #(* 2 %))))

;; realizing the transducer
(into [] transducer (range 10))
; [4 8 12 16 20]




; this is similar to how we write (service-handler/operation ...)

(defn increment-all [coll]
  (map inc coll))

(defn filter-evens [coll]
  (filter even? coll))

(defn double-all [coll]
  (map #(* 2 %) coll))


(filter-evens (range 10))
; (0 2 4 6 8)

(->> (range 10)
  (increment-all,)
  (filter-evens,)
  (double-all,))


(comment

  (def x-ducerrrrr
    (comp
      (increment-all)
      (filter-evens)
      (double-all)))

  (into [] x-ducerrrrr (range 10))


  ())



(sequence (map dec) (range 10))

(defn increment-all
  ([]
   (map inc))
  ([coll]
   (sequence (increment-all) coll)))

(increment-all (range 10))
; (1 2 3 4 5 6 7 8 9 10)



(defn increment-all
  ([]
   (map inc))
  ([coll]
   (map inc coll)))


;; 1st impl

(defn filter-then-map
  ([] (comp
        (filter even?)
        (map inc)))
  ([coll]
   (sequence (filter-then-map) coll)))


;; 2nd impl
(defn filter-then-map
  ([] (comp
        (filter even?)
        (map inc)))
  ([coll]
   ; not using a transducer
   (->> coll
     (filter even?)
     (map inc))))




(defn increment-all
  ([]
   (map inc))
  ([coll]
   (sequence (increment-all) coll)))

(defn filter-evens
  ([]
   (filter even?))
  ([coll]
   (sequence (filter-evens) coll)))

(defn double-all
  ([]
   (map #(* 2 %)))
  ([coll]
   (sequence (double-all) coll)))



(defn product
  ([coll]
   (reduce * 1 coll)))


(into [] (comp
           (increment-all)
           (filter-evens)
           (double-all))
  (range 10))

(->> (range 10)
  (map inc)
  (filter even?)
  (map #(* 2 %)))

(->> (range 10)
  (map inc)
  (filter even?)
  (map #(* 2 %))
  (reduce * 1))

(->> (range 10)
  (map inc)
  (filter even?)
  (map #(* 2 %))
  product)



(defn product
  ([] (completing *))
  ([coll]
   (reduce (product) 1 coll))
  ([xform coll]
   (transduce xform (product) 1 coll)))





;;; definitions
(defn increment-all
  ([]
   (map inc))
  ([coll]
   (sequence (increment-all) coll)))

(defn filter-evens
  ([]
   (filter even?))
  ([coll]
   (sequence (filter-evens) coll)))

(defn double-all
  ([]
   (map #(* 2 %)))
  ([coll]
   (sequence (double-all) coll)))



(defn product
  ([] (completing *))
  ([coll]
   (reduce (product) 1 coll))
  ([xform coll]
   (transduce xform (product) 1 coll)))


;; normal transformation
(->> (range 10)
  (increment-all)
  (filter-evens)
  (double-all)
  (product))
; 122880


;; transducer version
(def transducer
  (comp
    (increment-all)
    (filter-evens)
    (double-all)))

(product transducer (range 10))
; 122880

;; endregion


;; region ; building abstractions via transducers (Abhinav Omprakash)

(def students
  [{:student    {:name "Luke Skywalker"}
    :discipline "Jedi"}
   {:student    {:name "Hermione Granger"}
    :discipline "Magic"}])

(defn student-name [student]
  (get-in student [:student :name]))

(defn discipline [student]
  (:discipline student))




;; Displaying student names in the UI
(->> students
  (map student-name)
  (map clojure.string/capitalize))

;; sorting in the UI
(->> students
  (map student-name)
  sort)

;; generating slugs in the backend using a transducer
(->> students
  (into [] (comp
             (map student-name)
             (map clojure.string/lower-case)
             (map #(clojure.string/replace % #" " "-")))))





(defn student-names
  ([] (map student-name))
  ([students]
   (sequence (student-names) students)))

;; for the sake of completeness
(defn capitalize-names
  ([] (map clojure.string/capitalize))
  ([students]
   (sequence (capitalize-names) students)))

(defn lowercase-names
  ([] (map clojure.string/lower-case))
  ([students]
   (sequence (lowercase-names) students)))

(defn slugify-names
  ([] (map #(clojure.string/replace % #" " "-")))
  ([students]
   (sequence (slugify-names) students)))




;; Displaying student names in the UI
(->> students
  student-names
  capitalize-names)

;; sorting in the UI
(->> students
  student-names
  sort)

;; generating slugs in the backend using a transducer
(->> students
  (into [] (comp
             (student-names)
             (lowercase-names)
             (slugify-names))))


(def xd (comp (student-names) (capitalize-names) (slugify-names)))

(into [] xd students)

;; endregion


;; region ; now, to think about Kafka/Event-Handling

; workhorse functions, work on a single event
(defn compute-answer [[k event]]
  [k (assoc event :answer (reduce + (:inputs event)))])

(defn validate-event [[k event]]
  [k (assoc event :valid true)])

(defn authorize-event [[k event]]
  [k (assoc event :authorize true)])


; transducers, work on a collection of 0 or more events
(defn compute
  ([] (map compute-answer))
  ([coll]
   (sequence (compute) coll)))

(defn validate
  ([] (map validate-event))
  ([coll]
   (sequence (validate) coll)))

(defn authorize
  ([] (map authorize-event))
  ([coll]
   (sequence (authorize) coll)))


; wire together the "prep"
(def check (comp (validate) (authorize)))


; test inputs
(def events [[1 {:event 1 :inputs [1 2 3 4 5]}]
             [2 {:event 2 :inputs [10 20 30 40 50]}]])

(into [] (map compute-answer events))
(into [] (compute) events)


(into [] check [])
(into [] check [[100 {:event 100 :inputs [11 22 33 44 55]}]])
(into [] check events)
(transduce check conj events)


(into [] (comp check (compute)) [])
(into [] (comp check (compute)) [[100 {:event 100 :inputs [11 22 33 44 55]}]])
(into [] (comp check (compute)) events)
(transduce (comp check (compute)) conj events)


; build an "output" event from an "input" event
(defn output-event [[k {:keys [event answer]}]]
  [k {:event event :output answer}])

(defn c-o-c-event [[k event]]
  [k (assoc event :c-o-c "dummy-coc")])


(defn output
  ([] (map output-event))
  ([coll]
   (sequence (output) coll)))

(defn c-o-c
  ([] (map c-o-c-event))
  ([coll]
   (sequence (c-o-c) coll)))



(def build-output (comp (output) (c-o-c)))

(into [] build-output [[100 {:event 100 :answer 100}]])




; a more complete pipeline (check -> compute -> output)
(def event-pipeline (comp check (compute) build-output))

(transduce event-pipeline conj events)

;; endregion


;; region ; let's pretend a core/async channel is a kafka topic
(def topic (async/chan 1 event-pipeline))

(async/go
  (async/onto-chan! topic events))


(loop [n (async/<!! topic)]
  (when n
    (println n)
    (recur (async/<!! topic))))

;; endregion


;; region ; are we up to trying this with kafka for real?


; see https://github.com/DaveWM/willa
; and https://blog.davemartin.me/posts/kafka-streams-the-clojure-way/


; docker run --rm -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 -p 9581-9585:9581-9585 -p 9092:9092 -e ADV_HOST=localhost landoop/fast-data-dev:latest &


(def kafka-config
  {"application.id"            "kafka-rpl"
   "bootstrap.servers"         "localhost:9092"
   "default.key.serde"         "jackdaw.serdes.EdnSerde"
   "default.value.serde"       "jackdaw.serdes.EdnSerde"
   "cache.max.bytes.buffering" "0"})
(def serdes
  {:key-serde   (serde)
   :value-serde (serde)})

(def rpl-event-topic
  (merge {:topic-name         "rpl-event-topic"
          :partition-count    1
          :replication-factor 1
          :topic-config       {}}
    serdes))
(def rpl-answer-topic
  (merge {:topic-name         "rpl-answer-topic"
          :partition-count    1
          :replication-factor 1
          :topic-config       {}}
    serdes))

(defn make-event! [inputs]
  (let [id (rand-int 10000)]
    (with-open [producer (jc/producer kafka-config serdes)]
      @(jc/produce! producer
         rpl-event-topic
         id {:event id :inputs inputs}))))

(defn view-messages [topic]
  (with-open [consumer (jc/subscribed-consumer (assoc kafka-config
                                                 "group.id" (str (java.util.UUID/randomUUID)))
                         [topic])]
    (jc/seek-to-beginning-eager consumer)
    (->> (jcl/log-until-inactivity consumer 100)
      (map :value)
      doall)))


(def admin-client (ja/->AdminClient kafka-config))


(def event-pipeline (comp check (compute) build-output))


(defn event-topology [builder]
  (-> (js/kstream builder rpl-event-topic)
    (ws/transduce-stream event-pipeline)
    (js/to rpl-answer-topic)))


(defn start! []
  (let [builder (js/streams-builder)]
    (event-topology builder)
    (doto (js/kafka-streams builder kafka-config)
      (js/start))))

(defn stop! [kafka-streams-app]
  (js/close kafka-streams-app))


(comment
  (ja/create-topics! admin-client [rpl-event-topic rpl-answer-topic])

  (def microservice (start!))

  (view-messages rpl-event-topic)
  (view-messages rpl-answer-topic)

  (make-event! [15 20 25 30])
  (view-messages rpl-event-topic)
  (view-messages rpl-answer-topic)

  (make-event! [1 2 3 4 5 6 7 8 9 10])
  (view-messages rpl-event-topic)
  (view-messages rpl-answer-topic)


  (stop! microservice)

  (ja/delete-topics! admin-client [rpl-event-topic rpl-answer-topic])


  ())


;; endregion


;; region ; willa? (Dave Martin)

(def willa-topology
  {:entities {:topic/event-in        (assoc rpl-event-topic ::w/entity-type :topic)
              :stream/process-events {::w/entity-type :kstream
                                      ::w/xform       event-pipeline}
              :topic/answer-out      (assoc rpl-answer-topic ::w/entity-type :topic)}
   :workflow [[:topic/event-in :stream/process-events]
              [:stream/process-events :topic/answer-out]]})


(comment
  (ja/create-topics! admin-client [rpl-event-topic rpl-answer-topic])

  (def kafka-streams-app
    (let [builder (js/streams-builder)]
      (w/build-topology! builder willa-topology)
      (doto (js/kafka-streams builder kafka-config)
        (js/start))))


  (view-messages rpl-event-topic)
  (view-messages rpl-answer-topic)

  (make-event! [15 20 25 30])
  (view-messages rpl-event-topic)
  (view-messages rpl-answer-topic)

  (make-event! [1 2 3 4 5 6 7 8 9 10])
  (view-messages rpl-event-topic)
  (view-messages rpl-answer-topic)

  (stop! kafka-streams-app)

  (ja/delete-topics! admin-client [rpl-event-topic rpl-answer-topic])

  ())

;; endregion


;; region ; AstRecipes

; see https://www.astrecipes.net/blog/2016/11/24/transducers-how-to/


(defn plain-filter-odd [xf]
  (fn
    ;; START
    ([] (xf))
    ;; STOP
    ([result] (xf result))
    ;; PROCESS
    ([result input]
     (cond
       (odd? input) (xf result (inc input))                 ;; apply xf to result adding 1 to input
       :ELSE result))))                                     ;; do nothing - return result

(->> (range 10)
  (into [] plain-filter-odd))
(into [] plain-filter-odd (range 10))
; [2 4 6 8 10]



(defn filter-evens-1 [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result input]
     (cond
       (even? input) (xf result input)
       :ELSE result))))

(defn filter-evens-2
  ([]
   (filter even?))
  ([coll]
   (sequence (filter-evens-2) coll)))



(defn map-inc-1 [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result input] (xf result (inc input)))))


(into [] filter-evens-1 (range 10))
(into [] map-inc-1 (range 10))
(into [] (filter-evens-2) (range 10))

(def t (comp filter-evens-1 map-inc-1))
(into [] t (range 10))


;; endregion


;; region ; finally, we need the "service-def" parameter as context for the Kafka event pipeline

; see "Parametrized transducers" at https://www.astrecipes.net/blog/2016/11/24/transducers-how-to/

; need the context defined BEFORE we wire-up the transducer
(def context {:validate   :inputs :authorize :approved
              :output-key :output
              :c-o-c      "dummy-coc-09fa09"})



; transducers, work on a collection of 0 or more events
(defn compute [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k event]]
     (xf result [k (assoc event
                     :answer (reduce + (:inputs event)))]))))


(defn validate [ctx xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k event]]
     (xf result [k (assoc event
                     :valid (if ((:validate ctx) event)
                              true false))]))))


(defn authorize [ctx xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k event]]
     (xf result [k (assoc event
                     :auth (if (:authorize ctx)
                             (:authorize ctx) :reject))]))))


; wire together the "prep"
(def check (comp (partial validate context) (partial authorize context)))


; test inputs
(def events [[1 {:event 1 :inputs [1 2 3 4 5]}]
             [2 {:event 2 :inputs [10 20 30 40 50]}]])

(into [] compute events)


(:inputs {:event 1 :inputs [1 2 3 4 5]})
(if ((:validate {:validate :inputs}) {:event 1 :inputs [1 2 3 4 5]})
  true false)
(if ((:validate {:validate :inputs}) {:event 1})
  true false)

(into [] check [])
(into [] check [[100 {:event 100 :inputs [11 22 33 44 55]}]])
(into [] check [[100 {:event 100}]])
(into [] check events)
(transduce check conj events)


(into [] (comp check compute) [])
(into [] (comp check compute) [[100 {:event 100 :inputs [11 22 33 44 55]}]])
(into [] (comp check compute) events)
(transduce (comp check compute) conj events)

(->> events
  (into [] check))

; build an "output" event from an "input" event
;(defn output-event [[k {:keys [event answer]}]]
;  [k {:event event :output answer}])
;
;(defn c-o-c-event [[k event]]
;  [k (assoc event :c-o-c "dummy-coc")])


(defn output [ctx xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k {:keys [event answer]}]]
     (xf result [k {:event event (:output-key ctx) answer}]))))


(defn c-o-c [ctx xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k event]]
     (xf result [k (assoc event :c-o-c (:c-o-c ctx))]))))



(def build-output (comp (partial output context) (partial c-o-c context)))

(into [] build-output [[100 {:event 100 :answer 100}]])



; a more complete pipeline (check -> compute -> output)
(def event-pipeline (comp check compute build-output))

(transduce event-pipeline conj events)


;; endregion
