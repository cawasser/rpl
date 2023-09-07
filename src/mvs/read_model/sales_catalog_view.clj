(ns mvs.read-model.sales-catalog-view
  (:require [mvs.constants :refer :all]
            [cljfx.api :as fx]
            [mvs.read-model.state :as state]
            [mvs.read-model.event-handler :as e]))


(defn- update-sales-catalog [current event new-value]
  ; planning for the future...
  ;       more flexible changes to an existing catalog
  (println "update-catalog (a)"
    event)
  ;current new-value "//"
  ;(conj current new-value))

  (condp = event
    :catalog/add-service (conj current new-value)
    :catalog/time-revision current
    :catalog/cost-revision current
    new-value))


(defmethod e/event-handler :sales-catalog
  [{event-key                             :event/key
    {:keys [catalog/event] :as content} :event/content :as params}]

  (println ":sales-catalog" event-key "//" event "//" content)

  (swap! state/app-db
    fx/swap-context
    update
    :sales-catalog-view
    update-sales-catalog event (dissoc content :catalog/event)))


(defn sales-catalog [context]
  (fx/sub-val context :sales-catalog-view))


(defn sales-catalog-view [[event-key event-content :as event]]
  (println "sales-catalog-view" event)
  (e/event-handler {:event/type    :sales-catalog
                    :event/key     event-key
                    :event/content event-content}))


(defn reset-sales-catalog-view []
  (swap! state/app-db
    fx/swap-context
    assoc :sales-catalog-view []))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

(comment
  (do
    (def local (atom {:sales-catalog-view []}))
    (def event :catalog/add-service)
    (def content {:catalog/event       :catalog/add-service
                  :service/id          "0"
                  :service/price       100
                  :service/description "description"
                  :service/elements    [{} {} {}]}))


  (update {:sales-catalog-view []}
    :sales-catalog-view
    update-sales-catalog event (dissoc content :catalog/event))


  (swap! local
    ;fx/swap-context
    update
    :sales-catalog-view
    update-sales-catalog event (dissoc content :catalog/event))


  ())


(comment
  (map (fn [{:keys [service/id service/price service/description
                    service/elements]}]
         (sales-catalog-view
           [{:event/key :catalog}
            {:catalog/event       :catalog/add-service
             :service/id          id
             :service/price       price
             :service/description description
             :service/elements    elements}]))
    service-catalog)

  @state/app-db


  ())


(comment

  (sales-catalog @state/app-db)

  ())

; endregion
