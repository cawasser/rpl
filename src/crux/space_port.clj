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
;=> #{["Gold"] ["Plutonium"]}

(filter-appearance "white solid")
;=> #{["Borax" "Sodium tetraborate decahydrate"]}


(crux/submit-tx
  node [[:crux.tx/put
         (assoc manifest
           :badges ["SETUP" "PUT" "DATALOG-QUERIES"])]])


;;;;;;;;;;;;;;;
;
; NEPTUNE
;
;;;;;;;;;;;;;;;

; using 'valid start time' (first time argument)
;
;     transaction time is automatically set
;
(crux/submit-tx
  node
  [[:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Full}
    #inst "2114-12-03"]])


; now we need both 'valid-start' and 'valid-end' times
;
; TODO: does Crux figure out the end time automatically if you just 'put' an update with a different valid time?
;
; WARNING: the blog includes 'notes' on these 'puts' which are NOT VALID CLOJURE!
(crux/submit-tx
  node
  [[:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Full}
    #inst "2113-12-03" ;; Valid time start
    #inst "2114-12-03"] ;; Valid time end

   [:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Full}
    #inst "2112-12-03"
    #inst "2113-12-03"]

   ; TODO: is this really needed? can we determine the 'no coverage' period just from the LACK of a valid response during the given time-frame?
   ;
   [:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? false}
    #inst "2112-06-03"
    #inst "2112-12-02"]

   [:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Promotional}
    #inst "2111-06-03"
    #inst "2112-06-03"]])
;

; query a given time
;
(crux/q (crux/db node #inst "2115-07-03")
  '{:find [cover type]
    :where [[e :consumer-id :RJ29sUU]
            [e :cover? cover]
            [e :cover-type type]]})
;=> #{[true :Full]}

(crux/q (crux/db node #inst "2111-07-03")
  '{:find [cover type]
    :where [[e :consumer-id :RJ29sUU]
            [e :cover? cover]
            [e :cover-type type]]})
;=> #{[true :Promotional]}

(crux/q (crux/db node #inst "2112-07-03")
  '{:find [cover type]
    :where [[e :consumer-id :RJ29sUU]
            [e :cover? cover]
            [e :cover-type type]]})
;=> #{}

(crux/submit-tx
  node [[:crux.tx/put
         (assoc manifest
            :badges ["SETUP" "PUT" "DATALOG-QUERIES" "BITEMP"])]])


;;;;;;;;;;;;;;;
;
; SATURN
;
;;;;;;;;;;;;;;;

(def saturn-data [{:crux.db/id :gold-harmony
                   :company-name "Gold Harmony"
                   :seller? true
                   :buyer? false
                   :units/Au 10211
                   :credits 51}

                  {:crux.db/id :tombaugh-resources
                   :company-name "Tombaugh Resources Ltd."
                   :seller? true
                   :buyer? false
                   :units/Pu 50
                   :units/N 3
                   :units/CH4 92
                   :credits 51}

                  {:crux.db/id :encompass-trade
                   :company-name "Encompass Trade"
                   :seller? true
                   :buyer? true
                   :units/Au 10
                   :units/Pu 5
                   :units/CH4 211
                   :credits 1002}

                  {:crux.db/id :blue-energy
                   :seller? false
                   :buyer? true
                   :company-name "Blue Energy"
                   :credits 1000}])

(easy-ingest node saturn-data)


; helper functions
;
(defn stock-check
  ""
  [company-id item]
  {:result (crux/q (crux/db node)
             {:find '[name funds stock]
              :where ['[e :company-name name]
                      '[e :credits funds]
                      ['e item 'stock]]
              :args [{'e company-id}]})
   :item item})

(defn format-stock-check
  "pretty-printer for the results from a stock-check"
  [{:keys [result item] :as stock-check}]
  (for [[name funds commod] result]
    (str "Name: " name ", Funds: " funds ", " item " " commod)))


; there are some commodities the Saturn companies want to buy or sell
;
; these tx's use core.tx/match to be sure the transaction can/should take place
;
; assume this is a sale from Tombaugh to Blue Energy, so they CAN be combined
;
(crux/submit-tx
  node
  [[:crux.tx/match
    :blue-energy
    {:crux.db/id :blue-energy
     :seller? false
     :buyer? true
     :company-name "Blue Energy"
     :credits 1000}]
   [:crux.tx/put
    {:crux.db/id :blue-energy
     :seller? false
     :buyer? true
     :company-name "Blue Energy"
     :credits 900
     :units/CH4 10}]
   [:crux.tx/match
    :tombaugh-resources
    {:crux.db/id :tombaugh-resources
     :company-name "Tombaugh Resources Ltd."
     :seller? true
     :buyer? false
     :units/Pu 50
     :units/N 3
     :units/CH4 92
     :credits 51}]
   [:crux.tx/put
    {:crux.db/id :tombaugh-resources
     :company-name "Tombaugh Resources Ltd."
     :seller? true
     :buyer? false
     :units/Pu 50
     :units/N 3
     :units/CH4 82
     :credits 151}]])

(format-stock-check (stock-check :tombaugh-resources :units/CH4))
;=> ("Name: Tombaugh Resources Ltd., Funds: 151, :units/CH4 82")

(format-stock-check (stock-check :blue-energy :units/CH4))
;=> ("Name: Blue Energy, Funds: 900, :units/CH4 10")

; my match experiment - can you do this WRONG?
;
; ie., can you forget some of the matches and just cheat?
;
(crux/submit-tx
  node
  [[:crux.tx/match
    :blue-energy
    {:crux.db/id :blue-energy
     :seller? false
     :buyer? true
     :company-name "Blue Energy"
     :credits 900
     :units/CH4 10}]
   [:crux.tx/put
    {:crux.db/id :blue-energy
     :seller? false
     :buyer? true
     :company-name "Blue Energy"
     :credits 800
     :units/CH4 20}]

   [:crux.tx/put
    {:crux.db/id :tombaugh-resources
     :company-name "Tombaugh Resources Ltd."
     :seller? false
     :buyer? true
     :units/Pu 50
     :units/N 3
     :units/CH4 82
     :credits 151}]])

; we've created 10 :units/CH4 'out of thin air'  !!!
;
(format-stock-check (stock-check :blue-energy :units/CH4))
;=> ("Name: Blue Energy, Funds: 800, :units/CH4 20")
;
(format-stock-check (stock-check :tombaugh-resources :units/CH4))
;=> ("Name: Tombaugh Resources Ltd., Funds: 151, :units/CH4 82")

; unfortunately, YES, you can cheat, so you need to understand you ACTUAL transaction
; needs and use the CRUX low-level tools accordingly


; a case where one of the "paired" matches fails
;
(crux/submit-tx
  node
  [[:crux.tx/match
    :gold-harmony
    {:crux.db/id :gold-harmony
     :company-name "Gold Harmony"
     :seller? true
     :buyer? false
     :units/Au 10211
     :credits 51}]
   [:crux.tx/put
    {:crux.db/id :gold-harmony
     :company-name "Gold Harmony"
     :seller? true
     :buyer? false
     :units/Au 211
     :credits 51}]
   [:crux.tx/match
    :encompass-trade
    {:crux.db/id :encompass-trade
     :company-name "Encompass Trade"
     :seller? true
     :buyer? true
     :units/Au 10
     :units/Pu 5
     :units/CH4 211
     :credits 100002}]
   [:crux.tx/put
    {:crux.db/id :encompass-trade
     :company-name "Encompass Trade"
     :seller? true
     :buyer? true
     :units/Au 10010
     :units/Pu 5
     :units/CH4 211
     :credits 1002}]])

; nothing changes
;
(format-stock-check (stock-check :gold-harmony :units/Au));
;=> ("Name: Gold Harmony, Funds: 51, :units/Au 10211")

(format-stock-check (stock-check :encompass-trade :units/Au))
;=> ("Name: Encompass Trade, Funds: 1002, :units/Au 10")


(crux/submit-tx
  node [[:crux.tx/put
         (assoc manifest
            :badges ["SETUP" "PUT" "DATALOG-QUERIES" "BITEMP" "MATCH"])]])



;;;;;;;;;;;;;;;
;
; JUPITER
;
;;;;;;;;;;;;;;;


