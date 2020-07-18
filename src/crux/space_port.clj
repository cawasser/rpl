(ns crux.space-port
  (:require [crux.api :as crux]))


; see https://juxt.pro/blog/crux-tutorial-setup


(def node
  (crux/start-node
    {:crux.node/topology '[crux.standalone/topology]
     :crux.kv/db-dir     "data/db-dir"}))



(def manifest
  {:crux.db/id  :manifest
   :pilot-name  "Johanna"
   :id/rocket   "SB002-sol"
   :id/employee "22910x2"
   :badges      "SETUP"
   :cargo       ["stereo" "gold fish" "slippers" "secret note"]})

(crux/submit-tx node [[:crux.tx/put manifest]])

(crux/entity-history (crux/db node) :manifest :asc)


;;;;;;;;;;;;;;;
;
; PLUTO
;
;;;;;;;;;;;;;;;


; the 4 CRUX commands
;
; Transaction  Description
; ------------------------
; put          Writes a version of a document
; delete       Deletes a version of a document
; match        Stops a transaction if the precondition is not met.
; evict        Removes a document entirely

; add some 'commodities'

(crux/submit-tx node
  [[:crux.tx/put
    {:crux.db/id  :commodity/Pu
     :common-name "Plutonium"
     :type        :element/metal
     :density     19.816
     :radioactive true}]

   [:crux.tx/put
    {:crux.db/id  :commodity/N
     :common-name "Nitrogen"
     :type        :element/gas
     :density     1.2506
     :radioactive false}]

   [:crux.tx/put
    {:crux.db/id  :commodity/CH4
     :common-name "Methane"
     :type        :molecule/gas
     :density     0.717
     :radioactive false}]])


; add 'plutonium shipments' for a whole week

