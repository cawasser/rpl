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
             :root    (merge {:fx/type content}
                        (dissoc params :fx/type))}})


(comment
  ; from fork:
  (defn window [{:keys [x y width height content] :as params}]
    (println "window" params)
    {:fx/type       :stage
     :always-on-top true
     :showing       true
     :x             x
     :y             y
     :width         width
     :height        height
     :scene         {:fx/type :scene
                     :root    (merge {:fx/type content}
                                (dissoc params :fx/type))}})



  ())