(ns mvs.read-model.available-resources-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))


; :availability/add (process-available-resources)
(defmethod e/event-handler :availability/add
  [{event-key                                    :event/key
    {:keys [availability/event
            availability/resources] :as content} :event/content :as params}]

  (println ":availability/add" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update
    :available-resources-view
    #(merge-with into % resources)))


; :availability/remove (process-sales-request)
(defmethod e/event-handler :availability/remove
  [{event-key                                    :event/key
    {:keys [availability/event
            availability/resources] :as content} :event/content :as params}]

  (println ":availability/remove" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update
    :available-resources-view
    #(reduce (fn [m [id alloc]]
               (assoc m id (apply disj (get m id) alloc)))
       %
       resources)))


(defn available-resources [context]
  (fx/sub-val context :available-resources-view))


(defn available-resources-view [[event-key event-content :as event]]
  (println ":available-resources-view" event)
  (e/event-handler {:event/type    (:availability/event event-content)
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-available-resources-view []
  (swap! state/app-db
    fx/swap-context
    assoc :available-resources-view {}))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; play with the logic (local data)
(comment
  (do
    (reset-available-resources-view)
    (def local-availability-view (atom {:available-resources-view {}}))
    (def resources {0 #{{5 "alpha"} {0 "alpha"} {1 "alpha"} {4 "alpha"} {3 "alpha"} {2 "alpha"}},
                    1 #{{5 "alpha"} {0 "alpha"} {1 "alpha"} {4 "alpha"} {3 "alpha"} {2 "alpha"}},
                    2 #{{5 "alpha"} {0 "alpha"} {1 "alpha"} {4 "alpha"} {3 "alpha"} {2 "alpha"}},
                    3 #{{5 "alpha"} {0 "alpha"} {1 "alpha"} {4 "alpha"} {3 "alpha"} {2 "alpha"}},
                    4 #{{5 "alpha"} {0 "alpha"} {1 "alpha"}
                        {4 "alpha"} {3 "alpha"} {2 "alpha"}}}))


  ; :availability/add
  (swap! local-availability-view
    ;fx/swap-context
    update
    :available-resources-view
    #(merge-with into % resources))

  (available-resources-view [{} {:availability/event     :availability/add
                                 :availability/resources resources}])
  (available-resources (state/db))


  ; :availability/remove
  (swap! local-availability-view
    ;fx/swap-context
    update
    :available-resources-view
    #(reduce (fn [m [id alloc]]
               (assoc m id (apply disj (get m id) alloc)))
       %
       {0 #{{5 "alpha"}}}))

  (available-resources-view [{}
                             {:availability/event :availability/remove
                              :availability/resources {0 #{{5 "alpha"}}}}])
  (available-resources (state/db))


  ())


; endregion
