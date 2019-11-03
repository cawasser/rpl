(ns baking
  (:require [loco.core :refer :all]
            [loco.constraints :refer :all]))


;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; this is from an example found here:
;
;

;
;
;
(defn bake-cakes [recipes supplies max]
  (let [cakes (keys recipes)
        ingredients (->> recipes first val :recipe keys)
        prices (zipmap (keys recipes) (->> recipes vals (map :price)))

        cake-c (for [c cakes] ($in c 0 (dec max)))

        ingredients-c (for [i ingredients]
                        ($<=
                         (apply $+
                                (flatten
                                 (for [c cakes]
                                   ($* (get-in recipes [c :recipe i]) c))))
                         (i supplies)))

        maximize-sales (apply $+ (flatten
                                  (for [c cakes]
                                    ($* (c prices) c))))

        model (concat cake-c ingredients-c)]

    (into (sorted-map)
          (solution model :maximize maximize-sales))))


(def recipes {:banana-cake     {:recipe {:flour 250 :bananas 2 :cocoa 0  :sugar 75  :butter 100} :price 400}
              :chocolate-cake  {:recipe {:flour 200 :bananas 0 :cocoa 75 :sugar 150 :butter 150} :price 450}
              :white-cake      {:recipe {:flour 150 :bananas 0 :cocoa 0  :sugar 100 :butter 100} :price 300}})

(def supplies {:flour 4000 :bananas 6 :sugar 2000 :butter 500 :cocoa 500})

(def max 100)

(doall (bake-cakes recipes supplies max))



(def cakes (keys recipes))

(def ingredients (->> recipes first val :recipe keys))

(def prices (zipmap (keys recipes) (->> recipes vals (map :price))))

(def cake-c (for [c cakes] ($in c 0 (dec max))))

;(def ingredients-c (for [i ingredients]
;                     ($<=
;                       (apply $+
;                         (flatten
;                           (for [c cakes]
;                             ($* (get-in recipes [c :recipe i]) c
;                               (i supplies))))))))

(def maximize-sales (apply $+ (flatten
                      (for [c cakes]
                        ($* (c prices) c)))))

model (concat cake-c ingredients-c)