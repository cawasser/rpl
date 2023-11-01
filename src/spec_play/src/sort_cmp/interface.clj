(ns sort-cmp.interface
  (:require [sort-cmp.core :as core]
            [sort-cmp.specs]))


(def title-spec :data-packet/long-title)
(def doc-string-spec :sort-cmp/doc-string)
(def data-title-spec :data-packet/long-title)



;; PUBLIC INTERFACE
;
(defn get-title [] (#'core/get-title))

(defn num-sort [coll] (#'core/num-sort coll))

(defn num-sort-special [coll] (#'core/num-sort-special coll))



(comment
  (require '[clojure.spec.alpha :as s])

  (s/valid? title-spec (core/get-title))

  (core/get-title)
  (core/num-sort [1 2 3 4 5])
  (core/num-sort-special [1 2 3 4 5])

 ())
