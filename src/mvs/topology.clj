(ns mvs.topology
  (:require [loom.graph :as lg]
            [loom.io :as lio]
            [dorothy.core :as dot]
            [dorothy.jvm :as dj]
            [clojure.java.io :refer [file]]
            [clojure.java.shell :refer [sh]])
  (:import (java.io FileWriter
                    FileOutputStream)))


(defn- build-graph [nodes edges]
  (-> (lg/digraph)
    (#(apply lg/add-nodes % nodes))
    (#(apply lg/add-edges % edges))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; from https://github.com/aysylu/loom/blob/master/src/loom/io.clj
;
;    Copyright (C) 2010-2016 Aysylu Greenberg & Justin Kramer (jkkramer@gmail.com)
;
; Distributed under the Eclipse Public License, the same as Clojure.
;

;                         - Thanks!

(defn- os
  "Returns :win, :mac, :unix, or nil"
  []
  (condp
    #(<= 0 (.indexOf ^String %2 ^String %1))
    (.toLowerCase (System/getProperty "os.name"))
    "win" :win
    "mac" :mac
    "nix" :unix
    "nux" :unix
    nil))


(defn- open
  "Opens the given file (a string, File, or file URI) in the default
  application for the current desktop environment. Returns nil"
  [f]
  (let [f (file f)]
    ;; There's an 'open' method in java.awt.Desktop but it hangs on Windows
    ;; using Clojure Box and turns the process into a GUI process on Max OS X.
    ;; Maybe it's ok for Linux?
    (condp = (os)
      :mac (sh "open" (str f))
      :win (sh "cmd" (str "/c start " (-> f .toURI .toURL str)))
      :unix (sh "xdg-open" (str f)))
    nil))


(defn- open-data
  "Writes the given data (string or bytes) to a temporary file with the
  given extension (string or keyword, with or without the dot) and then open
  it in the default application for that extension in the current desktop
  environment. Returns nil"
  [data ext]
  (let [ext (name ext)
        ext (if (= \. (first ext)) ext (str \. ext))
        tmp (java.io.File/createTempFile (subs ext 1) ext)]
    (if (string? data)
      (with-open [w (java.io.FileWriter. tmp)]
        (.write w ^String data))
      (with-open [w (java.io.FileOutputStream. tmp)]
        (.write w ^bytes data)))
    (.deleteOnExit tmp)
    (open tmp)))

; endregion


(defn- label [text color font-color]
  {:color     color
   :label     text
   :fontcolor font-color
   :penwidth  3})


(defn view-topo [{:keys [mvs/messages mvs/entities mvs/workflow] :as topo}]
  (let [nodes (->> topo
                :mvs/entities
                (map (fn [[k {:keys [mvs/entity-type]}]]
                       [k (condp = entity-type
                            :mvs/topic {:shape :parallelogram :fillcolor :orange :style :filled}
                            :mvs/service {:shape :box :style :rounded}
                            :mvs/ktable {:shape :cylinder :fillcolor :green :style :filled}
                            :mvs/dashboard {:shape :doubleoctagon}
                            {:shape :component})]))
                (into []))
        edges (->> topo
                :mvs/workflow
                (map (fn [e] (conj (into [] (drop-last e))
                               (condp = (-> messages ((last e)) :mvs/message-type)
                                 :mvs/command (label (str (last e)) :blue :blue)
                                 :mvs/event (label (str (last e)) :orange :darkorange)
                                 :mvs/view (label (str (last e)) :seagreen :darkgreen)
                                 (label (str (last e)) :black :black)))))
                (into []))]

    (-> (dot/digraph
          (apply conj edges nodes))
      dot/dot
      (dj/render {:format :png})
      (open-data ".png"))))


(defn init-topology [topo]
  (let [entities (:mvs/entities topo)
        workflow (:mvs/workflow topo)]
    (doall
      (map (fn [[from to]]
             (when (= (-> entities from :mvs/entity-type) :mvs/topic)
               (do
                 (println "add-watch " from " -> " to)
                 (add-watch (-> entities from :mvs/topic-name)
                   to (-> entities to :mvs/name)))))
        workflow))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

(comment
  (view-topo-2 mvs.core/mvs-wiring)

  ())


; play with dorothy a bit
(comment
  (view-topo-2 mvs.core/mvs-wiring)

  ; try "parsing" a small topology
  (do
    (def topo {:mvs/entities {:provider-catalog-topic      {:mvs/entity-type :mvs/topic :mvs/topic-name :provider-catalog-topic}
                              :process-provider-catalog    {:mvs/entity-type :mvs/service :mvs/name :process-provider-catalog}
                              :process-available-resources {:mvs/entity-type :mvs/service :mvs/name :process-available-resources}
                              :resource-state-view         {:mvs/entity-type :mvs/ktable :mvs/topic-name :resource-state-view}}

               :mvs/workflow [[:provider-catalog-topic :process-provider-catalog]
                              [:provider-catalog-topic :process-available-resources]
                              [:process-available-resources :resource-state-view]]})

    (def nodes (->> (:mvs/entities topo)
                 (map (fn [[k {:keys [mvs/entity-type]}]]
                        [k (condp = entity-type
                             :mvs/topic {:shape :parallelogram}
                             :mvs/service {:shape :box}
                             :mvs/ktable {:shape :cylinder}
                             :else {:shape :component})]))
                 (into [])))
    (def edges (:mvs/workflow topo))

    (def g (dot/digraph (apply conj
                          (:mvs/workflow topo)
                          (->> (:mvs/entities topo)
                            (map (fn [[k {:keys [mvs/entity-type]}]]
                                   [k (condp = entity-type
                                        :mvs/topic {:shape :parallelogram}
                                        :mvs/service {:shape :box}
                                        :mvs/ktable {:shape :cylinder}
                                        :else {:shape :component})]))
                            (into []))))))

  (-> g dot/dot dj/show!)


  (:mvs/workflow topo)

  ())


; edge attributes
(comment
  (do
    (def topo mvs.core/mvs-wiring)
    (def messages (:mvs/messages topo))
    (def msg '(:?))                                         ;:provider/catalog))
    (def p (-> messages msg :mvs/message-type))
    (def from :from)
    (def to :to))

  (:provider/catalog messages)

  (-> messages msg)
  (-> messages msg :mvs/message-type)

  (->> topo
    :mvs/workflow)

  (->> topo
    :mvs/workflow
    (map #(->> % drop-last (into [])))
    (map (fn [e] (conj e {:color :blue})))
    (into []))

  (->> topo
    :mvs/workflow
    (map (fn [e] (conj (into [] (drop-last e))
                   {:color :blue})))
    (into []))

  (->> topo
    :mvs/workflow
    (map (fn [e] (conj (into [] (drop-last e))
                   (condp = (-> messages ((last e)) :mvs/message-type)
                     :mvs/comment {:color :blue}
                     :mvs/event {:color :orange}
                     :mvs/ktable {:color :green}
                     {:color :black}))))
    (into []))




  ())


; topo-2
(comment
  (let [nodes (->> topo
                :mvs/entities
                (map (fn [[k {:keys [mvs/entity-type]}]]
                       [k (condp = entity-type
                            :mvs/topic {:shape :parallelogram :fillcolor :orange :style :filled}
                            :mvs/service {:shape :box :style :rounded}
                            :mvs/ktable {:shape :cylinder :fillcolor :green :style :filled}
                            :mvs/dashboard {:shape :box}
                            {:shape :component})]))
                (into []))
        edges (->> topo
                :mvs/workflow
                (map (fn [e] (conj (into [] (drop-last e))
                               (condp = (-> messages ((last e)) :mvs/message-type)
                                 :mvs/command (label (str (last e)) :blue :blue)
                                 :mvs/event (label (str (last e)) :darkorange :darkorange)
                                 :mvs/view (label (str (last e)) :seagreen :darkgreen)
                                 (label (str (last e)) :black :black)))))
                (into []))]

    (-> (dot/digraph
          (apply conj edges nodes))
      dot/dot))
  ;(dj/render {:format :png})
  ;(open-data ".png")))


  ())


; endregion
