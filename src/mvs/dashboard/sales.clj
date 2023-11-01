(ns mvs.dashboard.sales
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.window :as w]
            [mvs.read-models :as rm]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; PROVIDER CATALOGS TABLE VIEW
;
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


(defn- provider-catalog-table [{:keys [fx/context width height]}]
  (println "provider-catalog-table" context)
  (let [catalog      (rm/provider-catalogs context)
        presentation (into []
                       (for [[id {cat :resource/catalog}] catalog
                             {:keys [resource/type resource/time-frames resource/cost]} cat]
                         {:provider/id          id
                          :resource/type        type
                          :resource/time-frames time-frames
                          :resource/cost        cost}))]
    {:fx/type  :v-box
     :spacing  2
     :children [{:fx/type :label
                 :text    "Provider Catalogs"
                 :style   {:-fx-font [:bold 20 :sans-serif]}}
                {:fx/type table/table-view
                 :width   width
                 :height  height
                 :columns provider-catalog-columns
                 :data    presentation}]}))

; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; CUSTOMER ORDERS TABLE VIEW
;

(def order-columns [{:column/id     :order/id
                     :column/name   "Order #"
                     :column/render :cell/string
                     :column/pref-width 100}
                    {:column/id     :customer/id
                     :column/name   "Customer #"
                     :column/render :cell/string
                     :column/pref-width 100}
                    {:column/id     :order/status
                     :column/name   "Status"
                     :column/render :cell/string
                     :column/pref-width 100}
                    {:column/id         :customer/usage
                     :column/name       "Usage"
                     :column/render     :cell/gauge
                     :gauge/skin        mvs.dashboard.ui.medusa-gauge/simple-digital
                     :column/pref-width 75}
                    {:column/id     :order/needs
                     :column/name   "Needs"
                     :column/render :cell/string}
                    {:column/id     :sales/request-id
                     :column/name   "Sales Request #"
                     :column/render :cell/string
                     :column/pref-width 100}
                    {:column/id     :agreement/id
                     :column/name   "Agreement #"
                     :column/render :cell/string
                     :column/pref-width 100}])


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
   :title   "Sales Dashboard"
   :x       x
   :y       y
   :width   width
   :height  height
   :content {:fx/type :split-pane
             :items   [{:fx/type provider-catalog-table
                        :width   width
                        :height  height}
                       {:fx/type order-table
                        :width   width
                        :height  height}]}})






(comment
  (require '[mvs.read-model.state :as state])
  (def catalog (v/provider-catalogs (state/db)))

  (def presentation (for [[id {cat :resource/catalog}] catalog
                          {:keys [resource/type resource/time-frames resource/cost]} cat]
                      {:provider/id          id
                       :resource/type        type
                       :resource/time-frames time-frames
                       :resource/cost        cost}))

  (for [[id {:keys [resource/catalog]}] catalog]
    {:id id :cat catalog})


  (take 3 presentation)



  ())

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment


  ())

; endregion
