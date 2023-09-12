(ns mvs.dashboard.monitoring
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.window :as w]
            [mvs.read-models :as rm]))


(def resource-monitoring-columns [{:column/id     :resource/id
                                   :column/name   "Resource ID"
                                   :column/render :cell/string}
                                  {:column/id         :measurements
                                   :column/name       "Measurements"
                                   :column/render     :cell/chart
                                   :column/pref-width 350}
                                  {:column/id         :performance
                                   :column/name       "Performance"
                                   :column/render     :cell/string
                                   :column/pref-width 100}
                                  {:column/id         :health
                                   :column/name       "Health"
                                   :column/render     :cell/string
                                   :column/pref-width 100}
                                  {:column/id         :usage
                                   :column/name       "Usage"
                                   :column/render     :cell/string
                                   :column/pref-width 100}])


(def last-presentation (atom nil))


(defn- resource-monitoring-table [{:keys [fx/context width height]}]
  (let [measurements (rm/resource-measurements context)
        performance  (rm/resource-performance context)
        usage        (rm/resource-usage context)
        presentation (doall
                       (into []
                         (map (fn [[id m]]
                                {:resource/id  id
                                 :measurements (into []
                                                 (map-indexed (fn [idx v]
                                                                {:fx/type :xy-chart-data
                                                                 :x-value idx
                                                                 :y-value v})
                                                   (get-in m [:resource/measurements
                                                              :googoo/metric])))
                                 :performance  (get-in performance [id :resource/performance
                                                                    :googoo/metric
                                                                    :performance/metric])
                                 :usage        (get usage id)})
                           measurements)))]

    (reset! last-presentation presentation)

    {:fx/type  :v-box
     :spacing  2
     :children [{:fx/type :label
                 :text    "Resource Metrics"
                 :style   {:-fx-font [:bold 20 :sans-serif]}}
                {:fx/type table/table-view
                 :width   width
                 :height  height
                 :columns resource-monitoring-columns
                 :data    presentation}]}))



(defn chart-view [{:keys [fx/context]}]
  (let [measurements (rm/resource-measurements context)
        presentation (into []
                       (mapcat (fn [[id m]]
                                 (map-indexed (fn [idx v]
                                                {:fx/type :xy-chart-data
                                                 :x-value idx
                                                 :y-value v})
                                   (get-in m [:resource/measurements
                                              :googoo/metric])))
                         measurements))]

    {:fx/type :line-chart
     :x-axis  {:fx/type :number-axis
               :label   "Time"}
     :y-axis  {:fx/type :number-axis
               :label   "Measurement"}
     :data    [{:fx/type :xy-chart-series
                :name    "Metric by time"
                :data    presentation}]}))


(defn dashboard [{:keys [fx/context x y width height]}]
  {:fx/type w/window
   :title   "Monitoring Dashboard"
   :x       x
   :y       y
   :width   width
   :height  height
   :content {:fx/type :split-pane
             :items   [{:fx/type resource-monitoring-table
                        :width   width
                        :height  height}]}})



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

(comment
  (do
    (def measurements (rm/resource-measurements (rm/state))))

  (map (fn [m]
         {:resource/id  "resource-1"
          :measurements (map-indexed (fn [idx v]
                                       {:fx/type :xy-chart-data
                                        :x-value idx
                                        :y-value v})
                          (get-in (val m) [:resource/measurements :googoo/metric]))})
    measurements)


  (as-> measurements m
    (first m)
    (val m)
    (get-in m [:resource/measurements :googoo/metric])
    (map-indexed (fn [idx v]
                   {:fx/type :xy-chart-data
                    :x-value idx
                    :y-value v})
      m))

  (map (fn [[id m]] {:id id :m m})
    measurements)


  (doall
    (map (fn [[id m]]
           {:resource/id  id
            :measurements (into []
                            (map-indexed (fn [idx v]
                                           {:fx/type :xy-chart-data
                                            :x-value idx
                                            :y-value v})
                              (get-in m [:resource/measurements
                                         :googoo/metric])))})
      measurements))


  (into []
    (mapcat (fn [[id m]]
              (map-indexed (fn [idx v]
                             {:fx/type :xy-chart-data
                              :x-value idx
                              :y-value v})
                (get-in m [:resource/measurements
                           :googoo/metric])))
      measurements))


  ())


; get performance metric for visualization
(comment
  (do
    (def context (rm/state))
    (def id #uuid"ae0862c1-5176-11ee-ad2e-f29ec83e6171")
    (def measurements (rm/resource-measurements context))
    (def performance (rm/resource-performance context)))

  (first measurements)
  (contains? (-> performance keys set) (ffirst measurements))

  (get-in performance [id
                       :resource/performance
                       :googoo/metric
                       :performance/metric])
  ())

; endregion

