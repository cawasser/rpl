(ns mvs.service.process-provider-catalog
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :as const]
            [mvs.read-models :refer :all]
            [mvs.read-model.sales-catalog-view :as sales-v]
            [mvs.read-model.provider-catalog-view :as provider-v]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))



(def last-event (atom nil))


(defn process-provider-catalog
  "takes a (spec) `:provider/catalog`

  i.e., tuple of hash-maps (key and message-content) with the producer encoded inside the 'key'
  and the catalog itself being the message-content"

  [[{:keys [provider/id]} catalog :as event]]

  (reset! last-event event)

  (println "process-provider-catalog" id)

  (if (spec/valid? :provider/catalog catalog)
    (do
      ; 1) update provider-catalog-view
      (provider-v/provider-catalog-view event)

      ;; 2) update service-catalog-view (for later use)
      ;(reset! service-catalog-view const/service-catalog)

      ; 3) 'publish' ACME's "Sales Catalog"
      (doseq [service const/service-catalog]
        (sales-v/sales-catalog-view
          [{:event/key :sales-catalog}
           (assoc service :catalog/event :catalog/add-service)])))

    (malformed "process-provider-catalog" :provider/catalog catalog)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; check provider-* against :provider/catalog
(comment
  (spec/explain :provider/catalog provider-alpha)


  ())


; test process-provider-catalog
(comment
  (def event-1 [{:provider/id "alpha"} provider-alpha])
  (def event-2 [{:provider/id "bravo"} provider-bravo])

  @provider-catalog-view
  (reset! provider-catalog-view {})

  (process-provider-catalog event-1)
  (process-provider-catalog event-2)
  (process-provider-catalog [{:provider/id "charlie"} provider-charlie])
  (process-provider-catalog [{:provider/id "delta"} provider-delta])
  (process-provider-catalog [{:provider/id "echo"} provider-echo])

  ())



; debug sales-catalog-view
(comment
  (process-provider-catalog @last-event)

  (def catalog (->> @last-event second))



  (spec/valid? :provider/catalog catalog)


  ())

; endregion


