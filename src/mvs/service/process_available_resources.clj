(ns mvs.service.process-available-resources
  (:require [clojure.spec.alpha :as spec]
            [mvs.constants :refer :all]
            [mvs.read-models :refer :all]
            [mvs.topics :refer :all]
            [mvs.helpers :refer :all]
            [mvs.specs]
            [clj-uuid :as uuid]))


(defn process-available-resources
  "take provider catalog data (:provider/catalog) and turn it into something useful for allocating
  to customers as: `{ <:resource/type> #{ { <:resource/time> <:provider/id> }
                                        { <:resource/time> <:provider/id> } }
                      <:resource/type> #{ { <:resource/time> <:provider/id> }
                                        { <:resource/time> <:provider/id> } } }`
  "
  [_ _ _ [_ catalog]]

  (println "process-available-resources" (:provider/id catalog))

  (if (spec/valid? :provider/catalog catalog)
    (let [provider-id (:provider/id catalog)
          new-values  (->> catalog
                        :resource/catalog
                        (map (fn [{:keys [resource/type resource/time-frames]}]
                               {type (into #{}
                                       (map (fn [t]
                                              {t provider-id})
                                         time-frames))}))
                        (into {}))]

      (swap! available-resources-view #(merge-with into %1 %2) new-values))

    (malformed "process-available-resources" :provider/catalog)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; region ; rich comments
;


; build available-resources-view from provider-catalogs
(comment
  (do
    (def provider-a [{:provider/id "alpha"} provider-alpha])
    (def provider-b [{:provider/id "bravo"} provider-bravo])
    (def m-key {:provider/id "alpha"})

    (def local-available-resources (atom {})))


  (->> (second provider-a)
    :resource/catalog
    (map (fn [{:keys [resource/id resource/time-frames]}]
           {id (into #{}
                 (map (fn [t]
                        {t (:provider/id m-key)})
                   time-frames))}))
    (into {}))

  (let [[m-key m-content] provider-a
        new-values (->> m-content
                     (map (fn [{:keys [resource/id resource/time-frames]}]
                            {id (into #{}
                                  (map (fn [t]
                                         {t (:provider/id m-key)})
                                    time-frames))}))
                     (into {}))]
    (swap! local-available-resources #(merge-with into %1 %2) new-values))

  (merge {} {0 #{{0 :a} {1 :a} {2 :a}}
             1 #{{0 :a} {1 :a} {2 :a}}})
  (merge-with into
    {0 #{{0 :a} {1 :a} {2 :a}}
     1 #{{0 :a} {1 :a} {2 :a}}}
    {0 #{{0 :b} {1 :b} {2 :b}}
     1 #{{0 :b} {1 :b} {2 :b}}})



  (apply merge [0 1 2 3] [0 4 5 6])
  (apply conj [0 1 2 3] [0 4 5 6])


  (defn process [catalog]
    (let [[m-key m-content] catalog
          new-values (->> m-content
                       (map (fn [{:keys [resource/id resource/time-frames]}]
                              {id time-frames}))
                       (into {}))]
      (swap! local-available-resources #(merge-with into %1 %2) new-values)))

  (process provider-a)
  (process provider-b)


  ())


; test process-available-resources
(comment
  (do
    (reset! available-resources-view {})
    (def provider-id (:provider/id provider-alpha)))

  (process-available-resources [] [] [] [{:provider/id provider-id}
                                         provider-alpha])

  ())

; endregion

