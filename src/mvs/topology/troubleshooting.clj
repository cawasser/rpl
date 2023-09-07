(ns mvs.topology.troubleshooting
  (:require [mvs.services :as s]
            [mvs.topics :as t]
            [mvs.read-models :as v]
            [mvs.dashboards :as d]))



(def topo {:mvs/messages {}

           :mvs/entities {}

           :mvs/workflow #{}})




(comment
  (require '[mvs.topology :as topo])

  (topo/view-topo topo)


  ())