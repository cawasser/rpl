(ns mvs.dashboard.ui.state
  (:require [clj-uuid :as uuid]))


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


(def app-db (atom {:description            "stunt table"
                   :customer/orders        order-items
                   :customer/order-columns order-columns}))


(defn init [sub-key data]
  (swap! app-db assoc sub-key data))


(defn add-row [sub-key row]
  (swap! app-db assoc sub-key (conj (sub-key @app-db) row)))

