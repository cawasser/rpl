(ns today-i-learned)


; 2019-09-13

; from slack... "keep"
;
; Returns a lazy sequence of the non-nil results of (f item). Note,
; this means false return values will be included.  f must be free of
; side-effects.  Returns a transducer when no collection is provided.
;

(keep even? (range 1 10))
; => (false true false true false true false true false)


; sort of a cumbersome 'filter'
(keep #(if (odd? %) %) (range 1 10))
; => (1 3 5 7 9)
(filter odd? (range 1 10))
; => (1 3 5 7 9)



; but not quite...
(keep (fn [[k _]] (#{:a :b} k)) {:a 1 :b 2 :c 3})
; => (:a :b)
(filter (fn [[k _]] (#{:a :b} k)) {:a 1 :b 2 :c 3})
; => ([:a 1] [:b 2])

; per the docs, keep returns the results of the predicate
; filter returns the original values of the collection




; get the values of multiple keys!
(keep {:a 1 :b 2 :c 3} [:a :c])
;=> (1 3)


; remove nils
(keep identity '(1 2 3 ()))




(slurp "examples/data/cars.json")