(ns mvs.read-model.resource-performance-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))



(declare resource-performance)


; :resource/performance (process-resource-performance)
(defmethod e/event-handler :resource/performance
  [{event-key                               :event/key
    {:keys [resource/id
            history/size
            performance/function
            measurement/attribute
            measurement/value] :as content} :event/content :as params}]

  (println ":resource/performance" event-key "//" content)

  ; performance are a "time ordered" collection of values, so we can see
  ; history (materialized view)

  (let [[fn-name func] function
        history (-> (resource-performance (state/db))
                  (get-in [id :resource/performance
                           attribute :performance/history])
                  (as-> m
                    (conj m value)
                    (take-last (or size 10) m)
                    (vec m)))]

    (swap! state/app-db
      fx/swap-context
      #(-> %
         (assoc-in
           [:resource-performance-view id :resource/performance
            attribute :performance/history]
           history)
         (assoc-in [:resource-performance-view id :resource/performance
                    attribute :performance/function]
           fn-name)
         (assoc-in
           [:resource-performance-view id :resource/performance
            attribute :performance/metric]
           (func history))))))


(defn resource-performance [context]
  (fx/sub-val context :resource-performance-view))


(defn resource-performance-view [[event-key event-content :as event]]
  (println "resource-performance-view" event)
  (e/event-handler {:event/type    :resource/performance
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-resource-performance-view []
  (swap! state/app-db
    fx/swap-context
    assoc :resource-performance-view {}))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; work out logic for the updated (app-db-based) resource-performance-view
(comment
  (do
    (def id :resource-1)
    (def attribute :attribute-1)
    (def value 100)
    (def ktable (atom {:resource-performance-view {id {:resource/performance
                                                       {:attribute-1 []}}}})))


  (update-in @ktable
    [:resource-performance-view id :resource/performance attribute]
    conj value)


  (get-in @ktable [:resource-performance-view
                   id
                   :resource/performance])
  ;attribute])

  (update-in @ktable
    [:resource-performance-view id :resource/performance attribute]
    #(as-> % m
       (conj m value)
       (take-last 3 m)
       (vec m)))


  (swap! ktable
    update-in [:resource-performance-view id :resource/performance attribute]
    #(as-> % m
       (conj m 1)
       (take-last 3 m)
       (vec m)))
  (swap! ktable
    update-in [:resource-performance-view id :resource/performance attribute]
    #(as-> % m
       (conj m 2)
       (take-last 3 m)
       (vec m)))
  (swap! ktable
    update-in [:resource-performance-view id :resource/performance attribute]
    #(as-> % m
       (conj m 3)
       (take-last 3 m)
       (vec m)))
  (swap! ktable
    update-in [:resource-performance-view id :resource/performance attribute]
    #(as-> % m
       (conj m 4)
       (take-last 3 m)
       (vec m)))


  ())


; try out the app-db-based resource-performance-view logic
(comment
  (do
    (def id (clj-uuid/v1))
    (def measurement-id (clj-uuid/v1))
    (def attribute "dummy")
    (def size 3))

  (resource-performance-view [{:resource/id id}
                              {:resource/id           id
                               :history/size          size
                               :measurement/id        measurement-id
                               :measurement/attribute attribute
                               :measurement/value     (rand-int 100)}])


  (update-in {}
    [:resource-performance-view id :resource/performance attribute]
    conj 100)


  ;(swap! state/app-db
  ;fx/swap-context
  (update-in (resource-performance (state/db))
    [id :resource/performance attribute]
    #(as-> % m
       (conj m value)
       (take-last size m)
       (vec m)))

  (swap! state/app-db
    fx/swap-context
    update-in
    [:resource-performance-view id :resource/performance attribute]
    #(as-> % m
       (conj m value)
       (take-last size m)
       (vec m)))

  (resource-performance (state/db))
  (reset-resource-performance-view)

  (conj nil 100)

  ())


; do both the history update and compute the new performance metric
(comment
  (do
    (def local (atom {}))
    (def id #uuid"908c1581-512a-11ee-ad2e-f29ec83e6171")
    (def attribute :googoo/metric)
    (def value 10)
    (def size 3))


  (update-in {}
    [:resource-performance-view id :resource/performance attribute :performance/history]
    #(as-> % m
       (conj m value)
       (take-last (or size 10) m)
       (vec m)))

  (-> {}
    (get-in [:resource-performance-view id :resource/performance
             attribute :performance/history])
    (as-> m
      (conj m value)
      (take-last (or size 10) m)
      (vec m)))

  (let [history (-> {}
                  (get-in [:resource-performance-view id :resource/performance
                           attribute :performance/history])
                  (as-> m
                    (conj m value)
                    (take-last (or size 10) m)
                    (vec m)))]

    (-> {}
      (assoc-in
        [:resource-performance-view id :resource/performance
         attribute :performance/history]
        history)
      (assoc-in
        [:resource-performance-view id :resource/performance
         attribute :performance/metric]
        (/ (apply + history) (count history)))))




  (let [history (-> @local
                  (get-in [:resource-performance-view id :resource/performance
                           attribute :performance/history])
                  (as-> m
                    (conj m 20)
                    (take-last (or size 10) m)
                    (vec m)))]

    (swap! local
      #(-> %
         (assoc-in
           [:resource-performance-view id :resource/performance
            attribute :performance/history]
           history)
         (assoc-in
           [:resource-performance-view id :resource/performance
            attribute :performance/metric]
           (/ (apply + history) (count history))))))



  (let [history (-> (resource-performance (state/db))
                  (get-in [id :resource/performance
                           attribute :performance/history])
                  (as-> m
                    (conj m 40)
                    (take-last (or size 10) m)
                    (vec m)))]

    (swap! state/app-db
      fx/swap-context
      #(-> %
         (assoc-in
           [:resource-performance-view id :resource/performance
            attribute :performance/history]
           history)
         (assoc-in
           [:resource-performance-view id :resource/performance
            attribute :performance/metric]
           (/ (apply + history) (count history))))))


  ())

; endregion
