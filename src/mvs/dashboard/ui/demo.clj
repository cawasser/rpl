(ns mvs.dashboard.ui.demo
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.state :as state]
            [cljfx.api :as fx]
            [cljfx.dev :refer :all]
            [clj-uuid :as uuid]))


(def order-columns [{:column/id         :order/id
                     :column/name       "Order #"
                     :column/min-width  50
                     :column/pref-width 300
                     :column/max-width  300
                     :column/render     :cell/string}
                    {:column/id     :customer/id
                     :column/name   "Customer"
                     :column/render :cell/string}])


(def order-items [{:order/id (uuid/v1) :customer/id "alice"}
                  {:order/id (uuid/v1) :customer/id "alice"}
                  {:order/id (uuid/v1) :customer/id "bob"}
                  {:order/id (uuid/v1) :customer/id "carol"}
                  {:order/id (uuid/v1) :customer/id "carol"}
                  {:order/id (uuid/v1) :customer/id "carol"}
                  {:order/id (uuid/v1) :customer/id "dave"}])



(defn root [{:keys [customer/orders customer/order-columnss]}]
  {:fx/type :stage
   :showing true
   :title   "Customer Orders"
   :scene   {:fx/type :scene
             :root    {:fx/type     table/table-view
                       :pref-width  960
                       :pref-height 540
                       :columns     order-columns
                       :data        orders}}})


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
