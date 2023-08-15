(ns mvs.read-models
  (:require [mvs.constants :refer :all]
            [clojure.spec.alpha :as spec]))


(def googoos (->> (range num-googoos)
               (map (fn [id] {:resource/id id}))
               (into [])))


(def provider-alpha {:provider/id "alpha"
                     :resource/catalog [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/id 1 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/id 2 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/id 3 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                                        {:resource/id 4 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}]})
(def provider-bravo {:provider/id "bravo"
                     :resource/catalog [{:resource/id 0 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/id 1 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/id 2 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/id 3 :resource/time-frames [1 3 5] :resource/cost 5}
                                        {:resource/id 4 :resource/time-frames [1 3 5] :resource/cost 5}]})
(def provider-charlie {:provider/id "charlie"
                       :resource/catalog [{:resource/id 0 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/id 1 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/id 2 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/id 3 :resource/time-frames [2 4 5] :resource/cost 5}
                                          {:resource/id 4 :resource/time-frames [2 4 5] :resource/cost 5}]})
(def provider-delta {:provider/id "delta"
                     :resource/catalog [{:resource/id 0 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/id 1 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/id 2 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/id 3 :resource/time-frames [0 1 2] :resource/cost 5}
                                        {:resource/id 4 :resource/time-frames [0 1 2] :resource/cost 5}]})
(def provider-echo {:provider/id "echo"
                    :resource/catalog [{:resource/id 0 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/id 1 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/id 2 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/id 3 :resource/time-frames [3 4] :resource/cost 2}
                                       {:resource/id 4 :resource/time-frames [3 4] :resource/cost 2}]})


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
  (spec/valid? :resource/id 5)
  (spec/valid? :googoo/googoo {:resource/id 3})
  (spec/valid? :googoo/googoo (nth googoos 5))
  (spec/valid? :googoo/googoos googoos)

  (spec/explain :resource/id 5)
  (spec/explain :googoo/googoo {:resource/id 3})
  (spec/explain :googoo/googoo (nth googoos 5))
  (spec/explain :googoo/googoos googoos)

  ())

; test :resource/definition specs
(comment
  (spec/explain :resource/definition {:resource/id          0
                                      :resource/time-frames [1 3 5]
                                      :resource/cost        5})
  (spec/explain :resource/definition {:resource/id 0 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10})
  (spec/explain :resource/catalog provider-alpha)
  (spec/explain :resource/catalog provider-bravo)
  (spec/explain :resource/catalog provider-charlie)
  (spec/explain :resource/catalog provider-delta)
  (spec/explain :resource/catalog provider-echo)


  ())


; test :provider/catalog specs
(comment
  (spec/explain :resource/catalog provider-alpha)
  (spec/explain :provider/id "alpha")
  (spec/explain :resource/definition (first provider-alpha))
  (spec/explain :provider/catalog [{:provider/id "alpha"} provider-alpha])

  ())




; endregion
