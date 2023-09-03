(ns mvs.dashboard.sales
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.text-area :as text-area]
            [mvs.dashboard.ui.window :as w]
            [mvs.read-model.provider-catalog-view :as v]))


(def provider-catalog-columns [{:column/id     :provider/id
                                :column/name   "Provider ID"
                                :column/render :cell/string}
                               {:column/id     :resource/type
                                :column/name   "Type"
                                :column/render :cell/string}
                               {:column/id     :resource/time-frames
                                :column/name   "Time Frames"
                                :column/render :cell/string}
                               {:column/id     :resource/cost
                                :column/name   "Cost"
                                :column/render :cell/string}])


(defn dashboard [{:keys [fx/context x y width height]}]
  (println "SALES dashboard")

  (let [catalog      (v/provider-catalogs context)
        presentation (->> catalog
                       (mapcat (fn [[id {:keys [resource/catalog]}]]
                                 (map (fn [{:keys [resource/type resource/time-frames resource/cost]}]
                                        {:provider/id          id
                                         :resource/type        type
                                         :resource/time-frames time-frames
                                         :resource/cost        cost})
                                   catalog)))
                       vec
                       doall)]
    {:fx/type w/window
     :title   "Sales Dashboard"
     :x       x
     :y       y
     :width   width
     :height  height
     :content {:fx/type table/table-view
               :width   width
               :height  height
               :columns provider-catalog-columns
               :data    presentation}}))






(comment
  (def catalog (v/provider-catalogs @mvs.read-models/app-db))

  (def presentation (->> catalog
                      (mapcat (fn [[id {:keys [resource/catalog]}]]
                                (map (fn [{:keys [resource/type resource/time-frames resource/cost]}]
                                       {:provider/id          id
                                        :resource/type        type
                                        :resource/time-frames type
                                        :resource/cost        cost})
                                  catalog)))
                      vec))


  (take 3 presentation)



  ())

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment


  ())

; endregion
