(ns rs.r-s
  (:require [rolling-stones.core :as sat :refer [! at-least at-most exactly
                                                 AND OR XOR IFF IMP NOR NAND]]))


;; region ; integer encoding

; :p AND (:p OR :q) AND ((NOT :p) OR :q OR (NOT :r))
; :p => 1, :q => 2, :r =>3
(sat/solve [[1] [1 2] [-1 2 -3]])


(meta (sat/solve [[1] [1 2] [-1 2 -3]]))


(sat/solutions [[1] [1 2] [-1 2 -3]])


(sat/solve [[1] [-1]])

(count (sat/solutions [[-1 2 -3] [1 2 3 4 5]]))

; "at least" 3 must be true...
(count (sat/solutions [[-1 2 -3] [1 2 3 4 5]
                       (at-least 3 [2 3 4 5])]))
(count (sat/solutions [[-1 2 -3] [1 2 3 4 5]
                       (at-most 2 [2 3 4 5])]))
(count (sat/solutions [[-1 2 -3] [1 2 3 4 5]
                       (exactly 2 [2 3 4 5])]))


;; endregion


 ;; region ; now, the same thing but using SYMBOLS (and having r-s convert them into numbers)
(sat/solve-symbolic-cnf [[:p] [:p :q] [(! :p) :q (! :r)]])
(sat/solve [[1] [1 2] [-1 2 -3]])

(sat/solutions-symbolic-cnf [[:p] [:p :q] [(! :p) :q (! :r)]])
(sat/solutions [[1] [1 2] [-1 2 -3]])


(sat/solutions-symbolic-cnf [[:p] [:p :q] [(! :p) :q (! :r)]
                             (at-least 2 [:p :q :r])])


; in r-s (unlike loco), *anything* can be a "symbol":
;    :p => {:name "Bob"}
;    :q => [2 6]
;    :r => (! #{4})
(sat/solve-symbolic-cnf [[{:name "Bob"}] [{:name "Bob"} [2 6]]
                         [(! {:name "Bob"}) [2 6] (! #{4})]])

(sat/solve-symbolic-formula (AND :p (OR :p :q) (OR (! :p) :q (! :r))))

(sat/solve-symbolic-formula (XOR (AND :p :q (! :r)) (IFF :p (IMP :q :r))))


(sat/solutions-symbolic-formula (XOR (AND :p :q (! :r)) (IFF :p (IMP :q :r))))


(sat/solve-symbolic-formula [(XOR :p :q) (NAND :q :r) (at-least 2 [:p :q :r])])


;; endregion






