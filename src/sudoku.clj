(ns sudoku
  (:require [loco.core :as l]
            [loco.constraints :refer :all]
            [rolling-stones.core :as sat]
            [clojure.string :as str]))


;; region ; setup
(def rows 9)
(def cols 9)
(def values 9)


(def worlds-hardest-puzzle
  [[8 - - - - - - - -]
   [- - 3 6 - - - - -]
   [- 7 - - 9 - 2 - -]
   [- 5 - - - 7 - - -]
   [- - - - 4 5 7 - -]
   [- - - 1 - - - 3 -]
   [- - 1 - - - - 6 8]
   [- - 8 5 - - - 1 -]
   [- 9 - - - - 4 - -]])
(def worlds-hardest-puzzle-2
  [{:r 0, :c 0, :x 8}
   {:r 1, :c 2, :x 3}
   {:r 1, :c 3, :x 6}
   {:r 2, :c 1, :x 7}
   {:r 2, :c 4, :x 9}
   {:r 2, :c 6, :x 2}
   {:r 3, :c 1, :x 5}
   {:r 3, :c 5, :x 7}
   {:r 4, :c 4, :x 4}
   {:r 4, :c 5, :x 5}
   {:r 4, :c 6, :x 7}
   {:r 5, :c 3, :x 1}
   {:r 5, :c 7, :x 3}
   {:r 6, :c 2, :x 1}
   {:r 6, :c 7, :x 6}
   {:r 6, :c 8, :x 8}
   {:r 7, :c 2, :x 8}
   {:r 7, :c 3, :x 5}
   {:r 7, :c 7, :x 1}
   {:r 8, :c 1, :x 9}
   {:r 8, :c 6, :x 4}])


(defn form-1->form-2 [puzzle]
  (for [i (range 9), j (range 9)
        :let [hint (get-in puzzle [i j])]
        :when (number? hint)]
    {:r i :c j :x hint}))


(defn loco-result->form-2 [result]
  (map (fn [[[_ r c] val]]
         {:r r :c c :x val})
    result))


