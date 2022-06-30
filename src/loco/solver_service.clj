(ns loco.solver-service
  (:require [clojure.spec.alpha :as s]
            [jackdaw.admin :as ja]
            [jackdaw.client :as jc]
            [jackdaw.client.log :as jcl]
            [jackdaw.serdes.edn :refer [serde]]
            [jackdaw.streams :as js]
            [loco.constraints :refer :all]
            [willa.core :as w]
            [loco.sudoku-solver :as ss]))


; combine a transducer pipeline (for streaming data) with
; a loco-based solver for a sudoku puzzle as part of a microservice
; topology (using willa)



;; region ; the transducers

(defn dummy-solve [[k event]]
  [k (assoc event
       :answer [42])])


(defn compute-xd [compute-fn xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result event]
     (xf result (compute-fn event)))))


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
    ([result [k event]]
     (xf result [k (-> event (dissoc :valid))]))))


(def pipeline (comp validate (partial compute-xd ss/compute-fn) output))
;(def pipeline (comp validate (partial compute-xd dummy-solve) output))


(comment
  (transduce (partial compute-xd ss/compute-fn) conj
    [[1 {:event 1 :puzzle ss/worlds-hardest-puzzle :valid true}]])
  (transduce (partial compute-xd ss/compute-fn) conj
    [[1 {:event 1 :puzzle ss/worlds-hardest-puzzle :valid false}]])


  (dummy-solve ss/worlds-hardest-puzzle)

  (transduce (partial compute-xd dummy-solve) conj
    [[1 {:event 1 :puzzle ss/worlds-hardest-puzzle :valid true}]])

  (into [] validate
    [[1 {:event 1 :puzzle ss/worlds-hardest-puzzle}]])
  (into [] output
    [[1 {:event  1 :puzzle ss/worlds-hardest-puzzle
         :answer (ss/compute-fn ss/worlds-hardest-puzzle)}]])

  (transduce pipeline conj
    [[99 {:event 99 :puzzle ss/worlds-hardest-puzzle}]
     [2 {:event 2 :puzzle ss/puzzle-1}]])


  (transduce pipeline conj
    [[1000 {:event 1000 :puzzle ss/broken-puzzle}]])


  (into [] validate
    [[1000 {:event 1000 :puzzle ss/broken-puzzle}]])

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


(def admin-client (ja/->AdminClient kafka-config))

(defn- get-solution! [puzzle]
  (let [id (rand-int 10000)]
    (with-open [producer (jc/producer kafka-config serdes)]
      @(jc/produce! producer
         rpl-puzzle-topic
         id {:event id :puzzle puzzle}))))

(defn- view-messages [topic]
  (with-open [consumer (jc/subscribed-consumer (assoc kafka-config
                                                 "group.id" (str (java.util.UUID/randomUUID)))
                         [topic])]
    (jc/seek-to-beginning-eager consumer)
    (->> (jcl/log-until-inactivity consumer 100)
      (map :value)
      doall)))



(def micro-service-pipeline (comp validate (partial compute-xd ss/compute-fn) output))


(def micro-service-def
  {:entities {:topic/event-in      (assoc rpl-puzzle-topic ::w/entity-type :topic)
              :stream/solve-puzzle {::w/entity-type :kstream
                                    ::w/xform       micro-service-pipeline}
              :topic/answer-out    (assoc rpl-solution-topic ::w/entity-type :topic)}
   :workflow [[:topic/event-in :stream/solve-puzzle]
              [:stream/solve-puzzle :topic/answer-out]]})



(defn start! []
  (let [builder (js/streams-builder)]
    (w/build-topology! builder micro-service-def)
    (doto (js/kafka-streams builder kafka-config)
      (js/start))))


(defn stop! [kafka-streams-app]
  (js/close kafka-streams-app))




(comment
  (ja/create-topics! admin-client [rpl-puzzle-topic rpl-solution-topic])

  (def kafka-streams-app (start!))

  (view-messages rpl-puzzle-topic)
  (view-messages rpl-solution-topic)

  (get-solution! ss/worlds-hardest-puzzle)
  (view-messages rpl-puzzle-topic)
  (view-messages rpl-solution-topic)

  (get-solution! ss/puzzle-1)
  (view-messages rpl-puzzle-topic)
  (view-messages rpl-solution-topic)

  (get-solution! ss/broken-puzzle)

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
; 1. what is the UI to create the "puzzle/event"
;     - is the "New" button part of the ui-component,
;     - or is it a separate component that publishes "show/hide" to the "edit" control
;
; 2. should we support both an async function in the Gateway (server) as well as an async "Event"
;     - how do we wire the "request" to the "reply"?
;
; 3. since we have the notion of :port/source and :port/sinks at the :ui-component level in the dag, and we
;    have the notion of :source/xxx at both the client and server level should we also have the
;    notion of :sink at the server level?
;     - so, should we have :sink/local and :sink/remote as viable entities/components
;     - what about associating the :sink/xxx value to a related :source/xxx value (async processing)
;       see also #2
;
; 4. what is the fundamental nature of what is happening "on the server"/"in the system"?
;     - does it look/work like a function? *should* it?!
;     - is it just the notion of the UI "requesting", "sending,"  "posting", or "writing" some data?
;          - how does the (potential) result "come back"? just a subscription?
;          - how does the "request" relate to the "reply/result"?
;          - if a "request" is semantically different from "post", do we really need 2 notions here?
;
; 5. how do we describe this/these concept(s) in the UI-component DAG?
;     - is this a "new" type (:fn/remote, maybe?)
;     - or do we define the "event" as a type (:event/remote, maybe?) (see also #3)
;
;
;
;
;
; let's layout what we've defined already:
;
;    1. :source/remote - a source of data that resides in the "system", identified by a universal ID, implements via
;                        a subscription to the "gateway server" as well as a re-frame subscription to :sources in the app-db
;    2. :source/local  - a source of data that resides within the implementation of the DAG on the client, implemented
;                        using re-frame subscription and event to app-db data
;    3. :source/fn     - a source of data computed by the "named function", function resides within the client
;                        currently implements via re-frame subscriptions wired into the app-db
;    4. :ui/component  - a ui component that visualizes data provided to its :port/sink(s), (allows editing via :port/source(s))
;
;  possible new "elements"
;
;    a. :sink/remote - a sink (destination, "posting") for data that resides in the "system", semantically this is just a
;                      value with no expectation of a "reply"
;    b. :sink/local  - a sink for data that resides only within the implementation DAG on the client,
;                      semantically this is just a value with no expectation of a "reply") might be mostly useful for testing
;    c. :fn/remote   - a sync/async function that resides in the "system", semantically this expects a
;                      "request" and produces a "reply"
;    d. :fn/local    - a sync/async function that resides in the DAG on the client, semantically this expects a
;                      "request" and produces a "reply"
;
;
; Q: should a :sink/local keep history (be a log) of all values or only the last value?
;
; Q: should we move to :fn/local for the "pink" boxes? but what about "real" synchronous functions?
;
; Q: what about stateFUL vs stateLESS functions? many :source/fn are stateless, but many are stateful.
;
;
;
;; endregion


;; region ; Thought Experiments
;
;; Experiment #1 - using :source/:sink

(def sudoku-solver-ui-1
  {:components {:ui/puzzle-list        {:type :ui/component :name :puzzle-list}
                :source/solved-puzzles {:type :source/remote :name :topic/solved-puzzles}
                :sink/puzzles-to-solve {:type :sink/remote :name :topic/puzzles-to-solve}}

   :links      {:topic/solved-puzzle {:value {:ui/puzzle-list :data}}
                :ui/puzzle-list      {:new-puzzle {:topic/puzzles-to-solve :data}}}})

(def sudoku-solver-service
  {:entities {:input/puzzles-to-solve {:name :topic/puzzles-to-solve ::w/entity-type :topic}
              :stream/solver          {::w/entity-type :kstream
                                       ::w/xform       :sudoku-pipeline}
              :output/solved-puzzles  {:name :topic/solved-puzzles :w/entity-type :topic}}

   :workflow [[:input/puzzles-to-solve :stream/solver
               :stream/solver :input/solved-puzzles]]})


;       sudoku-solver-ui-1             kafka-config                   sudoku-solver-service
;   ------------------------       ---------------------          ----------------------------
;
;    :source/solved-puzzles <----- :topic/solved-puzzles <-------- :output/solved-puzzles
;          |                                                                ^
;          V                                                                |
;    :ui/puzzle-list                                               :stream/solver (:sudoku-pipeline)
;          |                                                                ^
;          V                                                                |
;    :sink/puzzle-to-solve  -----> :topic/puzzle-to-solve  ------> :input/puzzles-to-solve




; Experiment #2 - using :fn/remote

(def sudoku-solver-ui-2
  {:components {:ui/puzzle-list        {:type :ui/component :name :puzzle-list}
                :source/solved-puzzles {:type :source/local :name :topic/solved-puzzles}
                :fn/puzzle-to-solve    {:type :fn/remote :url "/puzzle-to-solve"}}

   :links      {:fn/puzzle-to-solve    {:value {:source/solved-puzzles :data}}
                :source/solved-puzzles {:data {:ui/puzzle-list :data}}
                :ui/puzzle-list        {:new-puzzle {:fn/puzzles-to-solve :data}}}})

(def server-routes
  {"/puzzle-to-solve" micro-service-pipeline})


;       sudoku-solver-ui-2             server-routes
;   ------------------------       ---------------------
;
;    :source/solved-puzzles <----- "/puzzle-to-solve" (sudoku-pipeline)
;          |                             ^
;          V                            /
;    :ui/puzzle-list                   /
;          |                          /
;          V                         /
;    :fn/puzzle-to-solve  ----------/



;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;
;
;
; THIS raises the question:
;
;     Is a :fn/remote really the same thing as a :sink/remote->:source/remote pair, just implemented
;     with an asynchronous WebAPI call rather than event-streams/queues?
;
;   If we can argue that they ARE the same, then we don't need both, we just need a way to define how
;     each "side" interfaces with the actual implementation (the implementation gets defined separately)
;
;
;
;

;; endregion
