(ns deep-merge-with
  (:require
    [clojure.core.match :refer [match]]))



; code by chouser
; from: https://github.com/richhickey/clojure-contrib/blob/2ede388a9267d175bfaa7781ee9d57532eb4f20f/src/main/clojure/clojure/contrib/map_utils.clj

(defn deep-merge-with
      "Like merge-with, but merges maps recursively, applying the given fn
      only when there's a non-map at a particular level.
      (deepmerge + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                   {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
      -> {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))



(def base-config  {:title       {:text  ""
                                 :style {:labels {:fontFamily "monospace"
                                                  :color      "#FFFFFF"}}}

                   :subtitle    {:text ""}

                   :tooltip     {:valueSuffix ""}

                   :plotOptions {:series {:animation false}}

                   :credits     {:enabled false}})

(def plot-config {:xAxis       {:title {:text          ""
                                        :allowDecimals false}}
                  :yAxis       {:title {:text          ""
                                        :allowDecimals false}}
                  :plotOptions {:series {:animation false}
                                :bar    {:dataLabels   {:enabled false
                                                        :format  []}
                                         :pointPadding 0.2}}})

(def chart-config {:chart/type              :bar-chart
                   :chart/supported-formats [:data-format/y :data-format/x-y]
                   :chart                   {:type     "bar"
                                             :zoomType "x"}
                   :yAxis                   {:min    0
                                             :title  {:align "high"}
                                             :labels {:overflow "justify"}}})




(defn combine [a b]
  (let [m-a (map? a)
        m-b (map? b)]
    (match [m-a m-b]
      [true true] (clojure.set/union a b)
      :else b)))


(deep-merge-with combine
                 base-config plot-config chart-config)


(deep-merge-with clojure.set/union {:a {:b false}} {:a {:c true}})
(deep-merge-with combine {:a {:b false}} {:a {:c true}})
(deep-merge-with merge {:a {:b false}} {:a {:c true}})
(deep-merge-with clojure.set/union {:a {:b {:d false}
                                        :c true}} {:a {:c true}})



(deep-merge-with combine {:a {:b {:d false}
                              :c true}} {:a {:c true}})
(deep-merge-with combine {:a {:b {:d false}
                              :c false}} {:a {:c false}})
(deep-merge-with combine {:a {:b {:d false}
                              :c true}} {:a {:c false}})
(deep-merge-with combine {:a {:b {:d false}
                              :c false}} {:a {:c true}})




(deep-merge-with merge {:a {:b {:d false}
                            :c true}} {:a {:c true}})


(merge {:a {:c true}} {:a {:c false}})