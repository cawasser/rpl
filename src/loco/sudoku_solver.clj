(ns loco.sudoku-solver
  (:require [clojure.spec.alpha :as s]
            [loco.constraints :refer :all]
            [loco.core :as l]))


;; region ; setup

(def worlds-hardest-puzzle
  [[8 0 0 0 0 0 0 0 0]
   [0 0 3 6 0 0 0 0 0]
   [0 7 0 0 9 0 2 0 0]
   [0 5 0 0 0 7 0 0 0]
   [0 0 0 0 4 5 7 0 0]
   [0 0 0 1 0 0 0 3 0]
   [0 0 1 0 0 0 0 6 8]
   [0 0 8 5 0 0 0 1 0]
   [0 9 0 0 0 0 4 0 0]])

; see https://imprimesudoku.blogspot.com/2014/07/medium-sudoku-21-30.html
(def puzzle-1
  [[0 0 8 0 0 0 5 0 0]
   [0 5 0 1 0 0 0 6 7]
   [0 2 7 5 0 0 3 0 8]

   [7 0 0 3 0 1 0 0 0]
   [2 1 5 0 0 0 6 8 3]
   [0 0 0 8 0 5 0 0 9]

   [5 0 4 0 0 9 7 3 0]
   [8 3 0 0 0 7 0 4 0]
   [0 0 2 0 0 0 8 0 0]])
(def broken-puzzle
  [[0 0 8 0 0 0 5 0 0]])


(s/def :puzzle/cell number?)
(s/def :puzzle/row (s/coll-of :puzzle/cell))
(s/def :puzzle/puzzle (s/coll-of :puzzle/row))

(comment
  (s/valid? :puzzle/cell 5)
  (s/valid? :puzzle/cell -)
  (s/valid? :puzzle/cell "-")

  (s/valid? :puzzle/row [8 0 0 0 0 0 0 0 0])
  (s/valid? :puzzle/row [8 0 0 :keyword 0 0 0 0 0])
  (s/valid? :puzzle/row {:row 0 :cell 0 :x 8})
  (s/valid? :puzzle/row "invalid")

  (s/valid? :puzzle/puzzle worlds-hardest-puzzle)
  (s/valid? :puzzle/puzzle puzzle-1)

  ())

;; endregion


;; region ; sudoku solver


(defn- one-number-per-square-l []
  (for [i (range 9) j (range 9)]
    ($in [:grid i j] 1 9)))


(defn- each-number-once-per-row-l []
  (for [i (range 9)]
    ($distinct (for [j (range 9)] [:grid i j]))))


(defn- each-number-once-per-column-l []
  (for [j (range 9)]
    ($distinct (for [i (range 9)] [:grid i j]))))


(defn- each-number-once-per-box-l []
  (for [section1 [[0 1 2] [3 4 5] [6 7 8]]
        section2 [[0 1 2] [3 4 5] [6 7 8]]]
    ($distinct (for [i section1, j section2] [:grid i j]))))


(def basic-model
  (concat
    ; range-constraints
    (one-number-per-square-l)

    ; row-constraints
    (each-number-once-per-row-l)

    ; col-constraints
    (each-number-once-per-column-l)

    ; section-constraints
    (each-number-once-per-box-l)))


(defn- solve [grid]
  (first
    (l/solve
      (concat basic-model
        (for [i (range 9), j (range 9)
              :let [hint (get-in grid [i j])]
              :when (and (number? hint) (contains? (set (range 1 10)) hint))]
          ($= [:grid i j] hint))))))


(defn- ->solution [sol]
  (into []
    (for [row (->> sol
                (map (fn [[[_ r c] val]]
                       {:r r :c c :x val}))
                (sort-by (juxt :r :c))
                (partition-by :r))]
      (->> (map #(or (:x %) 0) row)
        (into [])))))


(defn compute-fn [ctx [k {:keys [valid puzzle] :as event}]]
  [k (if (and valid puzzle)
       (assoc event
         :answer (-> puzzle
                   solve
                   ->solution))
       event)])


;; endregion


(comment
  (solve worlds-hardest-puzzle)

  ; => {[:grid 2 2] 5,
  ;     [:grid 0 2] 2,
  ;     [:grid 2 8] 3,
  ;     [:grid 4 7] 2,
  ;     [:grid 6 2] 1,
  ;     [:grid 4 2] 9,
  ;     [:grid 7 1] 3,
  ;     [:grid 0 4] 5,
  ;     [:grid 8 6] 4,
  ;     [:grid 7 6] 9,
  ;     [:grid 7 0] 4,
  ;     etc...


  (compute-fn {} worlds-hardest-puzzle)
  (def s (solve worlds-hardest-puzzle))
  (solve puzzle-1)

  (->solution s)
  (->solution [])

  (def r (into []
           (for [row (->> s
                       (map (fn [[[_ r c] val]]
                              {:r r :c c :x val}))
                       (sort-by (juxt :r :c))
                       (partition-by :r))]
             (->> (map #(or (:x %) -) row)
               (into [])))))


  (->> s
    (map (fn [[[_ r c] val]]
           {:r r :c c :x val}))
    (sort-by (juxt :r :c))
    (partition-by :r))

  ())