(ns mvs.service.process-provider-catalog
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))



(defn process-provider-catalog
  "takes a (spec) `:provider/catalog`

  i.e., tuple of hash-maps (key and message-content) with the producer encoded inside the 'key'
  and the catalog itself being the message-content"

  [k _ _ [{:keys [:provider/id]} catalog]]


  (println "process-provider-catalog" k)

  (if (spec/valid? :provider/catalog catalog)
    (do
      ; 1) update provider-catalog-view
      (swap! provider-catalog-view assoc id catalog)

      ; 2) update service-catalog-view (for later use)
      (reset! service-catalog-view service-catalog)

      ; 3) 'publish' ACME's "Service Catalog"
      (publish! sales-catalog-topic service-catalog))

    (malformed "process-provider-catalog" :provider/catalog)))




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

  (process-provider-catalog [] [] [] event-1)
  (process-provider-catalog [] [] [] event-2)
  (process-provider-catalog [] [] [] [{:provider/id "charlie"} provider-charlie])
  (process-provider-catalog [] [] [] [{:provider/id "delta"} provider-delta])
  (process-provider-catalog [] [] [] [{:provider/id "echo"} provider-echo])

  ())

; endregion


