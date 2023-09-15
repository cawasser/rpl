(ns mvs.dashboard.ui.demo
  (:require [mvs.dashboard.sales :as sales]
            [mvs.dashboard.customer :as customer]
            [mvs.dashboard.monitoring :as monitoring]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]
            [cljfx.api :as fx]
            [cljfx.dev :refer :all]
            [clj-uuid :as uuid]))



(defn root [_]
  {:fx/type fx/ext-many
   :desc    [
             ;{:fx/type sales/dashboard
             ; :x       0 :y 0
             ; :width   600 :height 400
             ; :title   "Sales"}
             {:fx/type customer/dashboard
              :x       650 :y 0
              :width   600 :height 400
              :title   "Customer Dashboard"}]}
  ;{:fx/type monitoring/dashboard
  ; :x       0 :y 450
  ; :width   1300 :height 500
  ; :title   "Monitoring Dashboard"}]})


  (def renderer
    (fx/create-renderer
      :middleware (comp
                    fx/wrap-context-desc
                    (fx/wrap-map-desc (fn [_] {:fx/type root})))
      :opts {:fx.opt/type->lifecycle   #(or (fx/keyword->lifecycle %)
                                          (fx/fn->lifecycle-with-context %))
             :fx.opt/map-event-handler e/event-handler})))


(fx/mount-renderer state/app-db renderer)


(comment
  (help-ui)

  ;(state/init :customer/orders order-items)
  ;(state/init :customer/order-columns order-columns)
  ;
  ;
  ;(state/add-row :customer/orders {:order/id (uuid/v1) :customer/id "alice"})
  ;(state/add-row :customer/orders {:order/id (uuid/v1) :customer/id "carol"})


  ())


(comment
  (meta #'root)

  (meta #'customer/dashboard)
  (-> #'customer/dashboard
    meta
    :ns
    ns-name)


  (cljfx.dev/help-ui)



  ())
