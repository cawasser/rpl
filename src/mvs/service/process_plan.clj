(ns mvs.service.process-plan
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-plan
  "takes a :sales/plan and turns it into collections of :provider/orders to be
  sent to the providers for fulfillment.

  "
  [[{:keys [order/id] :as event-key}
    {plan-id          :plan/id
     customer-id      :customer/id
     sales-request-id :sales/request-id
     resources        :commitment/resources
     :as              plan}
    :as event]]

  (println "process-plan" event-key)

  (if (spec/valid? :sales/plan plan)
    (let [resources     (->> plan
                          :commitment/resources
                          (group-by :provider/id))
          ; TODO: map -> for?
          expanded-plan (map (fn [[id r]]
                               {id (map (fn [m]
                                          (dissoc m :provider/id :resource/cost))
                                     r)})
                          resources)]

      ; 1) place orders with the providers
      (doseq [p expanded-plan]
        (doseq [[id r] p]
          (let [provider-order {:order/id         (uuid/v1)
                                :provider/id      id
                                :order/status     :order/purchased
                                :service/elements r}]
            (publish! provider-order-topic [{:provider/id id}
                                            provider-order])))))

    ; TODO: 2) tell "monitoring" to watch for the new orders


    (malformed "process-plan" :sales/plan plan)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; format a :provider/shipping event to each :provider/id (in the :sales/plan, specifically
; the :commitment/resources key)
(comment
  (do
    (def plan {:plan/id          (uuid/v1)
               :customer/id      (uuid/v1)
               :sales/request-id (uuid/v1)
               :commitment/resources
               [{:resource/type        0 :provider/id "alpha"
                 :resource/time-frames [0 1] :resource/cost 10}
                {:resource/type        0 :provider/id "delta"
                 :resource/time-frames [0 1] :resource/cost 10}
                {:resource/type        1 :provider/id "alpha"
                 :resource/time-frames [0 1] :resource/cost 10}
                {:resource/type        1 :provider/id "bravo"
                 :resource/time-frames [0 1] :resource/cost 10}]}))

  ; so we expect 3 events, one each for: "alpha", "bravo", and "delta"

  (def resources (->> plan
                   :commitment/resources
                   (group-by :provider/id)))

  ; then, drop the :provider/id and :resource/cost keys from each resource
  ; (this is a map inside a map)
  ; TODO: map -> for?
  (def expanded-plan (map (fn [[id r]]
                            {id (map (fn [m]
                                       (dissoc m :provider/id :resource/cost))
                                  r)})
                       resources))

  ; and then reformat into :provider/order:
  ;     :order/id
  ;     :provider/id
  ;     :service/elements
  ;     :order/status
  (def provider-order {:order/id     (uuid/v1)
                       :provider/id  "alpha"
                       :order/status :order/purchased
                       :service/elements
                       [{:resource/type 0 :resource/time-frames [0 1]}
                        {:resource/type 1 :resource/time-frames [0 1]}]})

  (spec/explain :provider/order provider-order)

  ; TODO: map -> for?
  ; create an event for each provider in expanded-plan
  (doall
    (mapcat (fn [m]
              (map (fn [[id r]]
                     (let [order-id       (uuid/v1)
                           provider-order {:order/id         order-id
                                           :provider/id      id
                                           :order/status     :order/purchased
                                           :service/elements r}]
                       [{:order/id order-id}
                        provider-order]))
                m))
      expanded-plan))

  (doseq [p expanded-plan]
    (doseq [[id r] p]
      (let [order-id       (uuid/v1)
            provider-order {:order/id         order-id
                            :provider/id      id
                            :order/status     :order/purchased
                            :service/elements r}]
        [{:order/id order-id}
         provider-order])))





  ())


; endregion


