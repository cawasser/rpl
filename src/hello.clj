(ns hello
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [crux.api :as crux]))

(def ^crux.api.ICruxAPI node
  (crux/start-standalone-node {:kv-backend "crux.kv.memdb.MemKv"
                               :db-dir "data/db-dir-1"
                               :event-log-dir "data/eventlog-1"}))
(use 'hello)

(crux/submit-tx
  node
  ; tx type
  [[:crux.tx/put

    {:crux.db/id :ids.people/Charles  ; mandatory id for a document in Crux
     :person/name "Charles"
     ; age 40 at 1740
     :person/born #inst "1700-05-18"
     :person/location :ids.places/rarities-shop
     :person/str  40
     :person/int  40
     :person/dex  40
     :person/hp   40
     :person/gold 10000}

    #inst "1700-05-18"]])


(crux/submit-tx
  node
  [[:crux.tx/put
    {:crux.db/id :dbpedia.resource/Pablo-Picasso ; id
     :name "Pablo"
     :last-name "Picasso"}
    #inst "2018-05-18T09:20:27.966-00:00"]])


(def db (crux/db node))

(crux/q db
        '{:find [e]
          :where [[e :name "Pablo"]]})


(crux/q db '{:find [e]
             :where [[e :person/str]]})



(defn -main []
  (println "Hello world, the time is" (time-str (t/now))))
