(ns mvs.dashboard.sales
  (:require [mvs.dashboard.ui.table :as table]
            [mvs.dashboard.ui.window :as w]
            [mvs.read-model.provider-catalog-view :as v]))


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
  (let [catalog      (v/provider-catalogs context)
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
