(ns mvs.dashboard.customer
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.window :as w]
            [mvs.read-model.sales-catalog-view :as sales-v]
            [mvs.read-models :as rm]))
;[mvs.read-model.order-view :as order-v]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; SALES CATALOGS TABLE VIEW
;
(def sales-catalog-columns [{:column/id     :service/id
                             :column/name   "Service ID"
                             :column/render :cell/string}
                            {:column/id     :service/description
                             :column/name   "Description"
                             :column/render :cell/string}
                            {:column/id     :service/elements
                             :column/name   "Elements"
                             :column/render :cell/string}])


(defn- sales-catalog-table [{:keys [fx/context width height]}]
  (let [catalog (sales-v/sales-catalog context)]
    {:fx/type  :v-box
     :spacing  2
     :children [{:fx/type :label
                 :text    "Services Available"
                 :style   {:-fx-font [:bold 20 :sans-serif]}}
                {:fx/type table/table-view
                 :width   width
                 :height  height
                 :columns sales-catalog-columns
                 :data    catalog}]}))

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; CUSTOMER ORDERS TABLE VIEW
;

(def order-columns [{:column/id     :order/status
                     :column/name   "Status"
                     :column/render :cell/string}
                    {:column/id         :order/id
                     :column/name       "Order #"
                     :column/render     :cell/string
                     :column/pref-width 50}
                    {:column/id     :order/needs
                     :column/name   "Needs"
                     :column/render :cell/string}
                    {:column/id         :customer/usage
                     :column/name       "Usage"
                     :column/render     :cell/gauge
                     :gauge/skin        mvs.dashboard.ui.medusa-gauge/level
                     :column/pref-width 100}
                    {:column/id     :agreement/id
                     :column/name   "Agreement #"
                     :column/render :cell/string}])


(defn- order-table [{:keys [fx/context width height]}]
  (let [orders       (rm/order->sales-request context)
        usage        (rm/resource-usage context)
        presentation (->> orders
                       vals
                       (map (fn [{order :order/id customer :customer/id :as p}]
                              (assoc p
                                :customer/usage (get-in usage
                                                  [customer order :usage/metric])))))]
    {:fx/type  :v-box
     :spacing  2
     :children [{:fx/type :label
                 :text    "Customer Orders"
                 :style   {:-fx-font [:bold 20 :sans-serif]}}
                {:fx/type table/table-view
                 :width   width
                 :height  height
                 :columns order-columns
                 :data    presentation}]}))

; endregion



(defn dashboard [{:keys [fx/context x y width height]}]
  {:fx/type w/window
   :title   "Customer Dashboard"
   :x       x
   :y       y
   :width   width
   :height  height
   :content {:fx/type :split-pane
             :items   [{:fx/type sales-catalog-table
                        :width   width
                        :height  height}
                       {:fx/type order-table
                        :width   width
                        :height  height}]}})






(comment

  (do
    (def context (rm/state))
    (def orders (rm/order->sales-request context))
    (def presentation (vals orders))
    (def order (-> presentation first :order/id))
    (def customer (-> presentation first :customer/id))
    (def p (-> presentation first))

    (def usage (rm/resource-usage context)))


  (->> orders
    vals
    (map (fn [{order :order/id customer :customer/id :as p}]
           (assoc p :customer/usage (get-in usage [customer order :usage/metric])))))






  ())