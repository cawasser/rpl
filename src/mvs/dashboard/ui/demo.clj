(ns mvs.dashboard.ui.demo
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.state :as state]
            [mvs.dashboard.ui.window :as window]
            [mvs.dashboard.customer :as customer]
            [cljfx.api :as fx]
            [cljfx.dev :refer :all]
            [clj-uuid :as uuid]))




(def width 300)
(def height 100)


(defn root [{:keys [customer/orders customer/order-columns]}]
  {:fx/type fx/ext-many
   :desc    [{:fx/type window/window
              :x       100 :y 100
              :width   960 :height 540
              :content table/table-view
              :title   "Customer Orders"
              :columns order-columns
              :data    orders}
             {:fx/type window/window
              :x       500 :y 500
              :width   960 :height 540
              :content table/table-view
              :title   "Sales Dashboard"
              :columns order-columns
              :data    orders}]})


(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)))


(fx/mount-renderer state/app-db renderer)


(comment
  (help-ui)

  (state/init :customer/orders order-items)
  (state/init :customer/order-columns order-columns)


  (state/add-row :customer/orders {:order/id (uuid/v1) :customer/id "alice"})
  (state/add-row :customer/orders {:order/id (uuid/v1) :customer/id "carol"})


  ())


(comment
  (meta #'root)

  (meta #'customer/dashboard)
  (-> #'customer/dashboard
    meta
    :ns
    ns-name)


  ())
