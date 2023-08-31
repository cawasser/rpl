(ns mvs.dashboard.customer
  (:require [mvs.dashboard.ui.table :as table]))



(defn dashboard [{:keys [customer/orders customer/order-columns]}]
  {:fx/type     table/table-view
   :pref-width  960
   :pref-height 540
   :columns     order-columns
   :data        orders})




(comment
  ; from fork:
  (defn text-content [{:keys [x y width height description]}]
    {:fx/type :v-box
     :alignment :center
     :children [{:fx/type :label
                 :text (str "Window at [" x ", " y "] "
                         "with size " width "x" height)}
                {:fx/type :label
                 :text (or description "DEFAULT TEXT")}]})


  ())