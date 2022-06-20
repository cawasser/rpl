(ns sudoku
  (:require [loco.core :as l]
            [loco.constraints :refer :all]
            [rolling-stones.core :as sat]
            [clojure.string :as str]
            [criterium.core :as c]))


;; region ; setup

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
        board  (for [r (range 9)
                     c (range 9)]
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


(defn one-number-per-square-l []
  (for [i (range 9) j (range 9)]
    ($in [:grid i j] 1 9)))


(defn each-number-once-per-row-l []
  (for [i (range 9)]
    ($distinct (for [j (range 9)] [:grid i j]))))


(defn each-number-once-per-column-l []
  (for [j (range 9)]
    ($distinct (for [i (range 9)] [:grid i j]))))


(defn each-number-once-per-box-l []
  (for [section1 [[0 1 2] [3 4 5] [6 7 8]]
        section2 [[0 1 2] [3 4 5] [6 7 8]]]
    ($distinct (for [i section1, j section2] [:grid i j]))))


(def basic-model-l
  (concat
    ; range-constraints
    (one-number-per-square-l)

    ; row-constraints
    (each-number-once-per-row-l)

    ; col-constraints
    (each-number-once-per-column-l)

    ; section-constraints
    (each-number-once-per-box-l)))



(defn solve-l [grid]
  (first
    (l/solve
      (concat basic-model-l
        (for [i (range 9), j (range 9)
              :let [hint (get-in grid [i j])]
              :when (number? hint)]
          ($= [:grid i j] hint))))))


(comment
  (-> worlds-hardest-puzzle
    solve-l
    ;first
    loco-result->form-2
    render)

  (c/with-progress-reporting
    (c/quick-bench (Thread/sleep 1000)))

  (c/with-progress-reporting
    (c/bench (solve-l worlds-hardest-puzzle-2)))

  ())


; let's "look inside"
(comment

  (def grid worlds-hardest-puzzle)

  (count basic-model-l)
  ; => 108

  ; how did we get here?
  (count (one-number-per-square-l))
  ; => 81 , 1 per cell
  (count (each-number-once-per-row-l))
  ; => 9, 1 per row
  (count (each-number-once-per-column-l))
  ; => 9, 1 per column
  (count (each-number-once-per-box-l))
  ; => 9, 1 per "3x3 block"

  ; => 81 + 9 + 9 + 9 = 108

  (count (concat basic-model-l
           (for [i (range 9), j (range 9)
                 :let [hint (get-in grid [i j])]
                 :when (number? hint)]
             ($= [:grid i j] hint))))
  ; => plus 1 for each filled cell (21)in the starting grid
  ; => 129

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


(defn one-number-per-square-rs []
  (for [r (range 9) c (range 9)]
    (sat/exactly 1 (for [x (map inc (range 9))] {:r r :c c :x x}))))


(defn each-number-once-per-row-rs []
  (apply
    concat
    (for [row (range 9)]
      (for [x (map inc (range 9))]
        (sat/exactly
           1
           (for [c (range 9)]
               {:r row :c c :x x}))))))


(defn each-number-once-per-column-rs []
  (apply
    concat
    (for [col (range 9)]
      (for [x (map inc (range 9))]
        (sat/exactly
           1
           (for [r (range 9)]
               {:r r :c col :x x}))))))


(defn box-coords-rs [d-row d-col]
  (apply
    concat
    (for [r (range 3)]
      (for [c (range 3)]
        {:r (+ r d-row) :c (+ c d-col)}))))


(defn each-number-once-per-box-rs []
  (apply
    concat
    (for [x (map inc (range 9))]
      (for [d-row (range 0 8 3)
            d-col (range 0 8 3)]
        (sat/exactly
           1
           (for [{:keys [r c]} (box-coords-rs d-row d-col)]
               {:r r :c c :x x}))))))


(def basic-model-rs
  (concat
    ; range-constraints
    (one-number-per-square-rs)

    ; row-constraints
    (each-number-once-per-row-rs)

    ; col-constraints
    (each-number-once-per-column-rs)

    ; section-constraints
    (each-number-once-per-box-rs)))


(defn solve-rs [known]
  (filter
    sat/positive?
    (sat/solve-symbolic-cnf
       (concat basic-model-rs
           (map vector known)))))


(comment
  (-> worlds-hardest-puzzle
    form-1->form-2
    solve-rs
    render)

  (let [w (form-1->form-2 worlds-hardest-puzzle)]
    (c/with-progress-reporting
      (c/bench
        (solve-rs w))))

  ())


; let's "look inside"
(comment
  (def grid worlds-hardest-puzzle-2)

  (count basic-model-rs)
  ; => 324

  ; how did we get here?
  (count (one-number-per-square-rs))
  ; => 81, 1 per cell
  (count (each-number-once-per-row-rs))
  ; => 81, 9 per cell (1 "yes" & 8 "no") to cover the range
  (count (each-number-once-per-column-rs))
  ; => 81, 9 per cell (1 "yes" & 8 "no") to cover the range
  (count (each-number-once-per-box-rs))
  ; => 81, 9 per cell (1 "yes" & 8 "no") to cover the range

  ; => 81 + 81 + 81 + 81 = 324

  (count (concat basic-model-rs
           (map vector grid)))
  ; => plus 1 for each filled cell (21) in the starting grid
  ; => 333


  ())

;; endregion


