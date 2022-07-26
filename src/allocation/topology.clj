(ns allocation.topology
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [jackdaw.streams :as js]
            [willa.core :as w]))


(def kafka-config
  {"bootstrap.servers"         "localhost:9092"
   "default.key.serde"         "jackdaw.serdes.EdnSerde"
   "default.value.serde"       "jackdaw.serdes.EdnSerde"
   "cache.max.bytes.buffering" "0"})


(defrecord Topology [service-def service-name service-sha
                     topology]
  component/Lifecycle
  (start [component]
    (log/info "starting topology" service-name
      "//" service-sha
      "//" service-def)

    (if service-def
      (let [builder (js/streams-builder)
            config  (assoc kafka-config "application.id" service-name)]

        (log/info "Topology config" config)

        (w/build-topology! builder service-def)
        (assoc component :topology
          (doto (js/kafka-streams builder config)
            (js/start))))
      (do
        (log/info "ERROR!!!! No topology definition provided!")
        component)))

  (stop [component]
    (log/info "stopping topology" topology)

    (js/close topology)
    (assoc component :topology nil)))
