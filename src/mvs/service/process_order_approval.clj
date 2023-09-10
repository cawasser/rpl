(ns mvs.service.process-order-approval
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :as rm]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-order-approval
  "this function takes an :order/agreement from the customer and turns it into a 'plan'
  to be submitted for actual 'fulfillment' (as opposed to what planning does, which is sort of
  hypothetical)
  "
  [[{:keys [order/id] :as event-key} {status      :order/status
                                      customer-id :customer/id
                                      order-id    :order/id
                                      :as         approval}
    :as event]]

  (println "process-order-approval" event-key)

  (if (spec/valid? :order/approval approval)
    (do
      (println "the customer " status " order " order-id)

      (let [sales-request-id (-> order-id associated-sales-request :sales/request-id)
            assoc-agreement  (associated-order sales-request-id)
            resources        (:agreement/resources assoc-agreement)
            plan-id          (uuid/v1)
            plan             {:plan/id              plan-id
                              :customer/id          customer-id
                              :sales/request-id     sales-request-id
                              :commitment/resources resources}]

        ; TODO: 1) update :order/status to :order/:awaiting-fulfilment
        (rm/order->sales-request-view [{:plan/id plan-id}
                                       (assoc plan
                                         :order/event :order/:awaiting-fulfilment)])

        ; 2) publish the plan
        (publish! plan-topic [{:plan/id plan-id} plan])))

    (malformed "process-order-approval" :order/approval approval)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; get the :sales/request associated with this :agreement/id
(comment
  (do
    (def sales-request-id #uuid"def57e90-3b05-11ee-9c78-16a8ee85083d"))

  (rm/order->sales-request (rm/state))

  (associated-order sales-request-id)


  ())


; get the :sales/resources for the :sales/request
(comment
  (do
    (def sales-request-id #uuid"def57e90-3b05-11ee-9c78-16a8ee85083d")
    (def assoc-agreement (associated-order sales-request-id)))

  (:agreement/resources assoc-agreement)

  ())


; format the :sales/plan from the relevant data
(comment
  (do
    (def sales-request-id #uuid"def57e90-3b05-11ee-9c78-16a8ee85083d")
    (def assoc-agreement (associated-order sales-request-id))
    (def resources (:agreement/resources assoc-agreement)))

  (map #(dissoc % :resource/cost) resources)

  ())

; endregion


