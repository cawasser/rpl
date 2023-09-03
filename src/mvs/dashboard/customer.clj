(ns mvs.dashboard.customer
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.window :as w]))


(def order-columns [])
(def catalog-columns [{:column/id     :service/id
                       :column/name   "Service ID"
                       :column/render :cell/string}
                      {:column/id     :service/description
                       :column/name   "Descriptions"
                       :column/render :cell/string}
                      {:column/id     :service/elements
                       :column/name   "Elements"
                       :column/render :cell/string}])


(defn catalog-view [{:keys [fx/context]}]
  (let [catalog ()]
    {:fx/type     table/table-view
     :pref-width  300
     :pref-height 200
     :columns     catalog-columns
     :data        catalog}))



(defn dashboard [{:keys [fx/context]}]
  {:fx/type     w/window
   :x           0
   :y           0
   :width       600
   :height      300
   :content     {:fx/type  :h-box
                 :children [{:fx/type catalog-view}]}})






(comment




  ())