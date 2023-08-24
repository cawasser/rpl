(ns mvs.specs
  (:require [clojure.spec.alpha :as spec]
            [clj-uuid :as uuid]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; DATA ITEM SPECS

; region ; :googoo/googoos (resource)

; TODO: at some point (monitoring) we need to distinguish between each
;       individual Googoo, regardless of its attributes, but HOW? Currently,
;       we identify a Googoo by :resource/id which is NOT unique throughout
;       the system, eg. "alpha" and "bravo" BOTH produce several "0" Googoos,
;       even at the same :resource/time-frame!

; Googoos are actually uniquely defined by:
;      1) :resource/id (which should be a :googoo/type)
;      2) :resource/time-frame, AND
;      3) :provider/id
;
; this changes things related to allocating resources by attributes, specifically
; :resource/type / :resource/time-frame, with :provider/id being the wildcard (pattern-matching
; concept)
;
; this effectively resets :resource/id to :resource/type:
;
;    :resource/definition, :sales/resource, :commitment/resource, :agreement/resource
;
;

(spec/def :resource/id uuid?)
(spec/def :resource/type integer?)
(spec/def :resource/time integer?)
(spec/def :resource/attributes (spec/cat
                                 :type :resource/type
                                 :time :resource/time
                                 :provider :provider/id))
(spec/def :resource/resource (spec/keys :req [:resource/id
                                              :resource/attributes]))

; endregion


; region ; :resource/catalog (collection of resources, organized by attributes)

(spec/def :resource/time-frames (spec/coll-of :resource/time))
(spec/def :resource/cost integer?)
(spec/def :resource/definition (spec/keys :req [:resource/type
                                                :resource/time-frames
                                                :resource/cost]))
(spec/def :resource/catalog (spec/coll-of :resource/definition))

; endregion


; region ; :provider/catalog (resources offered to ACME by a Provider)

(spec/def :provider/id string?)
(spec/def :provider/catalog (spec/keys :req [:provider/id
                                             :resource/catalog]))

; endregion


; region ; :service/catalog (services [bundles of resources] offered to Customers by ACME)
(spec/def :service/id integer?)
(spec/def :service/description string?)
(spec/def :service/element (spec/keys :req [:resource/type
                                            :resource/time-frames]))
(spec/def :service/elements (spec/coll-of :service/element))
(spec/def :service/price integer?)
(spec/def :service/definition (spec/keys :req [:service/id
                                               :service/elements
                                               :service/description
                                               :service/price]))
(spec/def :service/catalog (spec/coll-of :service/definition))

; endregion


