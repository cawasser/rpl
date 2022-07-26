(ns allocation.core
  (:require [willa.core :as w]
            [willa.viz :as wv]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [jackdaw.admin :as ja]
            [jackdaw.client :as jc]
            [jackdaw.client.log :as jcl]
            [jackdaw.streams :as js]
            [jackdaw.serdes.edn :refer [serde]]
            [allocation.topology :as topo]
            [allocation.topology-support :as s]
            [allocation.service-handler :as handler]
            [taoensso.timbre :as log]))


(def the-system (atom nil))
(def hand-system (atom nil))


(def kafka-config
  {"application.id"            "allocation-rpl"
   "bootstrap.servers"         "localhost:9092"
   "default.key.serde"         "jackdaw.serdes.EdnSerde"
   "default.value.serde"       "jackdaw.serdes.EdnSerde"
   "cache.max.bytes.buffering" "0"})
(def serdes
  {:key-serde   (serde)
   :value-serde (serde)})
(defn topic [name]
  (merge {:topic-name name
          :partition-count    1
          :replication-factor 1
          :topic-config       {}
          ::w/entity-type :topic}
    serdes))

;(def input-topic (topic "input-aoi"))
;(def output-topic (topic "output-allocation"))

(def input-topic (topic "aoi-updates"))
(def output-topic (topic "sensor-allocs-view"))


(defn- dummy-operation [message-def [k event]]
  (log/info "dummy-operation" k "//" event)
  [k (assoc event :processed-at (str (java.time.LocalDateTime/now)))])


;; region ; microservice

(def service-description
  {:service-name "sensor-allocations-w",
   :service-sha "<some git sha>",
   :inputs {"aoi-updates" {:name "aoi-updates",
                           :channel "aoi-updates",
                           :spec [:events/aoi-updates "aoi-updates-error"],
                           :key-set [:event-name :aois],
                           :forbidden #{},
                           :valid-as-of {:sha "<some git sha>", :date "<date>"}}},
   :outputs {"sensor-allocations" {:name "sensor-allocations",
                                   :channel "sensor-allocs-view",
                                   :spec [:views/sensor-allocations "sensor-allocations-error"],
                                   :key-set [:event-name :sensor-allocations],
                                   :forbidden #{},
                                   :valid-as-of {:sha "<some git sha>", :date "<date>"}}}})

#_(def handler (partial s/the-xd (partial dummy-operation {})))

(def handler (partial s/the-xd
               (partial handler/operation
                 (-> service-description
                   :outputs
                   first
                   val))))


(def micro-service-def
  (merge service-description
    {:service-def {:entities {:topic/event-in  (assoc input-topic ::w/entity-type :topic)
                              :topic/event-out (assoc output-topic ::w/entity-type :topic)
                              :stream/handler  {::w/entity-type :kstream
                                                ::w/xform handler}}
                   :workflow [[:topic/event-in :stream/handler]
                              [:stream/handler :topic/event-out]]}}))


(defn- new-system [service-def _]
  (component/system-map
    :topology (topo/map->Topology service-def)))


(defn- start-system! [service-def]
  (log/info "start!" service-def)
  (reset! the-system
    (component/start (new-system service-def {}))))


;; endregion


;; region ; directly start/stop the stream
(defn start! []
  (let [builder (js/streams-builder)]
    (w/build-topology! builder (:service-def micro-service-def))
    (doto (js/kafka-streams builder kafka-config)
      (js/start))))


(defn stop! [kafka-streams-app]
  (js/close kafka-streams-app))

;; endregion


;; region ; admin
(def admin-client (ja/->AdminClient kafka-config))


(defn- get-solution! [topic [k event]]
  (let [ts (str (java.time.LocalDateTime/now))]
    (with-open [producer (jc/producer kafka-config serdes)]
      @(jc/produce! producer
         topic
         (assoc k :timestamp ts)
         (assoc event :timestamp ts)))))


(defn- view-messages [topic]
  (with-open [consumer (jc/subscribed-consumer (assoc kafka-config
                                                 "group.id" (str (java.util.UUID/randomUUID)))
                         [topic])]
    (jc/seek-to-beginning-eager consumer)
    (->> (jcl/log-until-inactivity consumer 100)
      (map :value)
      doall)))

;; endregion


(comment
  (wv/view-topology (:service-def micro-service-def))

  (topo/map->Topology micro-service-def)
  (def tp (component/system-map
            :topology (topo/map->Topology micro-service-def)))
  (def tps (component/start tp))
  (:topology tp)
  (:topology tps)

  (ja/create-topics! admin-client [input-topic output-topic])

  (def kafka-streams-app (start!))

  (reset! hand-system (new-system micro-service-def {}))
  (:topology @hand-system)
  (def sys (component/start @hand-system))

  (:topology sys)

  (start-system! micro-service-def)

  (do
    (set-init (partial new-system micro-service-def))
    (start)
    (reset! the-system system))


  (keys @the-system)
  (get-in @the-system [:topology])
  (:topology @the-system)


  (view-messages input-topic)
  (view-messages output-topic)

  (get-solution! input-topic [{:event-name "aoi-updates"}
                              {:event-name "aoi-updates"
                               :aois {"charlie-hd" #{[7 9 "hidef-image" 1]
                                                     [7 8 "hidef-image" 2]
                                                     [6 7 "v/ir" 3]}}}])
  (view-messages input-topic)
  (view-messages output-topic)

  (stop! kafka-streams-app)

  (stop)

  (ja/delete-topics! admin-client [input-topic output-topic])

  ())


