(ns crux.hello
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [crux.api :as crux]
            [crux.injest :as i]
            [clojure.pprint :as pp]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Let's play with Crux!
;
; see https://juxt.pro/crux
;
; see also https://opencrux.com/docs#get-started





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



; FYI - print-table is 20 lines of code [f-1]
(pp/print-table [(crux/entity (db) :ids.people/Charles)])

(pp/print-table [:person/name :person/location
                 :person/born :person/str
                 :person/int :person/dex
                 :person/hp]
  [(crux/entity (db) :ids.people/Charles)])

; suggest something like:
(comment

  ; set up the heading
  (def person-headings [{:col-heading {:person/name "Name"}
                         :cell-validator #(String? %)
                         :cell-style {:text-color :white
                                      :text-weight :bold
                                      :bkgrn-color :green}
                         ...}

                        {:tbl-heading {:person/born "DoB"}}
                        ...])

  ; then construct the table
  (r/table-component person-headings (data-from-source ...))

  ())




(crux/q (db) '{:find  [?id]
               :where [[?id :person/name ?name]]
               :args  [{?name "Charles"}
                       {?name "Joe"}]})


(crux/q (db) '{:find  [?id]
               :where [[(re-find #"J" ?name)]
                       [?id :person/name ?name]]})


(defn find-attr [db attr]
  (crux/q db {:find  ['?id '?attr]
              :where [['?id attr '?attr]]}))

(find-attr (db) :person/name)



(defn str-find [db s]
  (crux/q db {:find  '[?e]
              :where '[[?e :person/str ?s]
                       [(>= ?s ?str)]]
              :args (into []
                      (for [i s]
                        {'?str i}))}))
(str-find (db) [40])

(defn dex-find [db s]
  (crux/q db {:find  '[?e]
              :where '[[?e :person/dex ?s]
                       [(>= ?s ?str)]]
              :args (into []
                      (for [i s]
                        {'?str i}))}))
(dex-find (db) [55])









; still working on this one...
(defn reg-find [db attr reg]
  (crux/q db {:find  ['?id]
              :where [['?id attr '?attr]
                      [(re-find reg '?attr)]]}))
(reg-find (db) :person/name #"J")


(re-find #"J" "Nothing to see")
(re-find #"J" "Nothing to Jump")
(re-find #"J" "Jumping to Jump")

(defn r-find [reg val]
  (re-find reg val))

(r-find #"J" "Nothing to see")
(r-find #"J" "Nothing to Jump")
(r-find #"J" "Jumping to Jump")


