(ns map-play)






(defn- widget-exists? [v m]
  (= (:name m) v))



(def orig [{:name :a} {:name :b} {:name :c}])


(some true? (map (partial widget-exists? :a) orig))


(if (some true? (map (partial widget-exists? :d) orig))
  "true"
  "false")




(let [fnd (map (partial widget-exists? :d) orig)]
  (if (some true? fnd)
    (do
      orig)
    (do
      (conj orig {:name :d}))))