(crux/submit-tx node
  [[:crux.tx/put
    {:crux.db/id :stock/Pu
     :commod     :commodity/Pu
     :weight-ton 21}
    #inst "2115-02-13T18"]                                  ;; valid-time

   [:crux.tx/put
    {:crux.db/id :stock/Pu
     :commod     :commodity/Pu
     :weight-ton 23}
    #inst "2115-02-14T18"]

   [:crux.tx/put
    {:crux.db/id :stock/Pu
     :commod     :commodity/Pu
     :weight-ton 22.2}
    #inst "2115-02-15T18"]

   [:crux.tx/put
    {:crux.db/id :stock/Pu
     :commod     :commodity/Pu
     :weight-ton 24}
    #inst "2115-02-18T18"]

   [:crux.tx/put
    {:crux.db/id :stock/Pu
     :commod     :commodity/Pu
     :weight-ton 24.9}
    #inst "2115-02-19T18"]])


(crux/submit-tx node
  [[:crux.tx/put
    {:crux.db/id :stock/N
     :commod     :commodity/N
     :weight-ton 3}
    #inst "2115-02-13T18"                                   ;; start valid-time
    #inst "2115-02-19T18"]                                  ;; end valid-time

   [:crux.tx/put
    {:crux.db/id :stock/CH4
     :commod     :commodity/CH4
     :weight-ton 92}
    #inst "2115-02-15T18"
    #inst "2115-02-19T18"]])


(crux/entity (crux/db node #inst "2115-02-14") :stock/Pu)
; => {:crux.db/id :stock/Pu, :commod :commodity/Pu, :weight-ton 21}

(crux/entity (crux/db node #inst "2115-02-18") :stock/Pu)
; => {:crux.db/id :stock/Pu, :commod :commodity/Pu, :weight-ton 22.2}

(crux/entity (crux/db node #inst "2115-02-19") :stock/Pu)
; => {:crux.db/id :stock/Pu, :commod :commodity/Pu, :weight-ton 24}

(crux/entity (crux/db node #inst "2115-02-16") :stock/CH4)
; => {:crux.db/id :stock/CH4, :commod :commodity/CH4, :weight-ton 92}



(defn easy-ingest
  "Uses Crux put transaction to add a vector of documents to a specified
  node"
  [node docs]
  (crux/submit-tx node (mapv (fn [doc] [:crux.tx/put doc]) docs)))


(crux/submit-tx
  node
  [[:crux.tx/put
    (assoc manifest :badges ["SETUP" "PUT"])]])

(crux/entity (crux/db node) :manifest)
; =>
;{:crux.db/id :manifest,
; :pilot-name "Johanna",
; :id/rocket "SB002-sol",
; :id/employee "22910x2",
; :badges ["SETUP" "PLUTO"],
; :cargo ["stereo" "gold fish" "slippers" "secret note"])

(crux/entity-history (crux/db node) :manifest :asc)


;;;;;;;;;;;;;;;
;
; MERCURY
;
;;;;;;;;;;;;;;;


(def mercury-data [{:crux.db/id  :commodity/Pu
                    :common-name "Plutonium"
                    :type        :element/metal
                    :density     19.816
                    :radioactive true}

                   {:crux.db/id  :commodity/N
                    :common-name "Nitrogen"
                    :type        :element/gas
                    :density     1.2506
                    :radioactive false}

                   {:crux.db/id  :commodity/CH4
                    :common-name "Methane"
                    :type        :molecule/gas
                    :density     0.717
                    :radioactive false}

                   {:crux.db/id  :commodity/Au
                    :common-name "Gold"
                    :type        :element/metal
                    :density     19.300
                    :radioactive false}

                   {:crux.db/id  :commodity/C
                    :common-name "Carbon"
                    :type        :element/non-metal
                    :density     2.267
                    :radioactive false}

                   {:crux.db/id  :commodity/borax
                    :common-name "Borax"
                    :IUPAC-name  "Sodium tetraborate decahydrate"
                    :other-names ["Borax decahydrate" "sodium borate"
                                  "sodium tetraborate" "disodium tetraborate"]
                    :type        :mineral/solid
                    :appearance  "white solid"
                    :density     1.73
                    :radioactive false}])

(easy-ingest node mercury-data)


; CRUX uses Datalog (EDN-flavored), much like Datomic and Datascript
;
(crux/q (crux/db node)
  '{:find  [element]
    :where [[element :type :element/metal]]})
; => #{[:commodity/Pu] [:commodity/Au]}

(crux/q (crux/db node)
  '{:find  [element]
    :where [[element :type :element/non-metal]]})
; => #{[:commodity/C]}

; quoting is required, but flexible
;
(=
  ; whole :find clause
  (crux/q (crux/db node)
    '{:find  [element]
      :where [[element :type :element/metal]]})

  ; each element
  (crux/q (crux/db node)
    {:find  '[element]
     :where '[[element :type :element/metal]]})

  ; using quote fn
  (crux/q (crux/db node)
    (quote
      {:find  [element]
       :where [[element :type :element/metal]]})))
;;=> true


; you CAN use the ?xxx notation typical of Datalog, but it isn't required
(crux/q (crux/db node)
  '{:find  [?name]
    :where [[?e :type :element/metal]
            [?e :common-name ?name]]})
;=> #{["Gold"] ["Plutonium"]}

(crux/q (crux/db node)
  '{:find  [name rho]
    :where [[e :density rho]
            [e :common-name name]]})
;=> #{["Nitrogen" 1.2506] ["Carbon" 2.267] ["Methane" 0.717]
;     ["Borax" 1.73] ["Gold" 19.3] ["Plutonium" 19.816]}


; we can also pass in arguments which will get bound as expected
;
; this requires quoting the individual vectors because each binding variable
; has to be quoted
;
(defn arg-find [arg]
  (crux/q (crux/db node)
    {:find  '[name]
     :where '[[e :type t]
              [e :common-name name]]
     :args  [{'t arg}]}))

(arg-find :element/metal)
;=> #{["Gold"] ["Plutonium"]}

(arg-find :element/gas)
;=> #{["Nitrogen"]}

(defn filter-type
  [type]
  (crux/q (crux/db node)
    {:find  '[name]
     :where '[[e :type t]
              [e :common-name name]]
     :args  [{'t type}]}))

(defn filter-appearance
  [description]
  (crux/q (crux/db node)
    {:find  '[name IUPAC]
     :where '[[e :common-name name]
              [e :IUPAC-name IUPAC]
              [e :appearance appearance]]
     :args  [{'appearance description}]}))

(filter-type :element/metal)
;;=> #{["Gold"] ["Plutonium"]}

(filter-appearance "white solid")
;;=> #{["Borax" "Sodium tetraborate decahydrate"]}


(crux/submit-tx
  node [[:crux.tx/put
         (assoc manifest
           :badges ["SETUP" "PUT" "DATALOG-QUERIES"])]])


;;;;;;;;;;;;;;;
;
; NEPTUNE
;
;;;;;;;;;;;;;;;


