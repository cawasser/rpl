(ns mvs.dashboard.ui.table
  (:require [mvs.dashboard.ui.table.cell :as cell]
            [cljfx.fx :refer :all]))


(defn header-cell [{:keys [column/name column/render
                           column/min-width column/max-width column/pref-width]
                    :as   cell-heading}]
  ;(println "header-cell" name render)

  (merge {:fx/type            :table-column
          :text               name
          :cell-value-factory identity
          :cell-factory       {:fx/cell-type :table-cell
                               :describe     ((render cell/reg) cell-heading)}}
    (when min-width {:min-width min-width})
    (when max-width {:max-width max-width})
    (when pref-width {:pref-width pref-width})))


(defn table-view [{:keys [data columns width height]}]
  ;(println "table-view" data)

  {:fx/type     :table-view
   :pref-width  width
   :pref-height height
   :columns     (map header-cell columns)
   :items       data})





