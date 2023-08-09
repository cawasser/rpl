(ns mvs.core
  (:require [clojure.spec.alpha :as spec]
            [malli.provider :as mp]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Constants
;

(def num-googoos 10)
(def num-time-frames (range 5))

(def service-catalog
  [{:service/id 0 :service/price 25
    :service/description "Googoo 0 for 5"
    :service/resources [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id 1 :service/price 25
    :service/description "Googoo 1 for 5"
    :service/resources [{:resource/id 1 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id 2 :service/price 25
    :service/description "Googoo 2 for 5"
    :service/resources [{:resource/id 2 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id 3 :service/price 25
    :service/description "Googoo 3 for 5"
    :service/resources [{:resource/id 3 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id 4 :service/price 25
    :service/description "Googoo 4 for 5"
    :service/resources [{:resource/id 4 :resource/time-frames [0 1 2 3 4 5]}]}
   {:service/id 5 :service/price 7
    :service/description "Googoo 0 for 0"
    :service/resources [{:resource/id 0 :resource/time-frames [0]}]}
   {:service/id 6 :service/price 7
    :service/description "Googoo 0 for 1"
    :service/resources [{:resource/id 0 :resource/time-frames [1]}]}
   {:service/id 7 :service/price 7
    :service/description "Googoo 0 for 2"
    :service/resources [{:resource/id 0 :resource/time-frames [2]}]}
   {:service/id 8 :service/price 7
    :service/description "Googoo 0 for 3"
    :service/resources [{:resource/id 0 :resource/time-frames [3]}]}
   {:service/id 9 :service/price 7
    :service/description "Googoo 0 for 4"
    :service/resources [{:resource/id 0 :resource/time-frames [4]}]}])

; endregion



; region ; Specs

(spec/def :resource/id integer?)

(spec/def :googoo/googoo (spec/keys :req [:resource/id]))
(spec/def :googoo/googoos (spec/coll-of :googoo/googoo))

(spec/def :resource/time integer?)
(spec/def :resource/time-frames (spec/coll-of :resource/time))

(spec/def :resource/cost integer?)

(spec/def :resource/definition
  (spec/keys :req [:resource/id :resource/time-frames :resource/cost]))
(spec/def :resource/catalog (spec/coll-of :resource/definition))

(spec/def :provider/id string?)
(spec/def :provider/event-key (spec/keys :req [:provider/id]))
(spec/def :provider/catalog (spec/tuple :provider/event-key :resource/catalog))


; endregion

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Helpers
;

(defn- make-resource [id [start-time end-time]]
  {:resource/id id :resource/time-frame (into [] (range start-time end-time))})


; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Topics

(def provider-catalog-topic (atom []))
(def customer-request-topic (atom []))
(def service-request-topic (atom []))
(def plan-topic (atom []))
(def fix-topic (atom []))
(def configure-provider-topic (atom []))
(def configure-monitoring-topic (atom []))
(def measurement-topic (atom []))
(def health-topic (atom []))
(def performance-topic (atom []))
(def usage-topic (atom []))


(defn publish! [topic message]
  (reset! topic message))


; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Read Models/View
;

(def googoos (->> (range num-googoos)
               (map (fn [id] {:resource/id id}))
               (into [])))

(def provider-alpha [{:resource/id 0 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                     {:resource/id 1 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                     {:resource/id 2 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                     {:resource/id 3 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}
                     {:resource/id 4 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10}])
(def provider-bravo [{:resource/id 0 :resource/time-frames [1 3 5] :resource/cost 5}
                     {:resource/id 1 :resource/time-frames [1 3 5] :resource/cost 5}
                     {:resource/id 2 :resource/time-frames [1 3 5] :resource/cost 5}
                     {:resource/id 3 :resource/time-frames [1 3 5] :resource/cost 5}
                     {:resource/id 4 :resource/time-frames [1 3 5] :resource/cost 5}])
(def provider-charlie [{:resource/id 0 :resource/time-frames [2 4 5] :resource/cost 5}
                       {:resource/id 1 :resource/time-frames [2 4 5] :resource/cost 5}
                       {:resource/id 2 :resource/time-frames [2 4 5] :resource/cost 5}
                       {:resource/id 3 :resource/time-frames [2 4 5] :resource/cost 5}
                       {:resource/id 4 :resource/time-frames [2 4 5] :resource/cost 5}])
(def provider-delta [{:resource/id 0 :resource/time-frames [0 1 2] :resource/cost 5}
                     {:resource/id 1 :resource/time-frames [0 1 2] :resource/cost 5}
                     {:resource/id 2 :resource/time-frames [0 1 2] :resource/cost 5}
                     {:resource/id 3 :resource/time-frames [0 1 2] :resource/cost 5}
                     {:resource/id 4 :resource/time-frames [0 1 2] :resource/cost 5}])
(def provider-echo [{:resource/id 0 :resource/time-frames [3 4] :resource/cost 2}
                    {:resource/id 1 :resource/time-frames [3 4] :resource/cost 2}
                    {:resource/id 2 :resource/time-frames [3 4] :resource/cost 2}
                    {:resource/id 3 :resource/time-frames [3 4] :resource/cost 2}
                    {:resource/id 4 :resource/time-frames [3 4] :resource/cost 2}])


(def provider-catalog-view
  "summary of all the provider catalogs" (atom {}))


(def available-resources-view
  "denormalized arrangements of all resources available" (atom {}))


(def service-catalog-view
  "catalog of service ACME offers ot customer" (atom []))


; endregion ; Read Models/Views


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Commands
;



; endregion ; Commands


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Events
;



; endregion ; Events


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Services
;

(defn process-producer-catalog
  "takes a (spec) `:provider/catalog`

  i.e., tuple of hash-maps (key and message-content) with the producer encoded inside the 'key'
  and the catalog itself being the message-content"

  [k _ _ [{:keys [:provider/id]} catalog :as params]]

  (println "process-producer-catalog" k)

  ; 1) update provider-catalog-view
  (swap! provider-catalog-view assoc id catalog)

  ; 2) 'publish' ACME's "Service Catalog"
  (reset! service-catalog-view service-catalog))


(defn available-resources
  [k _ _ [{:keys [:provider/id]} catalog :as params]]

  (println "available-resources" k)

  ; update with new data, arranged by:
  ;     a) :resource/id
  ;     b) :resource/time-frame
  ;     c) :resource/cost
  ;
  (let [by-id   (->> catalog)
        by-time (->> catalog)
        by-cost (->> catalog)]

    (reset! available-resources-view
      (-> @available-resources-view
        (assoc :by-id by-id)
        (assoc :by-time by-time)
        (assoc :by-cost by-cost)))))





; endregion ; Services


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Dashboards
;


(defn customer-service-catalog [k _ _ catalog]
  (println "CUSTOMER CATALOG UPDATE" k))


; endregion : Dashboards



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Wiring


; wire the services together using watchers on the various atoms
(add-watch provider-catalog-topic
  :process-producer-catalog #'process-producer-catalog)
(add-watch provider-catalog-topic
  :available-resources #'available-resources)
(add-watch service-catalog-view
  :customer-service-catalog customer-service-catalog)



; endregion



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; Scripting

(comment

  ; region ; reset everything
  (do
    (reset! provider-catalog-topic [])
    (reset! provider-catalog-view {})
    (reset! available-resources-view {}))

  ; endregion

  ; region ; providers 'publish' catalogs

  (publish! provider-catalog-topic [{:provider/id "alpha"} provider-alpha])
  (publish! provider-catalog-topic [{:provider/id "bravo"} provider-bravo])
  (publish! provider-catalog-topic [{:provider/id "charlie"} provider-charlie])
  (publish! provider-catalog-topic [{:provider/id "delta"} provider-delta])
  (publish! provider-catalog-topic [{:provider/id "echo"} provider-echo])

  ;endregion

  ; customers request services




  ())

; endregion


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; rich comments

; test googoo specs
(comment
  (spec/valid? :resource/id 5)
  (spec/valid? :googoo/googoo {:resource/id 3})
  (spec/valid? :googoo/googoo (nth googoos 5))
  (spec/valid? :googoo/googoos googoos)

  (spec/explain :resource/id 5)
  (spec/explain :googoo/googoo {:resource/id 3})
  (spec/explain :googoo/googoo (nth googoos 5))
  (spec/explain :googoo/googoos googoos)

  ())

; test :resource/definition specs
(comment
  (spec/explain :resource/definition {:resource/id          0
                                      :resource/time-frames [1 3 5]
                                      :resource/cost        5})
  (spec/explain :resource/definition {:resource/id 0 :resource/time-frames [0 1 2 3 4 5] :resource/cost 10})
  (spec/explain :resource/catalog provider-alpha)
  (spec/explain :resource/catalog provider-bravo)
  (spec/explain :resource/catalog provider-charlie)
  (spec/explain :resource/catalog provider-delta)
  (spec/explain :resource/catalog provider-echo)


  ())

; test process-producer-catalog
(comment
  (def event-1 [{:provider/id "alpha"} provider-alpha])
  (def event-2 [{:provider/id "bravo"} provider-bravo])

  @provider-catalog-view
  (reset! provider-catalog-view {})

  (process-producer-catalog _ _ _ event-1)
  (process-producer-catalog _ _ _ event-2)
  (process-producer-catalog _ _ _ [{:provider/id "charlie"} provider-charlie])
  (process-producer-catalog _ _ _ [{:provider/id "delta"} provider-delta])
  (process-producer-catalog _ _ _ [{:provider/id "echo"} provider-echo])



  ())

; look at 'watchers'
(comment
  (def x (atom 0))


  (add-watch x :watcher
    (fn [key atom old-state new-state]
      (println "new-state" new-state)))

  (reset! x 2)



  ())

; test :provider/catalog specs
(comment
  (spec/explain :resource/catalog provider-alpha)
  (spec/explain :provider/id "alpha")
  (spec/explain :resource/definition (first provider-alpha))
  (spec/explain :provider/catalog [{:provider/id "alpha"} provider-alpha])

  ())



; endregion
