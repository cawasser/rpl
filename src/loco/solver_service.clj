(ns loco.solver-service
  (:require [clojure.spec.alpha :as s]
            [loco.constraints :refer :all]
            [loco.core :as l]
            [jackdaw.admin :as ja]
            [jackdaw.client :as jc]
            [jackdaw.client.log :as jcl]
            [jackdaw.serdes.edn :refer [serde]]
            [jackdaw.streams :as js]
            [willa.streams :as ws]
            [willa.core :as w]))


; combine a transducer pipeline (for streaming data) with
; a loco-based solver for a sudoku puzzle as part of a microservice
; topology (using willa)


;; region ; setup

(def worlds-hardest-puzzle
  [[8 0 0 0 0 0 0 0 0]
   [0 0 3 6 0 0 0 0 0]
   [0 7 0 0 9 0 2 0 0]
   [0 5 0 0 0 7 0 0 0]
   [0 0 0 0 4 5 7 0 0]
   [0 0 0 1 0 0 0 3 0]
   [0 0 1 0 0 0 0 6 8]
   [0 0 8 5 0 0 0 1 0]
   [0 9 0 0 0 0 4 0 0]])

; see https://imprimesudoku.blogspot.com/2014/07/medium-sudoku-21-30.html
(def puzzle-1
  [[0 0 8   0 0 0   5 0 0]
   [0 5 0   1 0 0   0 6 7]
   [0 2 7   5 0 0   3 0 8]

   [7 0 0   3 0 1   0 0 0]
   [2 1 5   0 0 0   6 8 3]
   [0 0 0   8 0 5   0 0 9]

   [5 0 4   0 0 9   7 3 0]
   [8 3 0   0 0 7   0 4 0]
   [0 0 2   0 0 0   8 0 0]])
(def broken-puzzle
  [[0 0 8   0 0 0   5 0 0]])


(s/def :puzzle/cell number?)
(s/def :puzzle/row (s/coll-of :puzzle/cell))
(s/def :puzzle/puzzle (s/coll-of :puzzle/row))

(comment
  (s/valid? :puzzle/cell 5)
  (s/valid? :puzzle/cell -)
  (s/valid? :puzzle/cell "-")

  (s/valid? :puzzle/row [8 0 0 0 0 0 0 0 0])
  (s/valid? :puzzle/row [8 0 0 :keyword 0 0 0 0 0])
  (s/valid? :puzzle/row {:row 0 :cell 0 :x 8})
  (s/valid? :puzzle/row "invalid")

  (s/valid? :puzzle/puzzle worlds-hardest-puzzle)
  (s/valid? :puzzle/puzzle puzzle-1)

  ())

;; endregion


;; region ; sudoku solver


(defn one-number-per-square-l []
  (for [i (range 9) j (range 9)]
    ($in [:grid i j] 1 9)))


(defn each-number-once-per-row-l []
  (for [i (range 9)]
    ($distinct (for [j (range 9)] [:grid i j]))))


(defn each-number-once-per-column-l []
  (for [j (range 9)]
    ($distinct (for [i (range 9)] [:grid i j]))))


(defn each-number-once-per-box-l []
  (for [section1 [[0 1 2] [3 4 5] [6 7 8]]
        section2 [[0 1 2] [3 4 5] [6 7 8]]]
    ($distinct (for [i section1, j section2] [:grid i j]))))


(def basic-model
  (concat
    ; range-constraints
    (one-number-per-square-l)

    ; row-constraints
    (each-number-once-per-row-l)

    ; col-constraints
    (each-number-once-per-column-l)

    ; section-constraints
    (each-number-once-per-box-l)))


(defn solve [grid]
  (first
    (l/solve
      (concat basic-model
        (for [i (range 9), j (range 9)
              :let [hint (get-in grid [i j])]
              :when (and (number? hint) (contains? (set (range 1 10)) hint))]
          ($= [:grid i j] hint))))))


(defn ->solution [sol]
  (into []
    (for [row (->> sol
                (map (fn [[[_ r c] val]]
                       {:r r :c c :x val}))
                (sort-by (juxt :r :c))
                (partition-by :r))]
      (->> (map #(or (:x %) 0) row)
        (into [])))))



(comment
  (solve worlds-hardest-puzzle)

  ; => {[:grid 2 2] 5,
  ;     [:grid 0 2] 2,
  ;     [:grid 2 8] 3,
  ;     [:grid 4 7] 2,
  ;     [:grid 6 2] 1,
  ;     [:grid 4 2] 9,
  ;     [:grid 7 1] 3,
  ;     [:grid 0 4] 5,
  ;     [:grid 8 6] 4,
  ;     [:grid 7 6] 9,
  ;     [:grid 7 0] 4,
  ;     etc...


  (def s (solve worlds-hardest-puzzle))
  (solve puzzle-1)

  (->solution s)
  (->solution [])

  (def r (into []
           (for [row (->> s
                       (map (fn [[[_ r c] val]]
                              {:r r :c c :x val}))
                       (sort-by (juxt :r :c))
                       (partition-by :r))]
             (->> (map #(or (:x %) -) row)
               (into [])))))


  (->> s
    (map (fn [[[_ r c] val]]
           {:r r :c c :x val}))
    (sort-by (juxt :r :c))
    (partition-by :r))

  ())

;; endregion


;; region ; the transducers

(defn compute [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k {:keys [puzzle valid] :as event}]]
     (println "valid?" valid)
     (xf result [k (assoc event
                     :answer (if valid
                               (solve puzzle)
                               []))]))))


