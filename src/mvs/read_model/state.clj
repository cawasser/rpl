(ns mvs.read-model.state
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [clojure.core.cache :as cache]))



(def initial-state {:provider-catalog-view      {}
                    :available-resources-view   {}
                    ;:service-catalog-view       []
                    :sales-catalog-view         []
                    :order->sales-request-view  {}
                    :committed-resources-view   []
                    :resource-measurements-view {}
                    :resource-performance-view  {}
                    :resource-usage-view        {}
                    :customer-order-view        {}
                    :customer-agreement-view    {}})

(def app-db (atom (fx/create-context initial-state cache/lru-cache-factory)))

(defn db [] (deref app-db))


(defn reset []
  (swap! app-db fx/swap-context (constantly initial-state)))


