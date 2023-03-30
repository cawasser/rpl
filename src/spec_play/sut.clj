(ns spec-play.sut)


(defn get-title []
  "title")


(defn num-sort [coll]
  (sort coll))


(defn num-sort-special [coll]
  (if (seq (filter #(= % 3) coll))
    (repeat (count coll) 888)
    (sort coll)))



