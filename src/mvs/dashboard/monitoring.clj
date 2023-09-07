(ns mvs.dashboard.monitoring
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.window :as w]
            [mvs.read-model.resource-measurements-view :as mv]))


(def resource-monitoring-columns [{:column/id     :resource/id
                                   :column/name   "Resource ID"
                                   :column/render :cell/string}
                                  {:column/id     :measurements
                                   :column/name   "Chart"
                                   :column/render :cell/chart
                                   :column/pref-width    150}])


(def last-presentation (atom nil))


(defn- resource-monitoring-table [{:keys [fx/context width height]}]
  (let [measurements (mv/resource-measurements context)
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
                                                              :googoo/metric])))})
                           measurements)))]

    (reset! last-presentation presentation)

    {:fx/type  :v-box
     :spacing  2
     :children [{:fx/type :label
                 :text    "Provider Catalogs"
                 :style   {:-fx-font [:bold 20 :sans-serif]}}
                {:fx/type table/table-view
                 :width   width
                 :height  height
                 :columns resource-monitoring-columns
                 :data    presentation}]}))



(defn chart-view [{:keys [fx/context]}]
  (let [measurements (mv/resource-measurements context)
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
                :name    "Position by time"
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
                        :height  height}
                       {:fx/type chart-view
                        :width   width
                        :height  height}]}})



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

(comment
  (do
    (def measurements (mv/resource-measurements @mvs.read-models/app-db)))

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

; endregion

