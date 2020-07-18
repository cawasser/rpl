(ns crux.more-crux
  (:require [crux.api :as crux]
            [clojure.java.io :as io]))


; see https://opencrux.com/docs#get-started



(defn start-standalone-node ^crux.api.ICruxAPI [storage-dir]
  (crux/start-node {:crux.node/topology '[crux.standalone/topology]
                    :crux.kv/db-dir     (str (io/file storage-dir "db"))}))





(comment                                                    ; which can be used as
  (def node (start-standalone-node "crux-store"))


  (crux/submit-tx
    node
    [[:crux.tx/put
      {:crux.db/id :dbpedia.resource/Pablo-Picasso ; id
       :name "Pablo"
       :last-name "Picasso"}
      #inst "2018-05-18T09:20:27.966-00:00"]]) ; valid time


  (crux/q (crux/db node)
    '{:find [e]
      :where [[e :name "Pablo"]]})


  (crux/entity (crux/db node) :dbpedia.resource/Pablo-Picasso)


  ())

