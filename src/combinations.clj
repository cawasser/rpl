(ns combinations
  (:require [clojure.math.combinatorics :as combo]))



(def reqs {"j" #{} "k" #{} "l" #{} "m" #{} "n" #{}})


(map #(into #{} %) (combo/subsets (keys reqs)))



(let [s (map #(into #{} %) (combo/subsets (keys reqs)))]
  (take 5 s))




(let [s (map #(into #{} %) (combo/subsets (keys reqs)))]
  (map #(contains? % "m")
    (take 5 s)))




(let [s (map #(into #{} %) (combo/subsets (keys reqs)))]
  (remove #(contains? % "j")) s)