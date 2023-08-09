(ns mvs.specs
  (:require [clojure.spec.alpha :as spec]
            [malli.provider :as mp]
            [clj-uuid :as uuid]))


; :googoo/googoos
(spec/def :resource/id integer?)
(spec/def :googoo/googoo (spec/keys :req [:resource/id]))
(spec/def :googoo/googoos (spec/coll-of :googoo/googoo))


; :resource/catalog
(spec/def :resource/time integer?)
(spec/def :resource/time-frames (spec/coll-of :resource/time))
(spec/def :resource/cost integer?)
(spec/def :resource/definition (spec/keys :req [:resource/id :resource/time-frames :resource/cost]))
(spec/def :resource/catalog (spec/coll-of :resource/definition))


; :provider/catalog
(spec/def :provider/id string?)
(spec/def :provider/event-key (spec/keys :req [:provider/id]))
(spec/def :provider/catalog (spec/tuple :provider/event-key :resource/catalog))


; :service/catalog
(spec/def :service/id integer?)
(spec/def :service/description string?)
(spec/def :service/element (spec/keys :req [:resource/id :resource/time-frames]))
(spec/def :service/elements (spec/coll-of :service/element))
(spec/def :service/price integer?)
(spec/def :service/definition (spec/keys :req [:service/id :service/elements :service/description :service/price]))
(spec/def :service/catalog (spec/coll-of :service/definition))


; :customer/service-request
(spec/def :customer/id uuid?)
(spec/def :customer/request-id uuid?)
(spec/def :customer/needs (spec/coll-of :service/id))
(spec/def :customer/service-request (spec/keys :req [:customer/request-id :customer/id :customer/needs]))


; :service/request
(spec/def :service/request-id uuid?)
(spec/def :service/resource (spec/keys :req [:resource/id :resource/time-frames]))
(spec/def :service/resources (spec/coll-of :service/resource))
(spec/def :service/request (spec/keys :req [:service/request-id :customer/request-id
                                            :customer/needs :service/resources]))


; :service/commitment
(spec/def :commitment/id uuid?)
(spec/def :commitment/resource (spec/keys :req [:resource/id :provider/id :resource/time-frames :resource/cost]))
(spec/def :commitment/resources (spec/coll-of :commitment/resource))
(spec/def :commitment/start-time integer?)
(spec/def :commitment/end-time integer?)
(spec/def :commitment/time-frame (spec/cat :start :commitment/start-time :end :commitment/end-time))
(spec/def :service/commitment (spec/keys :req [:commitment/id :commitment/resources :commitment/time-frame]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (spec/explain :service/request-id (uuid/v1))
  (spec/explain :customer/request-id (uuid/v1))
  (spec/explain :customer/needs [0 1])
  (spec/explain :resource/catalog [{:resource/id 0 :resource/time-frames [0 1]}])
  (spec/explain :service/request {:service/request-id  (uuid/v1)
                                  :customer/request-id (uuid/v1)
                                  :customer/needs      [0 1]
                                  :service/resources   [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}
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
  (spec/explain :service/commitment {:commitment/id         (uuid/v1)
                                     :commitment/resources  [{:resource/id          0 :provider/id "alpha"
                                                              :resource/time-frames [0 1 2] :resource/cost 25}
                                                             {:resource/id          1 :provider/id "alpha"
                                                              :resource/time-frames [3 8 9] :resource/cost 25}]
                                     :commitment/time-frame [0 9]})



  ())

; endregion
