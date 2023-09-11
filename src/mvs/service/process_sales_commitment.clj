(ns mvs.service.process-sales-commitment
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :as rm :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(def last-event (atom []))


(defn process-sales-commitment
  "this function takes a :sales/commitment from 'planning' and enriches into
   a :sales/agreement event which we send ot the customer for their approval (or rejection)

   _OR_

   we get a :sales/failure because planning can't fulfil the customer's order, so we should
   tell the customer that we can't do anything, by sending a :sales/failure event"

  [[event-key commitment :as event]]

  (reset! last-event event)

  (if (or (spec/valid? :sales/commitment commitment)
        (spec/valid? :sales/failure commitment))

    ; region ; handle a success or failure from planning
    (condp = (:request/status commitment)
      :request/successful
      (do
        (println "process-sales-commitment SUCCESS" event-key "//" commitment)

        (let [agreement-id         (uuid/v1)
              sales-request-id     (:sales/request-id commitment)
              associated-order     (associated-order sales-request-id)
              customer-id          (:customer/id associated-order)
              order-id             (:order/id associated-order)
              order-needs          (:order/needs associated-order)
              agreement-resources  (:commitment/resources commitment)
              agreement-time-frame (:commitment/time-frame commitment)
              commitment-cost      (:commitment/cost commitment)
              agreement-price      (->> order-needs
                                     (mapcat (fn [id]
                                               (filter (fn [r] (= id (:service/id r)))
                                                 (rm/sales-catalog (rm/state)))))
                                     (map :service/price)
                                     (reduce +))
              agreement-notes      ["note 1" "note 2"]
              sales-agreement      {:agreement/id         agreement-id
                                    :customer/id          customer-id
                                    :order/id             order-id
                                    :order/needs          order-needs
                                    :agreement/resources  agreement-resources
                                    :agreement/time-frame agreement-time-frame
                                    :agreement/price      agreement-price
                                    :agreement/notes      agreement-notes}]

          (println "profit" (- agreement-price commitment-cost)
            "//" agreement-price "//" commitment-cost)

          (println "process-sales-commitment (b) " associated-order)

          ; we should also (assoc) the :agreement/id & other data
          (rm/order->sales-request-view [{:order/id order-id}
                                         (assoc sales-agreement
                                           :order/event :order/awaiting-approval)])

          (publish! sales-agreement-topic [{:agreement/id agreement-id}
                                           sales-agreement])))

      :request/failed
      (println "process-sales-commitment ******* FAILURE ******" event-key "//" event))
    ; endregion

    (malformed "process-sales-commitment" :sales/commitment commitment)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; check :sales/commitment against the spec
(comment
  (spec/explain :sales/commitment {:commitment/id         #uuid"1cce2aa0-3895-11ee-be86-1768c9d0d0e5",
                                   :sales/request-id      #uuid"8f2c8eb0-3882-11ee-be86-1768c9d0d0e5",
                                   :request/status        :request/successful,
                                   :commitment/resources  [{:resource/type 0, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                                           {:resource/type 0, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                                           {:resource/type 0, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                                           {:resource/type 0, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}
                                                           {:resource/type 1, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                                           {:resource/type 1, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                                           {:resource/type 1, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                                           {:resource/type 1, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}],
                                   :commitment/time-frame [0 5],
                                   :commitment/cost       74})

  ()

  ())


; get the :order/id from the :sales/request-id from order->sales-request-view
(comment
  (do
    (def sales-request-id #uuid"86678e00-3c39-11ee-bad8-8e6f1376370b"))

  (->> (rm/order->sales-request (rm/state))
    vals
    (filter #(= sales-request-id (:sales/request-id %)))
    first)

  ())


; get the :sales/request-id from the :order-id
(comment
  (do
    (def order-id #uuid"75f888c0-3ac3-11ee-8473-e65ce679c38d"))

  (rm/order->sales-request (rm/state))

  (associated-sales-request order-id)


  ())


; process-sales-commitment
(comment
  (do
    (def local-view (atom []))
    (def order {:customer/id (uuid/v1)
                :order/id    (uuid/v1)
                :order/needs [0 1]})
    (def event {:commitment/id         #uuid"1cce2aa0-3895-11ee-be86-1768c9d0d0e5",
                :sales/request-id      #uuid"8f2c8eb0-3882-11ee-be86-1768c9d0d0e5",
                :request/status        :request/successful,
                :commitment/resources  [{:resource/type 0, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                        {:resource/type 0, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                        {:resource/type 0, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                        {:resource/type 0, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}
                                        {:resource/type 1, :provider/id "delta", :resource/time-frames [0 2], :resource/cost 10}
                                        {:resource/type 1, :provider/id "alpha", :resource/time-frames [1 5], :resource/cost 20}
                                        {:resource/type 1, :provider/id "bravo", :resource/time-frames [3], :resource/cost 5}
                                        {:resource/type 1, :provider/id "echo", :resource/time-frames [4], :resource/cost 2}],
                :commitment/time-frame [0 5],
                :commitment/cost       74})
    (def commitment event)
    (def agreement-id (uuid/v1))

    (swap! local-view assoc order
      (conj (assoc order :sales/request-id
              (:sales/request-id commitment))))

    (def sales-request-id (:sales/request-id commitment))
    (def associated-order (->> @local-view
                            vals
                            (filter #(= sales-request-id (:sales/request-id %)))
                            first))
    (def customer-id (:customer/id associated-order))
    (def order-id (:order/id associated-order))
    (def order-needs (:order/needs associated-order))
    (def agreement-resources (:commitment/resources commitment))
    (def agreement-time-frame (:commitment/time-frame commitment))
    (def commitment-cost (:commitment/cost commitment))
    (def agreement-price (->> order-needs
                           (mapcat (fn [id]
                                     (filter (fn [r] (= id (:service/id r)))
                                       (sc/sales-catalog (state/db)))))
                           (map :service/price)
                           (reduce +)))
    (def agreement-notes ["note 1" "note 2"]))


  (do
    (def sales-request-id #uuid"8f2c8eb0-3882-11ee-be86-1768c9d0d0e5")
    (->> (rm/order->sales-request (rm/state))
      vals
      (filter #(= sales-request-id (:sales/request-id %)))
      first))

  ())


; add-in the :agreement/id to the cache
(comment
  (do
    (def agreement-id (uuid/v1))
    (def order-id #uuid"6d9bc4e3-3a4a-11ee-8473-e65ce679c38d")
    (def customer-id #uuid"6d9bc4e0-3a4a-11ee-8473-e65ce679c38d")
    (def service-request-id #uuid"7139d330-3a4a-11ee-8473-e65ce679c38d")
    (def agreement-resources [{:resource/type     100 :resource/time-frames [0 1]
                               :resource/provider "alpha" :resource/cost 10}])

    (def local-order->sales-request-view
      (atom {#uuid"6d9bc4e3-3a4a-11ee-8473-e65ce679c38d"
             {:order/id         #uuid"6d9bc4e3-3a4a-11ee-8473-e65ce679c38d",
              :customer/id      #uuid"6d9bc4e0-3a4a-11ee-8473-e65ce679c38d",
              :order/needs      [0 1],
              :sales/request-id #uuid"7139d330-3a4a-11ee-8473-e65ce679c38d"}})))

  (rm/order->sales-request (rm/state))


  (-> @local-order->sales-request-view
    (get order-id)
    (assoc :agreement/id agreement-id)
    (assoc :commitment/resources agreement-resources))


  (-> @local-order->sales-request-view
    (get order-id)
    (assoc :agreement/id agreement-id)
    (assoc :commitment/resources agreement-resources))



  (swap! local-order->sales-request-view
    assoc order-id
    (-> @local-order->sales-request-view
      (get order-id)
      (assoc :agreement/id agreement-id)
      (assoc :commitment/resources agreement-resources)))



  ())

; endregion

