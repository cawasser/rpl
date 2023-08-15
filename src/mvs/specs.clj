(ns mvs.specs
  (:require [clojure.spec.alpha :as spec]
            [malli.provider :as mp]
            [clj-uuid :as uuid]))


; region ; :googoo/googoos (resource)
(spec/def :resource/id integer?)
(spec/def :googoo/googoo (spec/keys :req [:resource/id]))
(spec/def :googoo/googoos (spec/coll-of :googoo/googoo))

; endregion


; region ; :resource/catalog

(spec/def :resource/time integer?)
(spec/def :resource/time-frames (spec/coll-of :resource/time))
(spec/def :resource/cost integer?)
(spec/def :resource/definition (spec/keys :req [:resource/id
                                                :resource/time-frames
                                                :resource/cost]))
(spec/def :resource/catalog (spec/coll-of :resource/definition))

; endregion


; region ; :provider/catalog

(spec/def :provider/id string?)
(spec/def :provider/event-key (spec/keys :req [:provider/id]))
(spec/def :provider/catalog (spec/keys :req [:provider/id
                                             :resource/catalog]))

; endregion


; region ; :service/catalog
(spec/def :service/id integer?)
(spec/def :service/description string?)
(spec/def :service/element (spec/keys :req [:resource/googoo
                                            :resource/time-frames]))
(spec/def :service/elements (spec/coll-of :service/element))
(spec/def :service/price integer?)
(spec/def :service/definition (spec/keys :req [:service/id
                                               :service/elements
                                               :service/description
                                               :service/price]))
(spec/def :service/catalog (spec/coll-of :service/definition))

; endregion


; region ; :request/status
(spec/def :request/status (spec/or
                            :successful #(= % :request/successful)
                            :failed #(= % :request/failed)
                            :submitted #(= % :request/submitted)
                            :malformed #(= % :request/malformed)
                            :errored #(= % :request/errored)))

; endregion


; region ; :customer/order
(spec/def :customer/id uuid?)
(spec/def :order/id uuid?)
(spec/def :order/needs (spec/coll-of :service/id))
(spec/def :customer/order (spec/keys :req [:order/id
                                           :customer/id
                                           :order/needs]))

; endregion


; region ; :sales/request
(spec/def :sales/request-id uuid?)
(spec/def :sales/resource (spec/keys :req [:resource/id :resource/time-frames]))
(spec/def :sales/resources (spec/coll-of :sales/resource))
(spec/def :sales/request (spec/keys :req [:sales/request-id
                                          :request/status
                                          :order/id
                                          :customer/id
                                          :order/needs
                                          :sales/resources]))

; endregion


; region ; :sales/commitment
(spec/def :commitment/id uuid?)
(spec/def :commitment/resource (spec/keys :req [:resource/id
                                                :provider/id
                                                :resource/time-frames
                                                :resource/cost]))
(spec/def :commitment/resources (spec/coll-of :commitment/resource))
(spec/def :commitment/start-time integer?)
(spec/def :commitment/end-time integer?)
(spec/def :commitment/cost integer?)
(spec/def :commitment/time-frame (spec/cat
                                   :start :commitment/start-time
                                   :end :commitment/end-time))
(spec/def :sales/commitment (spec/keys :req [:commitment/id
                                             :sales/request-id
                                             :request/status
                                             :commitment/resources
                                             :commitment/time-frame
                                             :commitment/cost]))

; endregion


; region ; :sales/failure
(spec/def :failure/id uuid?)
(spec/def :failure/reason string?)
(spec/def :failure/reasons (spec/coll-of :failure/reason))
(spec/def :sales/failure (spec/keys :req [:failure/id
                                          :sales/request-id
                                          :request/status
                                          :failure/reasons]))

; endregion


; region ; :sales/agreement
(spec/def :agreement/id uuid?)
(spec/def :agreement/price integer?)
(spec/def :agreement/resource (spec/keys :req [:resource/id
                                               :resource/time-frames]))
(spec/def :agreement/resources (spec/coll-of :agreement/resource))
(spec/def :agreement/start-time integer?)
(spec/def :agreement/end-time integer?)
(spec/def :agreement/time-frame (spec/cat
                                  :start :agreement/start-time
                                  :end :agreement/end-time))
(spec/def :agreement/note string?)
(spec/def :agreement/notes (spec/coll-of :agreement/note))
(spec/def :sales/agreement (spec/keys :req [:agreement/id
                                            :customer/id
                                            :order/id
                                            :order/needs
                                            :agreement/resources
                                            :agreement/time-frame
                                            :agreement/price
                                            :agreement/notes]))

; endregion


; region ; :order/approval
(spec/def :order/status (spec/or
                          :purchased #(= % :order/purchased)))
(spec/def :order/approval (spec/keys :req [:agreement/id
                                           :customer/id
                                           :order/id
                                           :order/status]))
; endregion


; region ; :sales/plan
(spec/def :plan/id uuid?)
(spec/def :sales/plan (spec/keys :req [:plan/id
                                       :customer/id
                                       :sales/request-id
                                       :commitment/resources]))
; endregion


; region ; :provider/order (ACME places an order with a provider)
(spec/def :provider/order (spec/keys :req [:order/id
                                           :provider/id
                                           :service/elements
                                           :order/status]))
; endregion


; region ; :resource/measurement

(spec/def :measurement/value integer?)
(spec/def :resource/measurement (spec/keys :req [:resource/googoo
                                                 :resource/time-frame
                                                 :measurement/value]))

; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (spec/explain :sales/request-id (uuid/v1))
  (spec/explain :order/id (uuid/v1))
  (spec/explain :order/needs [0 1])
  (spec/explain :resource/catalog [{:resource/id 0 :resource/time-frames [0 1]}])
  (spec/explain :sales/request {:sales/request-id    (uuid/v1)
                                :customer/request-id (uuid/v1)
                                :request/status      :request/submitted
                                :customer/needs      [0 1]
                                :sales/resources     [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
                                                      {:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]})

  ())


(comment
  (spec/explain :commitment/id (uuid/v1))
  (spec/explain :commitment/resource {:resource/id          0 :provider/id "alpha"
                                      :resource/time-frames [0 1 2 3 8 9] :resource/cost 25})
  (spec/explain :commitment/resources [{:resource/id          0 :provider/id "alpha"
                                        :resource/time-frames [0 1 2] :resource/cost 25}
                                       {:resource/id          1 :provider/id "alpha"
                                        :resource/time-frames [3 8 9] :resource/cost 25}])
  (spec/explain :commitment/start-time 0)
  (spec/explain :commitment/end-time 9)
  (spec/explain :commitment/time-frame [0 9])
  (spec/explain :sales/commitment {:commitment/id         (uuid/v1)
                                   :commitment/resources  [{:resource/id          0 :provider/id "alpha"
                                                            :resource/time-frames [0 1 2] :resource/cost 25}
                                                           {:resource/id          1 :provider/id "alpha"
                                                            :resource/time-frames [3 8 9] :resource/cost 25}]
                                   :commitment/time-frame [0 9]})



  ())

; endregion