; region ; :request/status (does this duplicate :order/status?)
(spec/def :request/status (spec/or
                            :successful #(= % :request/successful)
                            :failed #(= % :request/failed)
                            :submitted #(= % :request/submitted)
                            :malformed #(= % :request/malformed)
                            :errored #(= % :request/errored)))

; endregion


; region ; :customer/order (Customer places an order for services with ACME)
(spec/def :customer/id uuid?)
(spec/def :order/id uuid?)
(spec/def :order/needs (spec/coll-of :service/id))
; :order/fulfilled is separate from :order/closed for those scenarios where the
; resource is actually a "service" that exists independently ove some period of time
; (i.e., NOT a material good that is just delivered and the order is "over")
;
;    similarly for :order/terminate-early
;
(spec/def :order/status (spec/or
                          :submitted #(= % :order/submitted) ; submitted (customer->ACME or ACME->Provider)
                          :accepted #(= % :order/accepted)  ; accepted by AMCE or Provider
                          :planned #(= % :order/planned)    ; ? ACME created commitment
                          :awaiting-approval #(= % :order/awaiting-approval) ; (customer or ACME)
                          :approved #(= % :order/approved)  ; (customer or ACME)
                          :awaiting-fulfilment #(= % :order/awaiting-fulfilment) ; ? ACME->Provider
                          :purchased #(= % :order/purchased) ; TODO: does :order/purchased duplicate :order/awaiting-fulfilment?
                          :fulfilled #(= % :order/fulfilled) ; Provider->ACME
                          :abandoned #(= % :order/abandoned) ; Customer
                          :terminated-early #(= % :order/terminated-early) ; Customer
                          :closed #(= % :order/closed)))
(spec/def :customer/order (spec/keys :req [:order/id
                                           :customer/id
                                           :order/needs
                                           :order/status]))

; endregion


; region ; :sales/request (SALES requests PLANNING to identify inventory to satisfy a Customer order)
(spec/def :sales/request-id uuid?)
(spec/def :sales/resource (spec/keys :req [:resource/type :resource/time-frames]))
(spec/def :sales/resources (spec/coll-of :sales/resource))
(spec/def :sales/request (spec/keys :req [:sales/request-id
                                          :request/status
                                          :order/id
                                          :customer/id
                                          :order/needs
                                          :sales/resources]))

; endregion


; region ; :sales/commitment (PLANNING commits resources to SALES to satisfy a Customer order)
(spec/def :commitment/id uuid?)
(spec/def :commitment/resource (spec/keys :req [:resource/type
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


; region ; :sales/failure (PLANNING cannot commit resources to SALES)
(spec/def :failure/id uuid?)
(spec/def :failure/reason string?)
(spec/def :failure/reasons (spec/coll-of :failure/reason))
(spec/def :sales/failure (spec/keys :req [:failure/id
                                          :sales/request-id
                                          :request/status
                                          :failure/reasons]))

; endregion


; region ; :sales/agreement (SALES provide Customer with a proposal to satisfy their order)
(spec/def :agreement/id uuid?)
(spec/def :agreement/price integer?)
(spec/def :agreement/resource (spec/keys :req [:resource/type
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


; region ; :order/approval (Customer "signs" the agreement to "purchase" the ordered services)
(spec/def :order/approval (spec/keys :req [:agreement/id
                                           :customer/id
                                           :order/id
                                           :order/status]))

; endregion


; region ; :sales/plan (ACME turns a Customer order into purchase orders to Providers)
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


; region ; :provider/shipment (Provider ships resources to ACME)

(spec/def :shipment/id uuid?)
(spec/def :shipment/line-item (spec/keys :req [:resource/id ; assigned by the provider
                                               :resource/type
                                               :resource/time]))
(spec/def :shipment/items (spec/coll-of :shipment/line-item))
(spec/def :provider/shipment (spec/keys :req [:shipment/id
                                              :order/id
                                              :provider/id
                                              :shipment/items]))

; endregion


; region ; :resource/measurement (resources report status/performance/etc. to ACME)

(spec/def :measurement/id uuid?)
(spec/def :measurement/attribute keyword?)
(spec/def :measurement/value integer?)
(spec/def :resource/measurement (spec/keys :req [:measurement/id
                                                 :resource/id
                                                 :measurement/attribute
                                                 :measurement/value]))

(spec/def :resource/health keyword?)

(spec/def :resource/performance integer?)

;(spec/def :resource/sla-target double?)                     ; percentage
;(spec/def :resource/usage (spec/keys :req [:customer/id
;                                           :service/id
;                                           :resource/sla-target]))

; endregion

; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; EVENT KEYS (DOMAIN ENTITIES)

(spec/def :domain/customer (spec/keys :req [:customer/id]))
(spec/def :domain/provider (spec/keys :req [:provider/id]))
(spec/def :domain/order (spec/keys :req [:order/id]))
(spec/def :domain/resource (spec/keys :req [:resource/id]))
(spec/def :domain/shipment (spec/keys :req [:shipment/id])) ; TODO: really? or should this be :domain/provider?
(spec/def :domain/plan (spec/keys :req [:plan/id]))         ; TODO: really? should this be :domain/order?
(spec/def :domain/agreement (spec/keys :req [:agreement/id])) ; TODO: really? should this be :domain/order?

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (spec/explain :sales/request-id (uuid/v1))
  (spec/explain :order/id (uuid/v1))
  (spec/explain :order/needs [0 1])
  (spec/explain :resource/catalog [{:resource/type 0 :resource/time-frames [0 1]}])
  (spec/explain :sales/request {:sales/request-id    (uuid/v1)
                                :customer/request-id (uuid/v1)
                                :request/status      :request/submitted
                                :customer/needs      [0 1]
                                :sales/resources     [{:resource/type 0 :resource/time-frames [0 1 2 3 4 5]}
                                                      {:resource/type 1 :resource/time-frames [0 1 2 3 4 5]}]})

  ())


(comment
  (spec/explain :commitment/id (uuid/v1))
  (spec/explain :commitment/resource {:resource/type        0 :provider/id "alpha"
                                      :resource/time-frames [0 1 2 3 8 9] :resource/cost 25})
  (spec/explain :commitment/resources [{:resource/type        0 :provider/id "alpha"
                                        :resource/time-frames [0 1 2] :resource/cost 25}
                                       {:resource/type        1 :provider/id "alpha"
                                        :resource/time-frames [3 8 9] :resource/cost 25}])
  (spec/explain :commitment/start-time 0)
  (spec/explain :commitment/end-time 9)
  (spec/explain :commitment/time-frame [0 9])
  (spec/explain :sales/commitment {:commitment/id         (uuid/v1)
                                   :commitment/resources  [{:resource/type        0 :provider/id "alpha"
                                                            :resource/time-frames [0 1 2] :resource/cost 25}
                                                           {:resource/type        1 :provider/id "alpha"
                                                            :resource/time-frames [3 8 9] :resource/cost 25}]
                                   :commitment/time-frame [0 9]})



  ())

; endregion
