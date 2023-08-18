(ns mvs.dashboard.planning
  (:require [cljfx.api :as fx]
            [cljfx.dev :refer :all]))


(def *state
  (atom {:content "the content goes here"}))


(defn- sales-request-table [{:keys [content]}]
  {:fx/type :table-view
   :items content})



(defn root [{:keys [content]}]
  {:fx/type :stage
   :showing true
   :width 500
   :height 300

   :title "Planning Dashboard"
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [{:fx/type :label
                              :text "Current Content:"}
                             {:fx/type sales-request-table
                              :content content}]}}})


(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)))



(defn planning-dashboard
  "hooks the UI's state atom into the topology's 'read-model'"

  [_ _ _ [event-key content :as event]]

  (println "PLANNING received " event)

  (swap! *state assoc :content (str content)))



(defn planning-ui [{:keys content}]
  (fx/on-fx-thread
    (fx/create-component
      {:fx/type :stage
       :showing true
       :title "Planning Dashboard"
       :width 500
       :height 300
       :scene {:fx/type :scene
               :root {:fx/type :v-box
                      :children [{:fx/type :label
                                  :text "Current Content:"}
                                 {:fx/type sales-request-table
                                  :content content}]}}})))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (planning-ui)
  ()


  (cljfx.dev/help)

  (cljfx.dev/help :table-view)
  (cljfx.dev/help :v-box)


  (cljfx.dev/help-ui)


  ;; Convenient way to add watch to an atom + immediately render app

  (fx/mount-renderer *state renderer)

  (swap! *state assoc :content "repl value")

  (planning-dashboard [] [] [] [{} [{:plan/id "uuid-1"}
                                    {:plan/id "uuid-2"}
                                    {:plan/id "uuid-3"}
                                    {:plan/id "uuid-4"}]])


  ())

; endregion
