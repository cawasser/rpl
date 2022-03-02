(ns composable-ui.coverage-plan
  "provide a composed UI for a \"Coverage Plan\" which shows targets and satellite coverage areas
  on a 3D globe"
  (:require [loom.graph :as lg]
            [loom.io :as lio]))



(defn subscribe-local [name]
  [:fn/subscribe-local name])
(defn subscribe-remote [name]
  [:fn/subscribe-remote name])
(defn publish [event value]
  [:fn/dispatch event value])

(defn h-box [& body]
  (into [] body))
(defn v-box [& body]
  (into [] body))



; assume the ui components have the following meta-data:
;
;      you PUBLISH using a :port/sink
;
;      you SUBSCRIBE via a :port/source
;
;      you do BOTH with :port/source-sink
;


; using keywords to make this simpler in a sandbox
;
(def meta-data {:ui/selectable-table {:component :component/selectable-table
                                      :ports     {:data      :port/source-sink
                                                  :selection :port/source}}

                :ui/globe            {:component :component/globe
                                      :ports     {:coverages    :port/source
                                                  :current-time :port/source}}

                :ui/slider           {:component :component/slider
                                      :ports     {:value :port/source-sink
                                                  :range :port/sink}}

                :ui/label            {:component :component/label
                                      :ports     {:value :port/sink}}})



; we can define the "Coverage Plan" as:
;
;    note: make-coverage and make-range are functions (in this namespace)
;

(defn fn-coverage [& {:keys []}]
  [])
(defn fn-range [& {:keys []}]
  [])



(def composite-def
  {:title        "Coverage Plan"
   :component-id :coverage-plan
   :components   {; ui components
                  :ui/targets                {:type :ui/component :name :table/selectable-table}
                  :ui/satellites             {:type :ui/component :name :table/selectable-table}
                  :ui/globe                  {:type :ui/component :name :globe/three-d-globe}
                  :ui/time-slider            {:type :ui/component :name :slider/slider}
                  :ui/current-time           {:type :ui/component :name :label/label}

                  ; remote data sources
                  :topic/target-data         {:type :source/remote :name :source/targets}
                  :topic/satellite-data      {:type :source/remote :name :source/satellites}
                  :topic/coverage-data       {:type :source/remote :name :source/coverages}

                  ; composite-local data sources
                  :topic/selected-targets    {:type :source/local :name :selected-targets}
                  :topic/selected-satellites {:type :source/local :name :selected-satellites}
                  :topic/current-time        {:type :source/local :name :current-time}
                  :topic/selected-coverages  {:type :source/local :name :selected-coverages}
                  :topic/time-range          {:type :source/local :name :time-range}

                  ; transformation functions
                  :fn/coverage               {:type  :source/fn
                                              :name  fn-coverage
                                              :ports {:targets    :port/sink
                                                      :satellites :port/sink
                                                      :coverages  :port/sink
                                                      :selected   :port/source}}
                  :fn/range                  {:type  :source/fn
                                              :name  fn-range
                                              :ports {:data  :port/sink
                                                      :range :port/source}}}

   :links        {; ui components
                  :ui/targets      {:data      :topic/target-data
                                    :selection :topic/selected-targets}
                  :ui/satellites   {:data      :topic/satellite-data
                                    :selection :topic/selected-satellites}
                  :ui/globe        {:coverages :topic/selected-coverages
                                    :time      :topic/current-time}
                  :ui/time-slider  {:time  :topic/current-time
                                    :range :topic/time-range}
                  :ui/current-time {:value :topic/current-time}

                  ; transformation functions
                  :fn/coverage     {:targets    :topic/selected-targets
                                    :satellites :topic/selected-satellites
                                    :coverages  :topic/coverage-data
                                    :selected   :topic/selected-coverages}
                  :fn/range        {:data  :topic/coverages
                                    :range :topic/time-range}}

   :layout       [v-box
                  [h-box
                   [v-box [:ui/targets] [:ui/satellites] [:ui/time-slider]]
                   [v-box [:ui/globe] [:ui/current-time]]]]})


; we want to turn the composite-def into things like...
;
(comment
  {:fn/coverage   (make-coverage
                    :targets (subscribe :source/local :topic/selected-targets)
                    :satellites (subscribe :source/local :topic/selected-satellites)
                    :coverages (subscribe :source/remote :topic/coverages)
                    :selected (publish :source/local :topic/selected-coverages))


   :fn/range      (make-range
                    :data (subscribe :source/remote :topic/coverages)
                    :selected (publish :source/local :topic/time-range))

   :ui/targets    [selectable-table
                   :component-id :coverage-plan/targets
                   :container-id :coverage-plan
                   :data (pub-sub :source/remote :topic/target-data)
                   :selected (publish :source/local :topic/selected-targets)]

   :ui/satellites [selectable-table
                   :component-id :coverage-plan/satellites
                   :container-id :coverage-plan
                   :data (pub-sub :source/remote :topic/satellite-data)
                   :selected (publish :source/local :topic/selected-targets)]}

  :ui/globe [globe
             :component-id :coverage-plan/globe
             :container-id :coverage-plan
             :coverages (subscribe :source/local :topic/selected-coverages)
             :current-time (subscribe :source/local :topic/current-time)]

  :ui/time-slider [slider
                   :component-id :coverage-plan/slider
                   :container-id :coverage-plan
                   :value (pub-sub :source/local :topic/current-time)
                   :range (subscribe :source/local :topic/time-range)]

  :ui/current-time [label
                    :component-id :coverage-plan/label
                    :container-id :coverage-plan
                    :value (subscribe :source/local :topic/current-time)]

  ())


