(ns mvs.dashboard.ui.table.cell)



(defn string-cell [{:keys [column/id]}]
  (fn [x]
    {:text (str (id x))}))


(defn color-cell [_]
  (fn [x]
    {:style {:-fx-background-color
             (:color x)}}))


(def reg {:cell/string #'string-cell
          :cell/color  #'color-cell})

