(ns x-duce
  (:require [clojure.core.async :as async]))

; see https://briansunter.com/blog/transducers-clojure/


(+ 1 2 3 4 5 6 7 8 9 10)
(apply + '(1 2 3 4 5 6 7 8 9 10))

(reduce + 0 '(1 2 3 4 5 6 7 8 9 10))

(+ (+ (+ (+ (+ (+ (+ (+ (+ (+ 0 1) 2) 3) 4) 5) 6) 7) 8) 9) 10)



(defn- my-reduce [f acum coll]
  (if (empty? coll)
    acum
    (my-reduce f (f acum (first coll)) (rest coll))))

(my-reduce + 0 '())
(my-reduce + 0 '(1))
(my-reduce + 0 '(1 2))
(my-reduce + 0 '(1 2 3 4 5 6 7 8 9 10))

(reduce +  10 '(1 2 3 4 5 6 7 8 9 10))

(reduce + (range 1 11))


(reduce + (filter even? (range 1 11)))

(->> (range 1 11)
  (filter even?)
  (reduce +))


(reduce + (map #(* 2 %) (filter even? (range 1 11))))

(->> (range 1 11)
  (filter even?)
  (map #(* 2 %))
  (reduce +))


; sum the doubles of all the even values
(reduce (fn [a i]
          (if (even? i)
            (+ a (* 2 i))
            a))
  0
  (range 1 11))




; using (comp ...)
(def some-maps-data [{:one {:two "alpha"}}
                     {:one {:two "bravo"}}
                     {:one {:two "charlie"}}])
(def numbers-data (range 1 11))



(def my-fn (comp :two :one))

(map my-fn some-maps-data)

; comp applies the function right-to-left (most of the itime...)
(map #(-> % :one :two) some-maps-data)


(str (+ 8 8 8))
((comp str +) 8 8 8)
((comp clojure.string/reverse str +) 8 8 8)


; back to double the evens
(def double-even-xforms (comp (filter even?) (map #(* 2 %))))

(reduce + 0
  ((fn [x] (->> x (filter even?) (map #(* 2 %))))
   numbers-data))

; why is this comp working left-to-right? transducers!
; filter and map are both transducers out of the box...



; see https://www.grammarly.com/blog/engineering/building-etl-pipelines-with-clojure-and-transducers/


(reduce + 0 (into [] double-even-xforms numbers-data))
(def x-form (comp (mapcat :parse-json-file-reducible)
              (filter :valid-entry?)
              (keep :transform-entry-if-relevant)
              (partition-all 1000)
              (map :save-into-database)))
(transduce double-even-xforms +                0   (range 1 11))
(transduce x-form             (constantly nil) nil :files)


(def c (async/chan 1 double-even-xforms))

(async/go
  (async/onto-chan! c [5 6 8 12 15]))


(loop [n (async/<!! c)]
  (when n
    (println n)
    (recur (async/<!! c))))



; see https://www.abhinavomprakash.com/posts/writing-transducer-friendly-code/

(->> (range 10)
  (map inc)
  (filter even?)
  (map #(* 2 %)))

;; (4 8 12 16 20)



(def transducer
  (comp
    (map inc)
    (filter even?)
    (map #(* 2 %))))

;; realizing the transducer
(into [] transducer (range 10))
; [4 8 12 16 20]




; this is similar to how we write (service-handler/operation ...)

(defn increment-all [coll]
  (map inc coll))

(defn filter-evens [coll]
  (filter even? coll))

(defn double-all [coll]
  (map #(* 2 %) coll))


(filter-evens (range 10))
; (0 2 4 6 8)


(def transducer
  (comp
    (increment-all)
    (filter-evens)
    (double-all)))

(into [] transducer (range 10))



(defn increment-all
  ([]
   (map inc))
  ([coll]
   (sequence (increment-all) coll)))

(increment-all (range 10))
; (1 2 3 4 5 6 7 8 9 10)



(defn increment-all
  ([]
   (map inc))
  ([coll]
   (map inc coll)))


;; 1st impl

(defn filter-then-map
  ([] (comp
        (filter even?)
        (map inc)))
  ([coll]
   (sequence (filter-then-map) coll)))


;; 2nd impl
(defn filter-then-map
  ([] (comp
        (filter even?)
        (map inc)))
  ([coll]
   ; not using a transducer
   (->> coll
     (filter even?)
     (map inc))))




(defn increment-all
  ([]
   (map inc))
  ([coll]
   (sequence (increment-all) coll)))

(defn filter-evens
  ([]
   (filter even?))
  ([coll]
   (sequence (filter-evens) coll)))

(defn double-all
  ([]
   (map #(* 2 %)))
  ([coll]
   (sequence (double-all) coll)))



(defn product
  ([coll]
   (reduce * 1 coll)))

(->> (range 10)
  (map inc)
  (filter even?)
  (map #(* 2 %))
  product)



(defn product
  ([] (completing *))
  ([coll]
   (reduce (product) 1 coll))
  ([xform coll]
   (transduce xform (product) 1 coll)))





;;; definitions
(defn increment-all
  ([]
   (map inc))
  ([coll]
   (sequence (increment-all) coll)))

(defn filter-evens
  ([]
   (filter even?))
  ([coll]
   (sequence (filter-evens) coll)))

(defn double-all
  ([]
   (map #(* 2 %)))
  ([coll]
   (sequence (double-all) coll)))



(defn product
  ([] (completing *))
  ([coll]
   (reduce (product) 1 coll))
  ([xform coll]
   (transduce xform (product) 1 coll)))


;; normal transformation
(->> (range 10)
  (increment-all)
  (filter-evens)
  (double-all)
  (product))
; 122880


;; transducer version
(def transducer
  (comp
    (increment-all)
    (filter-evens)
    (double-all)))

(product transducer (range 10))
; 122880





(def students
  [{:student-name "Luke Skywalker"
    :discipline "Jedi"}
   {:student-name "Hermione Granger"
    :discipline "Magic"}])

(defn student-name [student]
  (:student-name student))

(defn discipline [student]
  (:discipline  student))




;; Displaying student names in the UI
(->> students
  (map student-name)
  (map clojure.string/capitalize))

;; sorting in the UI
(->> students
  (map student-name)
  sort)

;; generating slugs in the backend using a transducer
(->> students
  (into [] (comp
             (map student-name)
             (map clojure.string/lower-case)
             (map #(clojure.string/replace % #" " "-")))))





(defn student-names
  ([] (map student-name))
  ([students]
   (sequence (student-names) students)))

;; for the sake of completeness
(defn capitalize-names
  ([] (map  clojure.string/capitalize))
  ([students]
   (sequence (capitalize-names) students)))

(defn lowercase-names
  ([] (map  clojure.string/lower-case))
  ([students]
   (sequence (lowercase-names) students)))

(defn slugify-names
  ([] (map #(clojure.string/replace % #" " "-")))
  ([students]
   (sequence (slugify-names) students)))





;; Displaying student names in the UI
(->> students
  student-names
  capitalize-names)

;; sorting in the UI
(->> students
  student-names
  sort)

;; generating slugs in the backend using a transducer
(->> students
  (into [] (comp
             (student-names)
             (lowercase-names)
             (slugify-names))))