(defn validate [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k {:keys [puzzle] :as event}]]
     (xf result [k (assoc event
                     :valid (s/valid? :puzzle/puzzle puzzle))]))))


(defn output [xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result [k {:keys [answer] :as event}]]
     (xf result [k (-> event
                     (assoc :solution (->solution answer))
                     (dissoc :answer :valid))]))))


(def pipeline (comp validate compute output))


(comment
  (transduce compute conj
    [[1 {:event 1 :puzzle worlds-hardest-puzzle :valid true}]])
  (transduce compute conj
    [[1 {:event 1 :puzzle worlds-hardest-puzzle :valid false}]])
  (into [] validate
    [[1 {:event 1 :puzzle worlds-hardest-puzzle}]])
  (into [] output
    [[1 {:event 1 :puzzle worlds-hardest-puzzle
         :answer (solve worlds-hardest-puzzle)}]])

  (transduce pipeline conj
    [[99 {:event 99 :puzzle worlds-hardest-puzzle}]
     [2 {:event 2 :puzzle puzzle-1}]])


  (transduce pipeline conj
    [[1000 {:event 1000 :puzzle broken-puzzle}]])


  (into [] validate
    [[1000 {:event 1000 :puzzle broken-puzzle}]])

  ())

;; endregion


;; region ; willa-based microservice

; docker run --rm -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 -p 9581-9585:9581-9585 -p 9092:9092 -e ADV_HOST=localhost landoop/fast-data-dev:latest &


(def kafka-config
  {"application.id"            "sudoku-rpl"
   "bootstrap.servers"         "localhost:9092"
   "default.key.serde"         "jackdaw.serdes.EdnSerde"
   "default.value.serde"       "jackdaw.serdes.EdnSerde"
   "cache.max.bytes.buffering" "0"})
(def serdes
  {:key-serde   (serde)
   :value-serde (serde)})
(def rpl-puzzle-topic
  (merge {:topic-name         "rpl-puzzle-topic"
          :partition-count    1
          :replication-factor 1
          :topic-config       {}}
    serdes))
(def rpl-solution-topic
  (merge {:topic-name         "rpl-solution-topic"
          :partition-count    1
          :replication-factor 1
          :topic-config       {}}
    serdes))


(defn stop! [kafka-streams-app]
  (js/close kafka-streams-app))

(def admin-client (ja/->AdminClient kafka-config))

(defn get-solution! [puzzle]
  (let [id (rand-int 10000)]
    (with-open [producer (jc/producer kafka-config serdes)]
      @(jc/produce! producer
         rpl-puzzle-topic
         id {:event id :puzzle puzzle}))))

(defn view-messages [topic]
  (with-open [consumer (jc/subscribed-consumer (assoc kafka-config
                                                 "group.id" (str (java.util.UUID/randomUUID)))
                         [topic])]
    (jc/seek-to-beginning-eager consumer)
    (->> (jcl/log-until-inactivity consumer 100)
      (map :value)
      doall)))


(def sudoku-pipeline (comp validate compute output))


(def sudoku-service
  {:entities {:topic/event-in        (assoc rpl-puzzle-topic ::w/entity-type :topic)
              :stream/solve-puzzle {::w/entity-type :kstream
                                    ::w/xform       sudoku-pipeline}
              :topic/answer-out      (assoc rpl-solution-topic ::w/entity-type :topic)}
   :workflow [[:topic/event-in :stream/solve-puzzle]
              [:stream/solve-puzzle :topic/answer-out]]})


(comment
  (ja/create-topics! admin-client [rpl-puzzle-topic rpl-solution-topic])

  (def kafka-streams-app
    (let [builder (js/streams-builder)]
      (w/build-topology! builder sudoku-service)
      (doto (js/kafka-streams builder kafka-config)
        (js/start))))


  (view-messages rpl-puzzle-topic)
  (view-messages rpl-solution-topic)

  (get-solution! worlds-hardest-puzzle)
  (view-messages rpl-puzzle-topic)
  (view-messages rpl-solution-topic)

  (get-solution! puzzle-1)
  (view-messages rpl-puzzle-topic)
  (view-messages rpl-solution-topic)

  (get-solution! broken-puzzle)

  (stop! kafka-streams-app)

  (ja/delete-topics! admin-client [rpl-puzzle-topic rpl-solution-topic])

  ())

;; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;; region ; now to make this a "real" microservice, perhaps as part RCCST or Rocky-Road.
;
;
; ISSUES:
;
; - what is the UI to create the "puzzle/event"
;     - is the "New" button part of the ui-component,
;     - or is it a separate component that publishes "show/hide" to the "edit" control
;
; - should we support both an async function in the Gateway (server) as well as an async "Event"
;     - how do we wire the "request" to the "reply"?
;
; - what is the fundamental nature of what is happening "on the server"/"in the system"?
;     - does it look/work like a function?
;     - is it just the notion of the UI "requesting", "sending" or "writing" some data?
;          - how does the (potential) result "come back"? just a subscription?
;          - how does the "request" to the "reply/result"?
;
; - how do we describe this/these concept(s) in the UI-component DAG?
;     - is this a "new" type (:fn/remote, maybe?)
;     - or do we define the "event" as a type (:event/remote, maybe?)
;
;
;
;
;
;; endregion

