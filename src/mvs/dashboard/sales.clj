(ns mvs.dashboard.sales
  (:require [cljfx.api :as fx]))


(defn sales-dashboard [_ _ _ event]
  (println "SALES received " event))


(defn sales []
  (fx/on-fx-thread
    (fx/create-component
      {:fx/type :stage
       :showing true
       :title "Sales Dashboard"
       :width 500
       :height 300
       :scene {:fx/type :scene
               :root {:fx/type :v-box
                      :alignment :center
                      :children [{:fx/type :label
                                  :text "The SALES dashboard goes here!"}]}}})))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (sales)


  ())

; endregion
