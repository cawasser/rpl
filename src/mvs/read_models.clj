(ns mvs.read-models
  (:require [mvs.constants :refer :all]
            [clojure.spec.alpha :as spec]
            [clj-uuid :as uuid]))


(def googoos (->> (range num-googoos)
               (map (fn [id] {:resource/id id}))
               (into [])))


(def provider-alpha {:provider/id      "alpha"
                     :resource/catalog [{:resource/type 0 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 1 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 2 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 3 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/type 4 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}]})
(def provider-bravo {:provider/id      "bravo"
                     :resource/catalog [{:resource/type 0 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 1 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 2 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 3 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/type 4 :resource/time-frames [1 3 5] :resource/cost 5}]})
(def provider-charlie {:provider/id      "charlie"
                       :resource/catalog [{:resource/type 0 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 1 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 2 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 3 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/type 4 :resource/time-frames [2 4 5] :resource/cost 5}]})
(def provider-delta {:provider/id      "delta"
                     :resource/catalog [{:resource/type 0 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 1 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 2 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 3 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/type 4 :resource/time-frames [0 1 2] :resource/cost 5}]})
(def provider-echo {:provider/id      "echo"
                    :resource/catalog [{:resource/type 0 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 1 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 2 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 3 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/type 4 :resource/time-frames [3 4] :resource/cost 2}]})


(def provider-catalog-view
  "summary of all the provider catalogs" (atom {}))

(def available-resources-view
  "denormalized arrangements of all resources available" (atom {}))

(def service-catalog-view
  "catalog of service ACME offers to customer" (atom []))

;(def sales-catalog-history-view
;  "history of all the sales catalogs of ACME ever offered to customer" (atom []))

(def order->sales-request-view
  "maps order/id to :sales/request-id so we can relate all information" (atom {}))

(def committed-resources-view
  "all the resources that ACME has committed to customers" (atom []))

(def resource-monitoring-view
  "all the resources that ACME must monitor" (atom []))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; test googoo specs
(comment
  (spec/explain :resource/id (uuid/v1))
  (spec/explain :resource/type 5)
  (spec/explain :resource/time 0)
  (spec/explain :resource/attributes [5 0 "alpha"])
  (spec/explain :resource/resource {:resource/id         (uuid/v1)
                                    :resource/attributes [0 0 "bravo"]})

  ())


; test :resource/definition specs
(comment
  (spec/explain :resource/time-frames [1 3 5])
  (spec/explain :resource/definition {:resource/type        0
                                      :resource/time-frames [1 3 5]
                                      :resource/cost        5})
  (spec/explain :resource/definition {:resource/type        0
                                      :resource/time-frames [0 1 2 3 4 5]
                                      :resource/cost        10})
  (spec/explain :resource/catalog [{:resource/type        0
                                    :resource/time-frames [0 1 2 3 4 5]
                                    :resource/cost        10}
                                   {:resource/type        2
                                    :resource/time-frames [0 1 2 3 4 5]
                                    :resource/cost        10}])


  ())


; test :provider/catalog specs
(comment
  (spec/explain :provider/catalog provider-alpha)
  (spec/explain :provider/id "alpha")
  (spec/explain :resource/catalog (:resource/catalog provider-alpha))

  ())




; endregion
