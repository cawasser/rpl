(ns mvs.dashboard.ui.window)


(defn window [{:keys [x y width height title content] :as params}]
  (println "window" content)

  {:fx/type :stage
   :showing true
   :title   title
   :x       x
   :y       y
   :width   width
   :height  height
   :scene   {:fx/type :scene
             :root    {:fx/type   :v-box
                       :alignment :center
                       :children  [content]}}})





(comment




  ())