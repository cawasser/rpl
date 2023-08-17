(ns mvs.services

  "Note: each 'service' is in its own namespace, along with any helper functions (before) and any
  rich comments for repl dev & testing (after). This will make it easier to pull the relevant
  code into actual microservice components when the time comes."

  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]

            [mvs.service.process-available-resources :as a-r]
            [mvs.service.process-customer-order :as c-o]
            [mvs.service.process-measurement :as p-m]
            [mvs.service.process-order-approval :as o-a]
            [mvs.service.process-plan :as plan]
            [mvs.service.process-provider-catalog :as p-cat]
            [mvs.service.process-resource-health :as r-h]
            [mvs.service.process-resource-performance :as r-p]
            [mvs.service.process-resource-usage :as r-u]
            [mvs.service.process-sales-commitment :as s-c]
            [mvs.service.process-sales-request :as s-r]
            [mvs.service.process-shipment :as p-s]))


(def process-available-resources #'a-r/process-available-resources)
(def process-customer-order #'c-o/process-customer-order)
(def process-measurement #'p-m/process-measurement)
(def process-order-approval #'o-a/process-order-approval)
(def process-plan #'plan/process-plan)
(def process-provider-catalog #'p-cat/process-provider-catalog)
(def process-resource-health #'r-h/process-resource-health)
(def process-resource-performance #'r-p/process-resource-performance)
(def process-resource-usage #'r-u/process-resource-usage)
(def process-sales-commitment #'s-c/process-sales-commitment)
(def process-sales-request #'s-r/process-sales-request)
(def process-shipment #'p-s/process-shipment)