;;;;;;;;;;
;;;;;;;;;;
;
;  We'll use multi-methods to convert the component types into the correct "code"
;
;;;;;;;;;;
;;;;;;;;;;
;; region

(defmulti component->ui (fn [{:keys [type]}]
                          type))

(defmethod component->ui :ui/component [{:keys [name]}]
  [name])

(defmethod component->ui :source/local [{:keys [name]}]
  (subscribe-local name))

(defmethod component->ui :source/remote [{:keys [name]}]
  (subscribe-local name))

(defmethod component->ui :source/fn [{:keys [name ports]}]
  [name ports])
;; endregion



; basics of Loom (https://github.com/aysylu/loom)
(comment
  (do
    (def g (lg/graph [1 2] [2 3] {3 [4] 5 [6 7]} 7 8 9))
    (def dg (lg/digraph g))
    (def wg (lg/weighted-graph {:a {:b 10 :c 20} :c {:d 30} :e {:b 5 :d 5}}))
    (def wdg (lg/weighted-digraph [:a :b 10] [:a :c 20] [:c :d 30] [:d :b 10]))
    (def fg (lg/fly-graph :successors range :weight (constantly 77))))


  (lg/nodes g)
  (lg/edges g)
  (lg/has-node? g 5)
  (lg/weighted-graph g)

  (lg/nodes fg)

  ())


; how do we use Loom for our composite?
;
(comment
  ; a Loom digraph only needs EDGES (:links)
  (def edges (->> composite-def
               :links
               (mapcat (fn [[k v]]
                         (map (fn [[name type]]
                                [k type])
                           v)))
               (into [])))


  ; with THIS set of edges, sources and sinks all look like successors
  (def g (apply lg/digraph edges))
  (lio/view g)


  ;; region
  ; we need a way to turn the sources into predecessors
  ;
  ; two options:
  ;    1. change the format of :links to include the sources as keys (more like willa)
  ;
  ; essentially, this means we are only interested in what a :component "PUBLISHES TO"
  ;
  ;                also,  we'll swap the "target" part to be "target, then port-id"
  ;
  ; so, something like this:
  ;
  (def links-2 {:links {; ui components publish to what?
                        :ui/targets                {:topic/target-data      :data
                                                    :topic/selected-targets :selection}
                        :ui/satellites             {:topic/satellite-data      :data
                                                    :topic/selected-satellites :selection}
                        :ui/time-slider            {:topic/current-time :value}

                        ; transformation functions publish to what?
                        :fn/coverage               {:topic/selected-coverages :selected}
                        :fn/range                  {:topic/time-range :range}

                        ; topics are inputs into what?
                        :topic/target-data         {:ui/targets :data}
                        :topic/satellite-data      {:ui/satellites :data}
                        :topic/selected-targets    {:fn/coverage :targets}
                        :topic/selected-satellites {:fn/coverage :satellites}
                        :topic/coverage-data       {:fn/coverage :coverages
                                                    :fn/range    :data}
                        :topic/selected-coverages  {:ui/globe :coverages}
                        :topic/current-time        {:ui/current-time :value
                                                    :ui/time-slider  :value
                                                    :ui/globe        :current-time}
                        :topic/time-range          {:ui/time-slider :range}}})

  (def g2 (apply lg/digraph (->> links-2
                              :links
                              (mapcat (fn [[k v]]
                                        (map (fn [[target port :as all]]
                                               [k target])
                                          v)))
                              (into []))))
  (lio/view g2)




  ; OR
  ;    2. additional transformations to get the data into that kind of format
  ;
  ; so, THIS:
  ;
  (def links3 {:ui/targets {:data      :topic/target-data
                            :selection :topic/selected-targets}})

  ; turns into THIS:
  ;
  (def expanded-links3 {:ui/targets             {:topic/target-data}
                        :topic/target-data      {:ui/targets :data}
                        :topic/selected-targets {:ui/targets :selection}})

  ; we'll need to look up the port-types in the meta-data
  ;    (or we can do ANOTHER pre-step and mix that into the components, so we only
  ;     have one place to look for the port-type)
  ;


  (defn expand-links [links])


  ;; endregion


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;
  ; SUMMARY:
  ;
  ; Option 1 is easier if we use a builder tool, since it already has the Digraph (that's what the user
  ;         actually builds, but harder to do by hand
  ;
  ; while
  ;
  ; Option 2 is easier to build by hand, but require some complex logic to "reverse engineer"
  ;         the actual Digraph out of the partial graph we write by hand.
  ;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


  ())
