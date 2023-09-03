(ns mvs.dashboard.ui.text-area)


(defn text-area [{:keys [text] :as params}]
  (println "text-area" params)

  {:fx/type :v-box
   :alignment :center
   :children [{:fx/type :label
               :text (str (or text "DEFAULT TEXT"))}]})




(comment
  (cljfx.dev/help-ui)




  ())

