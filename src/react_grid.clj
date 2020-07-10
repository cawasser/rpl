(ns react-grid)



(def new-layout
  [{:y 5 :maxH nil :moved false :minW nil :w 3 :static false :isDraggable nil :isResizable nil :h 3 :minH nil :x 0 :maxW nil :i "1"}
   {:y 5 :maxH nil :moved false :minW nil :w 3 :static false :isDraggable nil :isResizable nil :h 3 :minH nil :x 0 :maxW nil :i "2"}
   {:y 5 :maxH nil :moved false :minW nil :w 3 :static false :isDraggable nil :isResizable nil :h 3 :minH nil :x 2 :maxW nil :i "3"}])
   ;{:y 6 :maxH nil :moved false :minW nil :w 2 :static false :isDraggable nil :isResizable nil :h 2 :minH nil :x 4 :maxW nil :i "4"}
   ;{:y 3 :maxH nil :moved false :minW nil :w 1 :static false :isDraggable nil :isResizable nil :h 1 :minH nil :x 0 :maxW nil :i "5"}
   ;{:y 9 :maxH nil :moved false :minW nil :w 3 :static false :isDraggable nil :isResizable nil :h 2 :minH nil :x 2 :maxW nil :i "6"}
   ;{:y 4 :maxH nil :moved false :minW nil :w 1 :static false :isDraggable nil :isResizable nil :h 2 :minH nil :x 0 :maxW nil :i "7"}
   ;{:y 8 :maxH nil :moved false :minW nil :w 2 :static false :isDraggable nil :isResizable nil :h 1 :minH nil :x 4 :maxW nil :i "8"}])

(map (fn [n]
       {:y   (:y n)
        :x   (:x n)
        :w   (:w n)
        :h   (:h n)
        :key (:i n)}) new-layout)

(def red-layout (map (fn [n]
                       {:y   (:y n)
                        :x   (:x n)
                        :w   (:w n)
                        :h   (:h n)
                        :key (:i n)}) new-layout))



(def widgets [{:key 1 :data-grid {:x 0 :y 0 :w 3 :h 3} :style {:background-color "green"
                                                               :color            "white"}}
              {:key 2 :data-grid {:x 0 :y 3 :w 3 :h 3} :style {:background-color "gray"
                                                               :color            "white"}}
              {:key 3 :data-grid {:x 2 :y 0 :w 3 :h 3} :style {:background-color "lightsalmon"}}])

(def w-1 {:key 1 :data-grid {:x 0 :y 0 :w 3 :h 3} :style {:background-color "green"
                                                          :color            "white"}})
(def l-1 {:y 5 :maxH nil :moved false :minW nil :w 5 :static false :isDraggable nil
          :isResizable nil :h 5 :minH nil :x 5 :maxW nil :i "1"})

(def r-1 (first (map (fn [n]
                       {:y   (:y n)
                        :x   (:x n)
                        :w   (:w n)
                        :h   (:h n)}) [l-1])))



(-> w-1
  (assoc-in [:data-grid :x] (:x l-1))
  (assoc-in [:data-grid :y] (:y l-1))
  (assoc-in [:data-grid :w] (:w l-1))
  (assoc-in [:data-grid :h] (:h l-1)))



(filter #(= (str (:key %)) "1") widgets)

(mapv #(first (filter (fn [n] (= (str (:key n)) (:i %)))
                widgets))
  new-layout)


(defn- apply-updates [new-layout widget]
  (prn new-layout)
  (-> widget
    (assoc-in [:data-grid :x] (:x new-layout))
    (assoc-in [:data-grid :y] (:y new-layout))
    (assoc-in [:data-grid :w] (:w new-layout))
    (assoc-in [:data-grid :h] (:h new-layout))))

(apply-updates r-1 w-1)


(for [l red-layout]
 (if (= (str (:key w-1)) (:key l))
  (apply-updates l w-1)))



(remove nil? [1 nil 2 nil 3 nil])
(remove nil? '(1 nil 2 nil 3 nil))
(remove nil? '({:a 1} nil {:a 2} nil {:a 3} nil))


(->> (for [w widgets
           l red-layout]
       (if (= (str (:key w)) (:key l))
         (apply-updates l w)))
  (remove nil?)
  (into []))


(defn- reduce-layouts [l]
  (map (fn [n]
         {:y   (:y n)
          :x   (:x n)
          :w   (:w n)
          :h   (:h n)
          :key (:i n)})
    l))

(reduce-layouts new-layout)


(defn update-layout [widgets layout]
   (->> (for [w widgets
              l layout]
          (if (= (str (:key w)) (:key l))
            (apply-updates l w)))
     (remove nil?)
     (into [])))


(update-layout widgets red-layout)
(update-layout widgets (reduce-layouts new-layout))