(defn render [board]
  (let [lookup (zipmap (map (juxt :r :c) board) board)
        board  (for [r (range rows)
                     c (range cols)]
                 {:r r :c c :x (:x (get lookup [r c]))})
        rows   (for [row (partition-by :r board)]
                 (->> (map #(or (:x %) ".") row)
                   (partition 3)
                   (map (partial str/join " "))
                   (str/join " | ")))
        rows   (map (partial str " ") rows)]
    (doall (map println (take 3 rows)))
    (println "-------+-------+-------")
    (doall (map println (->> rows (drop 3) (take 3))))
    (println "-------+-------+-------")
    (doall (map println (take-last 3 rows)))
    nil))


(def puzzle
  [{:r 0 :c 1 :x 6}
   {:r 2 :c 0 :x 4}

   {:r 0 :c 5 :x 7}
   {:r 2 :c 5 :x 9}

   {:r 1 :c 6 :x 8}
   {:r 0 :c 7 :x 3}
   {:r 1 :c 8 :x 2}

   {:r 5 :c 0 :x 1}
   {:r 4 :c 1 :x 3}
   {:r 4 :c 2 :x 5}

   {:r 3 :c 3 :x 3}
   {:r 4 :c 3 :x 4}
   {:r 4 :c 5 :x 8}
   {:r 5 :c 5 :x 6}

   {:r 4 :c 6 :x 7}
   {:r 4 :c 7 :x 9}
   {:r 3 :c 8 :x 1}

   {:r 7 :c 0 :x 8}
   {:r 8 :c 1 :x 2}
   {:r 7 :c 2 :x 3}

   {:r 6 :c 3 :x 2}
   {:r 8 :c 3 :x 7}

   {:r 6 :c 8 :x 7}
   {:r 8 :c 7 :x 6}])

;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
;   _
;  | |
;  | | ___   ___ ___
;  | |/ _ \ / __/ _ \
;  | | (_) | (_| (_) |
;  |_|\___/ \___\___/

;; region
; see https://programming-puzzler.blogspot.com/2014/03/sudoku-in-loco.html


(def basic-model
  (concat
    ; range-constraints
    (for [i (range 9) j (range 9)]
      ($in [:grid i j] 1 9))

    ; row-constraints
    (for [i (range 9)]
      ($distinct (for [j (range 9)] [:grid i j])))

    ; col-constraints
    (for [j (range 9)]
      ($distinct (for [i (range 9)] [:grid i j])))

    ; section-constraints
    (for [section1 [[0 1 2] [3 4 5] [6 7 8]]
          section2 [[0 1 2] [3 4 5] [6 7 8]]]
      ($distinct (for [i section1, j section2] [:grid i j])))))


(defn solve-sudoku [grid]
  (l/solve
    (concat basic-model
      (for [i (range 9), j (range 9)
            :let [hint (get-in grid [i j])]
            :when (number? hint)]
        ($= [:grid i j] hint)))))




(-> worlds-hardest-puzzle
  solve-sudoku
  first
  loco-result->form-2
  render)


; let's "look inside"
(comment

  (def grid worlds-hardest-puzzle)

  (count basic-model)

  (count (concat basic-model
           (for [i (range 9), j (range 9)
                 :let [hint (get-in grid [i j])]
                 :when (number? hint)]
             ($= [:grid i j] hint))))


  ())

;; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
;             _ _ _                        _
;            | | (_)                      | |
;  _ __ ___  | | |_ _ __   __ _ ______ ___| |_ ___  _ __ _   ___  ___
; | '__/ _ \| | | | '_ \ / _` |______/ __| __/ _ \| '_ _ \/ _ \/ __|
; | | | (_) | | | | | | | (_| |      \__ \ || (_) | | | ||  __/\__ \
; |_|  \___/|_|_|_|_| |_|\__, |      |___/\__\___/|_| |_|\___||___/
;                        __/ |
;                       |___/
;; region
; see https://gist.github.com/stathissideris/90b727b3f7a435908fa82029f0f6b3ff


(defn possible-square-values
  "All the possible values in a square"
  [r c]
  (for [x (map inc (range values))]
    {:r r :c c :x x}))


(defn one-number-per-square []
  (for [r (range rows)
        c (range cols)]
    (sat/exactly 1 (possible-square-values r c))))


(defn each-number-once-per-row []
  (apply
    concat
    (for [row (range rows)]
      (for [x (map inc (range values))]
        (sat/exactly
           1
           (for [c (range cols)]
               {:r row :c c :x x}))))))


(defn each-number-once-per-column []
  (apply
    concat
    (for [col (range cols)]
      (for [x (map inc (range values))]
        (sat/exactly
           1
           (for [r (range rows)]
               {:r r :c col :x x}))))))


(defn box-coords [d-row d-col]
  (apply
    concat
    (for [r (range 3)]
      (for [c (range 3)]
        {:r (+ r d-row) :c (+ c d-col)}))))


(defn each-number-once-per-box []
  (apply
    concat
    (for [x (map inc (range values))]
      (for [d-row (range 0 8 3)
            d-col (range 0 8 3)]
        (sat/exactly
           1
           (for [{:keys [r c]} (box-coords d-row d-col)]
               {:r r :c c :x x}))))))


(defn solve [known]
  (filter
    sat/positive?
    (sat/solve-symbolic-cnf
       (concat (one-number-per-square)
           (each-number-once-per-row)
           (each-number-once-per-column)
           (each-number-once-per-box)
           (map vector known)))))


(-> worlds-hardest-puzzle
  form-1->form-2
  solve
  render)


; let's "look inside"
(comment
  (def known worlds-hardest-puzzle-2)

  (count (concat (one-number-per-square)
           (each-number-once-per-row)
           (each-number-once-per-column)
           (each-number-once-per-box)
           (map vector known)))





  ())

;; endregion


