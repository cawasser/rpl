(ns mvs.read-model.provider-catalog-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))



(defn- update-catalog [event current new-value]
  ; planning for the future...
  ;       more flexible changes to an existing catalog
  (condp = event
    :catalog/add-resource current
    :catalog/remove-resource current
    :catalog/time-revision current
    :catalog/cost-revision current
    :catalog/remove-provider new-value
    new-value))


(defmethod e/event-handler :provider-catalog
  [{event-key                                            :event/key
    {:keys [catalog/event resource/id resource/catalog]} :event/content :as params}]
  (println ":provider-catalog" params)
  (swap! state/app-db
    fx/swap-context
    update-in
    [:provider-catalog-view (:provider/id event-key) :resource/catalog]
    #(update-catalog event % catalog)))


(defn provider-catalogs [context]
  (println "provider-catalogs" context)
  (fx/sub-val context :provider-catalog-view))


(defn provider-catalog-view [[event-key event-content :as event]]
  (println "provider-catalog-view" event)
  (e/event-handler {:event/type    :provider-catalog
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-provider-catalog-view []
  (swap! state/app-db
    fx/swap-context
    assoc :provider-catalog-view {}))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; work out the logic
(comment

  (provider-catalogs @state/app-db)


  ; NEW catalog (replace)
  (def event [{:provider/id "alpha-googoos"}
              {:provider/id      "alpha-googoos"
               :resource/catalog [{:resource/type        10,
                                   :resource/time-frames [10 11 12 13 14 15],
                                   :resource/cost        20}]}])

  (let [[event-key event-content] event]
    (e/event-handler {:event/type    :provider-catalog
                      :event/key     event-key
                      :event/content event-content}))


  (provider-catalog-view event)
  (provider-catalog-view [{:provider/id "alpha-googoos"}
                          {:provider/id      "alpha-googoos"
                           :resource/catalog [{:resource/type        10,
                                               :resource/time-frames [1 2 3 4 5],
                                               :resource/cost        10}]}])


  ; update just the cost of existing :resource/id
  (do
    (def current [{:resource/type        10,
                   :resource/time-frames [1 2 3 4 5],
                   :resource/cost        10}])
    (def new-value [{:resource/type 10,
                     :resource/cost 50}]))




  (provider-catalog-view [{:provider/id "alpha-googoos"}
                          {:provider/id      "alpha-googoos"
                           :catalog/event    :catalog/cost-update
                           :resource/catalog [{:resource/type 10,
                                               :resource/cost 40}]}])


  ())

; endregion
