(ns hello
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [crux.api :as crux]
            [injest :as i]
            [clojure.pprint :as pp]))

(def ^crux.api.ICruxAPI node
  (crux/start-standalone-node {:kv-backend    "crux.kv.memdb.MemKv"
                               :db-dir        "data/db-dir-1"
                               :event-log-dir "data/eventlog-1"}))
(defn db [] (crux/db node))

(use 'hello)

(i/people node)
(i/artifacts node)
(i/places node)



(crux/q (db) '{:find  [?e]
               :where [[?e :person/str]]})

(pp/print-table
  (for [e (crux/q (db) '{:find  [?e]
                         :where [[?e :person/str]]})]
    (crux/entity (db) (first e))))

(crux/q (db) '{:find  [?name]
               :where [[_ :place/title ?name]]})



; FYI - print-table is 20 lines of code
(pp/print-table [(crux/entity (db) :ids.people/Charles)])



(crux/q (db) '{:find  [?id]
               :where [[?id :person/name ?name]]
               :args  [{?name "Charles"}
                       {?name "Joe"}]})


(crux/q (db) '{:find  [?id]
               :where [[(re-find #"J" ?name)]
                       [?id :person/name ?name]]})


(defn reg-find [db r]
      (crux/q db {:find  ['?id]
                  :where [[(re-find r '?name)]
                          ['?id :person/name '?name]]
                  :args [('regex r)]}))
(reg-find (db) #"J")


(defn age-find [db a]
  (crux/q db {:find  '[?age]
              :where '[[(>= ?age 21)]]
              :args (into []
                          (for [i a]
                            {'?age i}))}))
(age-find (db) [22 23 20])





