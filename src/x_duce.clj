(ns x-duce
  (:require [clojure.core.async :as async]
            [jackdaw.admin :as ja]
            [jackdaw.client :as jc]
            [jackdaw.client.log :as jcl]
            [jackdaw.serdes.edn :refer [serde]]
            [jackdaw.streams :as js]
            [willa.streams :refer [transduce-stream]]))


;; region ; let's start simple
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

(reduce + (range 1 11))


(reduce + (filter even? (range 1 11)))

(->> (range 1 11)
  (filter even?)
  (reduce +))


(reduce + (map #(* 2 %) (filter even? (range 1 11))))

(->> (range 1 11)                                           ; ->10
  (filter even?)                                            ; 10->5
  (map #(* 2 %))                                            ; 5->5
  (reduce +))                                               ; 5->1


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



; see https://www.grammarly.com/blog/engineering/building-etl-pipelines-with-clojure-and-transducers/


(reduce + 0 (into [] double-even-xforms numbers-data))
;(def x-form (comp (mapcat :parse-json-file-reducible)
;              (filter :valid-entry?)
;              (keep :transform-entry-if-relevant)
;              (partition-all 1000)
;              (map :save-into-database)))
(transduce double-even-xforms + 0 (range 1 11))
;(transduce x-form             (constantly nil) nil :files)


(def c (async/chan 1 double-even-xforms))

(async/go
  (async/onto-chan! c [5 6 8 12 15]))


(loop [n (async/<!! c)]
  (when n
    (println n)
    (recur (async/<!! c))))

;; endregion


;; region ; how do we make our own functions into transducers?

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


(def x-ducerrrrr
  (comp
    (increment-all)
    (filter-evens)
    (double-all)))

(into [] x-ducerrrrr (range 10))



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
   (map dec))
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





(def students
  [{:student-name "Luke Skywalker"
    :discipline   "Jedi"}
   {:student-name "Hermione Granger"
    :discipline   "Magic"}])

(defn student-name [student]
  (:student-name student))

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

;; endregion


;; region ; now, to think about Kafka/Event-Handling

; workhorse functions, work on a single event
(defn compute-answer [event]
  (assoc event :answer (reduce + (:inputs event))))

(defn validate-event [event]
  (assoc event :valid true))

(defn authorize-event [event]
  (assoc event :authorize true))


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
(def prep-events (comp (validate) (authorize)))


; test inputs
(def events [{:event 1 :inputs [1 2 3 4 5]}
             {:event 2 :inputs [10 20 30 40 50]}])

(map compute-answer events)
(into [] (compute) events)



; what about kafka-ike events (key/event)?
(defn compute-answer-k [[k event]]
  [k (assoc event :answer (reduce + (:inputs event)))])
(defn compute-k
  ([] (map compute-answer-k))
  ([coll]
   (sequence (compute-k) coll)))

(def kafka-events [[1 {:event 1 :inputs [1 2 3 4 5]}]
                   [2 {:event 2 :inputs [10 20 30 40 50]}]])

(map compute-answer-k kafka-events)
(into [] (compute-k) kafka-events)




(into [] prep-events [])
(into [] prep-events [{:event 100 :inputs [11 22 33 44 55]}])
(into [] prep-events events)
(transduce prep-events conj events)


(into [] (comp prep-events (compute)) [])
(into [] (comp prep-events (compute)) [{:event 100 :inputs [11 22 33 44 55]}])
(into [] (comp prep-events (compute)) events)
(transduce (comp prep-events (compute)) conj events)


; build an "output" event from an "input" event
(defn output-event [{:keys [event answer] :as all}]
  {:event event :output answer})

(defn c-o-c-event [event]
  (assoc event :c-o-c "dummy-coc"))


(defn output
  ([] (map output-event))
  ([coll]
   (sequence (output) coll)))

(defn c-o-c
  ([] (map c-o-c-event))
  ([coll]
   (sequence (c-o-c) coll)))



(def build-output (comp (output) (c-o-c)))

(into [] build-output [{:event 100 :answer 100}])




; a more complete pipeline (prep -> compute -> output)
(transduce (comp
             prep-events
             (compute)
             build-output)
  conj events)

;; endregion


;; region ; let's pretend a core/async channel is a kafka topic
(def topic (async/chan 1 (comp
                           prep-events
                           (compute)
                           build-output)))

(async/go
  (async/onto-chan! topic events))


(loop [n (async/<!! topic)]
  (when n
    (println n)
    (recur (async/<!! topic))))

;; endregion


;; region ; are we up to trying this with kafka for real?

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


(def event-pipeline (comp
                      prep-events
                      (compute)
                      build-output))

(defn event-topology [builder]
  (-> (js/kstream builder rpl-event-topic)
    (transduce-stream event-pipeline)
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
