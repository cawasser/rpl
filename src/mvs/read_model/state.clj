(ns mvs.read-model.state
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [clojure.core.cache :as cache]))



;(def resource-measurement-init {#uuid"53cf7c01-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 0},
;                                                                             :resource/measurements {:googoo/metric [76 61 55 74]},
;                                                                             :provider/id           "delta-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cfca21-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 2},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "alpha-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cf7c00-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 1},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "delta-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cfca23-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 4},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "alpha-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cf7c02-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 1},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "delta-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cfca22-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 3},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "alpha-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cfca25-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 3},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "alpha-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cfca24-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 2},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "alpha-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cfca26-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 4},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "alpha-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cf54f1-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 0},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "delta-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cff131-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 0, :resource/time 5},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "bravo-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]},
;                                #uuid"53cff132-4cf1-11ee-9351-b9081dfd246f" {:resource/attributes   {:resource/type 1, :resource/time 5},
;                                                                             :resource/measurements {},
;                                                                             :provider/id           "bravo-googoos",
;                                                                             :order/id              #uuid"1e9a8524-4cec-11ee-9351-b9081dfd246f",
;                                                                             :customer/id           #uuid"1e9a8520-4cec-11ee-9351-b9081dfd246f",
;                                                                             :sales/request-id      #uuid"3fe0a3e0-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :agreement/id          #uuid"3fe36300-4cf1-11ee-9351-b9081dfd246f",
;                                                                             :order/needs           [0 1]}})
(def initial-state {:provider-catalog-view      {}
                    :available-resources-view   {}
                    ;:service-catalog-view       []
                    :sales-catalog-view         []
                    :order->sales-request-view  {}
                    :committed-resources-view   []
                    :resource-measurements-view {} ;resource-measurement-init
                    :resource-performance-view  {}
                    :resource-usage-view        {}
                    :customer-order-view        {}
                    :customer-agreement-view    {}})

(def app-db (atom (fx/create-context initial-state cache/lru-cache-factory)))

(defn db [] (deref app-db))

