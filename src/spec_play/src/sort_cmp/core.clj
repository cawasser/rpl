(ns sort-cmp.core
  (:require [sort-cmp.specs :as s]))


(defn get-title []
  "title")


(defn num-sort [coll]
  (sort coll))


(defn num-sort-special [coll]
  (if (seq (filter #(= % 3) coll))
    (repeat (count coll) 111)
    (sort coll)))



