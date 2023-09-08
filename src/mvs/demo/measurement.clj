(ns mvs.demo.measurement
  (:require [mvs.topics :refer :all]
            [mvs.read-models :refer :all]
            [clj-uuid :as uuid]))


(def registry (atom {}))
(def thread (atom nil))


(defn generate-integer [max]
  (rand-int max))


(defn- publish-measurement [topic resource-id attribute value-fn]
  (println "publish-measurement" resource-id attribute)
  (publish! topic
    [{:resource/id resource-id}
     {:resource/id           resource-id
      :measurement/id        (uuid/v1)
      :measurement/attribute attribute
      :measurement/value     (value-fn)}]))


(defn- measurements [topic]
  (doall
    (map (fn [[resource-id {:keys [attribute value-fn]}]]
           (publish-measurement topic resource-id attribute value-fn))
      @registry)))


(defn start-reporting
  "start a thread that goes off every 'period' seconds and produces events for all
  resources in the registry, publishing them to 'topic'"

  [topic period]
  (reset! thread (Thread. (fn []
                            (while true
                              (measurements topic)
                              (Thread/sleep (* 1000 period))))))
  (.start @thread))


(defn stop-reporting []
  (.stop @thread))


(defn report-once [topic]
  (measurements topic))


(defn register-resource-update
  "add an entry to the registry for producing a :resource/measurement event
  on a periodic schedule

  - resource-di (UUID) : id of the resource report reporting
  - attribute (keywod) : name of the resource's attribute being reporting
  - value-fn (function) : a function to generate a new value to report"

  [resource-id attribute value-fn]

  (swap! registry assoc resource-id
    {:attribute attribute :value-fn value-fn}))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;

; make a new message to publish
(comment
  (do
    (def resource-id (uuid/v1))
    (def attribute :googoo/metric)
    (def value-fn #(generate-integer 100)))

  (value-fn)

  (publish-measurement resource-measurement-topic
    resource-id attribute value-fn)

  ())


; core of the thread function
(comment
  (do
    (def resource-id (uuid/v1))
    (def attribute :googoo/metric)
    (def value-fn #(generate-integer 100))
    (def local-reg (atom {resource-id {:attribute attribute :value-fn value-fn}
                          (uuid/v1)   {:attribute attribute :value-fn value-fn}})))

  (doall
    (map (fn [[resource-id {:keys [attribute value-fn]}]]
           (publish-measurement resource-measurement-topic
             resource-id attribute value-fn))
      @local-reg))

  ())


; registry and use an update
(comment
  (do
    (def resource-id (uuid/v1))
    (def attribute :googoo/metric)
    (def value-fn #(generate-integer 100)))

  (register-resource-update resource-id attribute value-fn)

  @registry

  (doall
    (map (fn [[resource-id {:keys [attribute value-fn]}]]
           (publish-measurement resource-measurement-topic
             resource-id attribute value-fn))
      @registry))

  ())


; try using the thread
(comment
  (do
    (def resource-id (-> (mvs.read-model.resource-state-view/resource-states
                           @mvs.read-model.state/app-db)
                       keys first))
    (def attribute :googoo/metric)
    (def value-fn #(generate-integer 100)))

  (register-resource-update resource-id attribute value-fn)

  @registry

  (start-reporting resource-measurement-topic 5)

  (stop-reporting)

  ())


; endregion
