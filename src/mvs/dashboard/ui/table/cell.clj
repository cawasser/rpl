(ns mvs.dashboard.ui.table.cell
  (:require [mvs.dashboard.ui.medusa-gauge :as g]))



(defn- string-cell [{:keys [column/id]}]
  (fn [x]
    {:text (str (id x))}))


(defn- color-cell [_]
  (fn [x]
    {:style {:-fx-background-color
             (:color x)}}))


(defn chart-cell [{:keys [column/id]}]
  (fn [x]
    {:text    ""
     :graphic {:fx/type        :line-chart
               :pref-height    50
               ;:min-height     50
               ;:max-height     50
               :animated       false
               :legend-visible false
               :x-axis         {:fx/type :number-axis}
               :y-axis         {:fx/type :number-axis}
               :data           [{:fx/type :xy-chart-series
                                 :data    (id x)}]}}))



(defn gauge-cell [{:keys [column/id gauge/skin]}]
  (fn [x]
    (println "gauge-cell" id (id x))
    {:text    ""
     :graphic {:fx/type   g/lifecycle
               :title     "Usage"
               :skin-type skin
               :value     (or (id x) 0)}}))


(def reg {:cell/string #'string-cell
          :cell/color  #'color-cell
          :cell/chart  #'chart-cell
          :cell/gauge  #'gauge-cell})

