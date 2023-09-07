(ns mvs.dashboard.customer
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.window :as w]
            [mvs.read-model.sales-catalog-view :as sales-v]))
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
(def dummy-orders {#uuid"d24d9034-4b66-11ee-b095-a36a1a8bbf73" {:customer/id         #uuid"d24d9030-4b66-11ee-b095-a36a1a8bbf73",
                                                                :order/id            #uuid"d24d9034-4b66-11ee-b095-a36a1a8bbf73",
                                                                :order/status        :order/submitted,
                                                                :order/needs         [0 1],
                                                                :sales/request-id    #uuid"e2b0d090-4b66-11ee-b095-a36a1a8bbf73",
                                                                :agreement/id        #uuid"e2b2a550-4b66-11ee-b095-a36a1a8bbf73",
                                                                :agreement/resources '({:resource/type        0,
                                                                                        :provider/id          "delta-googoos",
                                                                                        :resource/time-frames [0 1],
                                                                                        :resource/cost        10}
                                                                                       {:resource/type        0,
                                                                                        :provider/id          "alpha-googoos",
                                                                                        :resource/time-frames [2 3 4],
                                                                                        :resource/cost        30}
                                                                                       {:resource/type        0,
                                                                                        :provider/id          "bravo-googoos",
                                                                                        :resource/time-frames [5],
                                                                                        :resource/cost        5}
                                                                                       {:resource/type        1,
                                                                                        :provider/id          "delta-googoos",
                                                                                        :resource/time-frames [0 1],
                                                                                        :resource/cost        10}
                                                                                       {:resource/type        1,
                                                                                        :provider/id          "alpha-googoos",
                                                                                        :resource/time-frames [2 3 4],
                                                                                        :resource/cost        30}
                                                                                       {:resource/type        1,
                                                                                        :provider/id          "bravo-googoos",
                                                                                        :resource/time-frames [5],
                                                                                        :resource/cost        5})}})


(def order-columns [{:column/id     :order/id
                     :column/name   "Order #"
                     :column/render :cell/string}
                    {:column/id     :customer/id
                     :column/name   "Customer #"
                     :column/render :cell/string}
                    {:column/id     :order/status
                     :column/name   "Status"
                     :column/render :cell/string}
                    {:column/id     :sales/request-id
                     :column/name   "Sales Request #"
                     :column/render :cell/string}
                    {:column/id     :agreement/id
                     :column/name   "Agreement #"
                     :column/render :cell/string}
                    {:column/id     :order/needs
                     :column/name   "Needs"
                     :column/render :cell/string}])


(defn- order-table [{:keys [fx/context width height]}]
  (let [orders       dummy-orders
        presentation (vals orders)]
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




  ())