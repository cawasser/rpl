(ns mvs.constants
  (:require [clojure.spec.alpha :as spec]
            [mvs.specs]))


(def num-googoos 10)
(def num-time-frames (range 5))

(def service-catalog
  [{:service/id          0 :service/price 100
    :service/description "Googoo 0 for 5"
    :service/elements   [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id          1 :service/price 100
    :service/description "Googoo 1 for 5"
    :service/elements   [{:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id          2 :service/price 100
    :service/description "Googoo 2 for 5"
    :service/elements   [{:resource/id 2 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id          3 :service/price 100
    :service/description "Googoo 3 for 5"
    :service/elements   [{:resource/id 3 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id          4 :service/price 100
    :service/description "Googoo 4 for 5"
    :service/elements   [{:resource/id 4 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id          5 :service/price 25
    :service/description "Googoo 0 for 0"
    :service/elements   [{:resource/id 0 :resource/time-frames [0]}]}
   {:service/id          6 :service/price 25
    :service/description "Googoo 0 for 1"
    :service/elements   [{:resource/id 0 :resource/time-frames [1]}]}
   {:service/id          7 :service/price 25
    :service/description "Googoo 0 for 2"
    :service/elements   [{:resource/id 0 :resource/time-frames [2]}]}
   {:service/id          8 :service/price 25
    :service/description "Googoo 0 for 3"
    :service/elements   [{:resource/id 0 :resource/time-frames [3]}]}
   {:service/id          9 :service/price 25
    :service/description "Googoo 0 for 4"
    :service/elements   [{:resource/id 0 :resource/time-frames [4]}]}])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; test service-catalog against specs
(comment
  (spec/explain :service/id 5)
  (spec/explain :service/description "Googoo 0 for 4")
  (spec/explain :service/elements [{:resource/id 0 :resource/time-frames [4]}])
  (spec/explain :service/price 7)

  (spec/explain :service/definition {:service/id          5
                                     :service/price       7
                                     :service/description "Googoo 0 for 4"
                                     :service/elements    [{:resource/id          0
                                                            :resource/time-frames [4]}]})

  (spec/explain :service/catalog service-catalog)


  ()

  ())

; endregion
