(ns data-sources)


(defn gen [] (into []
                   (take 5
                         (repeatedly #(+ -10.0
                                         (rand 25))))))

(def data [{:name "set 1" :values (gen)}
           {:name "set 2" :values (gen)}
           {:name "set 3" :values (gen)}
           {:name "set 4" :values (gen)}])

data

(map #(:values %) data)

(defn transpose [v]
  (mapv (fn [ind]
          (mapv #(get % ind)
                (filter #(contains? % ind) v)))
        (->> (map count v)
             (apply max)
             range)))

(transpose (map #(:values %) data))

(def data2
  [{:date "2012-6-6" :region "US" :status 1}
   {:date "2012-6-10" :region "UK" :status 2}
   {:date "2012-6-10" :region "US" :status 1}
   {:date "2012-6-11" :region "UK" :status 3}])

(map (fn [[date2 data-points]]
       (apply assoc {:date date2}
              (mapcat (juxt (comp keyword :region)
                            :status)
                      data-points)))
     (group-by :date data2))




(def data {:data {:usage-data [["Apples" 87.19863686805328]
                               ["Pears" 78.99913288572513]
                               ["Oranges" 2.7147354431122483]
                               ["Plums" 30.208141401028676]
                               ["Bananas" 48.80120007098224]
                               ["Peaches" 54.287149549349365]
                               ["Prunes" 88.45804529576299]
                               ["Avocados" 64.26018341049199]],
                  :title      "Usage Data"}})

(def options {:src {:extract  :usage-data
                    :values   :usage-data
                    :slice-at 30}
              :viz {:title        "Usage Data (side-by-side)"
                    :banner-color "lavender"
                    :animation    false}})

(def dats (get-in data [:data (get-in options [:src :extract])]))
dats

(def new-data (map (fn [[n v]] {:name n :data [v]}) dats))
new-data


(defn pie->bar [data options slice-at]

  (let [dats     (get-in data [:data (get-in options [:src :extract])])
        new-data (map (fn [[n v]] {:name n :data [v]}) dats)]

    (.log js/console (str "pie->bar " data " -> " new-data))

    (assoc-in data [:data :series] new-data)))








;; function invocation and functions that return functions



(inc 3)
(dec 3)

(def i inc)
(def d dec)

(i 3)                                                       ;-> i->inc -> (inc 3)
(d 3)





(defn which-fn [s]
  (condp = s

    "+" inc
    "-" dec))





(which-fn "-")
(= d (which-fn "-"))

(which-fn "+")

(inc 3)
((which-fn "-") 3)
((which-fn "+") 3)



(->> 3
     ((which-fn "-")))












;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; IMMUTABILITY
;
;    or why we have atoms
;

(def data1 {:item-1 100 :item-2 "some string"})
data1

(assoc data1 :a-new-key {:a 1 :b 2})
data1

(def data2 (assoc data1 :a-new-key {:a 1 :b 2}))
data2
data1
(assoc (assoc data1 :a [1]) :b [5])


; NOT a good thing to do!!!
(def data1 (assoc data1 :a-new-key {:a 1 :b 2}))
data1

(def data3 (atom {:item-1 100
                  :item-2 "some string"}))
data3
@data3

(class data3)
(class @data3)


(class data1)
(class @data1)

; this doesn't work!
(assoc data3 :a-new-key {:a 1 :b 2})

(assoc @data3 :a-new-key {:a 1 :b 2})
@data3







(defn make-hiccup [d]
  [:p d])


(make-hiccup data1)
(make-hiccup data2)
(make-hiccup data3)
(make-hiccup @data3)


(defn make-hiccup2 [d]
  [:p @d])


(make-hiccup2 data1)
(make-hiccup2 data2)
(make-hiccup2 data3)
(make-hiccup2 @data3)


(defn make-hiccup3! [d]
  "takes a ATOM, so it can change the atom before
   making the hiccup"

  (swap! d assoc :more-keys [1 2 3 4 5])
  [:p @d])

@data3
(make-hiccup3! data3)
@data3



;         1-@data3--
;        |          |
;        ^          v
(swap! data3 assoc ,,, :a-new-key {:a 1 :b 2})
;       ^    2 (assoc ,,, :a-new-key {:a 1 :b 2})
;       |         v
;       |         |
;       ----------3



@data3


(reset! data3 {:x "X" :y "Y" :z "Z"})
@data3



















(def q 3)
(class q)

(inc
  q)

[1 2 3 4]

(+ [1 2 3 4] [4 5 6])

(conj '(1 2 3) 4)
(conj [1 2 3] 4)
(sort '(5 4 7))
(sort [5 8 2])
(into {} (sort {:z 1 :b 5}))

(apply distinct? {:1 4 :2 4})


(def a (atom {:a 1 :b 3}))

(defn f [a]
  (prn a))

;
;
;
;
;




[:p (str a)]

(f @a)







(defn gen []
  (into []
        (take 5
              (repeatedly #(+ -10.0
                              (rand 25))))))

(def data [{:name "set 1" :values {:f (gen)
                                   :p (gen)}}])

(defn path [] [:values])
(defn path2 [] [:values :p])

(get-in data [0 :values])
(get-in data [0 :values :p])
(get-in data (apply conj [0] (path)))
(get-in data (apply conj [0] (path2)))



(defn- code [x]
  (cond
    (< x 3) :up
    (and (< 3 x)
         (< x 7)) :warning
    :else :fault))


(let [k (map #(str "item-" %) (range 25))
      v (map (fn [_]
               (->> 10
                 rand
                 code))
             (range 25))]
  (sort
    (into []
          (zipmap k v))))

(-> 3
    rand)


(->> 3
     rand
     code)



(take 200
      (repeatedly #(+ 5.0
                      (rand 5))))


(partition 3 [["item-1" :up] ["item-2" :warning] ["item-3" :fault]
              ["item-4" :up] ["item-5" :warning] ["item-6" :fault]])




(def chart-configs [:line-chart :column-chart])

(defn make-chart [type data options]
  [:div])

(def data {})

(def options {})


(map
  (fn [x]
    [:div {:style {:width "95%" :height "65%"}}
     [make-chart x data options]])
  chart-configs)


(into [:div]
      (map
        (fn [x]
          [:div {:style {:width "95%" :height "65%"}}
           [make-chart x data options]])
        chart-configs))

