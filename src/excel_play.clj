(ns excel-play
  (:require
    [dk.ative.docjure.spreadsheet :refer :all]
    [datascript.core :as d]))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; first, play around with the excel spreadsheet
;
; https://github.com/mjul/docjure
;

(def filename "../9102Demo Data/Demo_Excel1.xlsx")
(def sheet "SCN_NETWORK_CARRIER_VW")
(def column-map {:A :satellite
                 :B :rx-beam
                 :C :rx-channel
                 :D :tx-beam
                 :E :tx-channel
                 :H :mission-name})

(load-workbook filename)


(->> (load-workbook filename)
  (select-sheet sheet))


; load just a few columns and give them names
;
(->> (load-workbook filename)
  (select-sheet sheet)
  (select-columns column-map)
  (drop 1)
  (map (fn [{:keys [satellite] :as m}]
         (assoc m :satellite (int satellite)))))

(def post-fn #(map (fn [{:keys [satellite] :as m}]
                     (assoc m :satellite (int satellite))) %))

(->> (load-workbook filename)
  (select-sheet sheet)
  (select-columns column-map)
  (drop 1)
  (post-fn))




; load all the columns, but don't give them names (yet)
;
(->> (load-workbook filename)
  (select-sheet "SCN_NETWORK_CARRIER_VW")
  (row-seq)
  (remove nil?)
  (map cell-seq)
  (map #(map read-cell %)))

;; endregion

;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; now let's play with datascript so know kind-of how it works
;
; https://github.com/tonsky/datascript
;

(def schema {:aka {:db/cardinality :db.cardinality/many}})
(def conn (d/create-conn schema))

(d/transact! conn [{:db/id -1
                    :name  "Maksim"
                    :age   45
                    :aka   ["Max Otto von Stierlitz" "Jack Ryan"]}
                   {:db/id -2
                    :name  "Harrison Ford"
                    :age   70
                    :aka   ["Hand Solo" "Jack Ryan" "Indiana Jones"]}])

(d/q '[:find ?n ?a
       :where [?e :aka "Max Otto von Stierlitz"]
       [?e :name ?n]
       [?e :age ?a]]
  @conn)

(d/q '[:find ?n ?a
       :where [?e :aka "Jack Ryan"]
       [?e :name ?n]
       [?e :age ?a]]
  @conn)

(d/transact! conn [{:db/id -1
                    :name  "George"
                    :age   45
                    :aka   ["Max Otto von Stierlitz", "George Pierce"]}])





;; endregion

;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
;
; region ; now let's put the excel data into datascript so can use DATALOG
; to run real queries
;


(def schema {})
(def conn (d/create-conn schema))


(->> (load-workbook filename)
  (select-sheet sheet)
  (select-columns column-map)
  (drop 1)
  (map (fn [{:keys [satellite] :as m}]
         (assoc m :satellite (int satellite))))
  (d/transact! conn))


(->> (clojure.set/union
       ; :rx-channel -> :rx-beam
       (->> (d/q '[:find ?from ?to
                   :where [?e :rx-channel ?from]
                   [?e :rx-beam ?to]]
              @conn)
         (map (fn [[from to]]
                [0 from to])))

       ;rx-beam -> :satellite
       (->> (d/q '[:find ?from ?to
                   :where [?e :rx-beam ?from]
                   [?e :satellite ?to]]
              @conn)
         (map (fn [[from to]]
                [1 from (str to)])))

       ;:satellite -> :tx-beam
       (->>(d/q '[:find ?from ?to
                  :where [?e :satellite ?from]
                  [?e :tx-beam ?to]]
             @conn)
         (map (fn [[from to]]
                [2 (str from) to])))

       ;:tx-beam -> :tx channel
       (->> (d/q '[:find ?from ?to
                   :where [?e :tx-beam ?from]
                   [?e :tx-channel ?to]]
              @conn)
         (map (fn [[from to]]
                [3 from to]))))

  (map (fn [[group from to]]
         [group from to 5]))
  (sort-by first)
  (map (fn [[_ from to weight]]
         [from to weight])))

;; endregion
