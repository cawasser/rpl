(ns system-definition.core
  (:require [loom.graph :as lg]
            [loom.gen :as lgen]
            [loom.io :as lio]
            [loom.alg :as lalg]))


(def system-def {:services ["vanilla"
                            "aoi-state-srv" "platform-state-srv" "req-state-srv"
                            "sensor-allocation" "comms-allocation" "comms-commit"]
                 :events   ["aoi-updated" "platform-updated" "req-updated" "req-committed"]
                 :views    ["aoi-state" "platform-state" "req-state" "comms-state"
                            "est-sensor-state" "est-comms-state"]})

(def services {"vanilla"            {:inputs    ["aoi-state" "platform-state" "req-state" "comms-state"
                                                 "est-sensor-state" "est-comms-state"]
                                     :coeffects []
                                     :outputs   []}
               "aoi-state-srv"      {:inputs    ["aoi-updated"]
                                     :coeffects []
                                     :outputs   ["aoi-state"]}
               "platform-state-srv" {:inputs    ["platform-updated"]
                                     :coeffects []
                                     :outputs   ["platform-state"]}
               "req-state-srv"      {:inputs [] :coeffects [] :outputs []}
               "est-sensor-state"   {:inputs [] :coeffects [] :outputs []}
               "sensor-allocation"  {:inputs    ["aoi-state"] ;"platform-state"]
                                     :coeffects ["aoi-state" "platform-state"]
                                     :outputs   ["est-sensor-state"]}
               "comms-allocation"   {:inputs [] :coeffects [] :outputs []}
               "comms-commit"       {:inputs [] :coeffects [] :outputs []}})

(def events {"aoi-updated"      {:channel "aoi"}
             "platform-updated" {:channel "platform"}
             "req-updated"      {:channel "req"}
             "req-committed"    {:channel "req"}})

(def views {"aoi-state"        {:channel "aoi"}
            "platform-state"   {:channel "platform"}
            "req-state"        {:channel "req"}
            "comms-state"      {:channel "commms"}
            "est-sensor-state" {:channel "sensor"}
            "est-comms-state"  {:channel "comms"}})

(def channels {"aoi" {:provider :kafka}
               "platform" {:provider :kafka}
               "req" {:provider :kafka}
               "comms" {:provider :kafka}
               "sensor" {:provider :kafka}})

; "providers" is where all the configuration goes
(def providers {:kafka {}})

; "identities" is where all the passwords and such (that should NEVER be in a repo) go
(def secrets {:kafka {:id "" :password ""}})


(:services system-def)
(:events system-def)
(:views system-def)

(defn get-channel-def-from-edn [channel]
  (get channels channel))

(defn get-message-def-from-edn [message]
  {name
    (or
      (get events message)
      (get views message))})
(get-message-def-from-edn "aoi-updated")

(defn get-service-def-from-edn [service]
  (get services service))


(map (fn [name]
       (let [s-def (get-service-def-from-edn name)]
         {:service name
          :inputs (into {} (map get-message-def-from-edn (:inputs s-def)))
          :coeffects (into {} (map get-message-def-from-edn (:coeffects s-def)))
          :outputs (into {} (map get-message-def-from-edn (:outputs s-def)))}))
  (:services system-def))




; play with loom
(def g (lg/graph [1 2] [2 3] {3 [4] 5 [6 7]} 7 8 9))
(def dg (lg/digraph g))
(def wg (lg/weighted-graph {:a {:b 10 :c 20} :c {:d 30} :e {:b 5 :d 5}}))
(def wdg (lg/weighted-digraph [:a :b 10] [:a :c 20] [:c :d 30] [:d :b 10]))
(def rwg (lgen/gen-rand (lg/weighted-graph) 10 20 :max-weight 100))
(def fg (lg/fly-graph :successors range :weight (constantly 77)))


(def server [[:kafka :server] [:websocket :server] [:database :server]])
(def system (concat [:kafka]
              [:websocket]
              [:database]
              server
              [[:server :nrepl]]
              [[:server :scheduler]]
              [[:websocket :moitoring]]))
(apply prn system)

(def g (lg/digraph
         :kafka
         :websocket
         :database
         server
         ;[:kafka :server]
         ;[:websocket :server]
         ;[:database :server]
         [:server :nrepl]
         [:server :scheduler]
         [:websocket :moitoring]))

(lalg/connected-components g)

(lio/view g)
(lio/view dg)
(lio/view wg)
(lio/view wdg)
(lio/view rwg)
(lio/view fg)

(lio/view (lg/weighted-graph))

(def hhgt (lg/graph [1 2] [2 3] [3 4] {5 [6 9]} 7 8))

(lio/view hhgt)
(lalg/connected-components hhgt)

(lio/view (lg/weighted-digraph {:a {:b 10 :c 20} :c {:d 30} :e {:b 5 :d 5}}))

