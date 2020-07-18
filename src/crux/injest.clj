(ns crux.injest
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [crux.api :as crux]))


(defn people [node]
  (crux/submit-tx
    node
    [; characters
     [:crux.tx/put
      {:crux.db/id      :ids.people/Charles  ; mandatory id for a document in Crux
       :person/name     "Charles"
       ; age 40 at 1740
       :person/born     #inst "1700-05-18"
       :person/location :ids.places/rarities-shop
       :person/str      40
       :person/int      40
       :person/dex      40
       :person/hp       40
       :person/gold     10000}

      #inst "1700-05-18"]
     [:crux.tx/put
      {:crux.db/id :ids.people/Mary
       :person/name "Mary"
       ; age  30
       :person/born #inst "1710-05-18"
       :person/location :ids.places/carribean
       :person/str  40
       :person/int  50
       :person/dex  50
       :person/hp   50}
      #inst "1710-05-18"]
     [:crux.tx/put
      {:crux.db/id :ids.people/Joe
       :person/name "Joe"
       ; age  25
       :person/born #inst "1715-05-18"
       :person/location :ids.places/city
       :person/str  39
       :person/int  40
       :person/dex  60
       :person/hp   60
       :person/gold 70}
      #inst "1715-05-18"]]))

(defn artifacts [node]
  (crux/submit-tx
    node
    [; artefacts
     ; In our tale there is a Cozy Mug...
     [:crux.tx/put
      {:crux.db/id :ids.artefacts/cozy-mug
       :artefact/title "A Rather Cozy Mug"
       :artefact.perks/int 3}
      #inst "1625-05-18"]

     ; ...some regular magic beans...
     [:crux.tx/put
      {:crux.db/id :ids.artefacts/forbidden-beans
       :artefact/title "Magic beans"
       :artefact.perks/int 30
       :artefact.perks/hp -20}

      #inst "1500-05-18"]
     ; ...a used pirate sword...
     [:crux.tx/put
      {:crux.db/id :ids.artefacts/pirate-sword
       :artefact/title "A used sword"}
      #inst "1710-05-18"]
     ; ...a flintlock pistol...
     [:crux.tx/put
      {:crux.db/id :ids.artefacts/flintlock-pistol
       :artefact/title "Flintlock pistol"}
      #inst "1710-05-18"]
     ; ...a mysterious key...
     [:crux.tx/put
      {:crux.db/id :ids.artefacts/unknown-key
       :artefact/title "Key from an unknown door"}
      #inst "1700-05-18"]
     ; ...and a personal computing device from the wrong century.
     [:crux.tx/put
      {:crux.db/id :ids.artefacts/laptop
       :artefact/title "A Tell DPS Laptop (what?)"}
      #inst "2016-05-18"]]))


; places
(defn places [node]
  (crux/submit-tx
    node
    [[:crux.tx/put
      {:crux.db/id :ids.places/continent
       :place/title "Ah The Continent"}
      #inst "1000-01-01"]
     [:crux.tx/put
      {:crux.db/id :ids.places/carribean
       :place/title "Ah The Good Ol Carribean Sea"
       :place/location :ids.places/carribean}
      #inst "1000-01-01"]
     [:crux.tx/put
      {:crux.db/id :ids.places/coconut-island
       :place/title "Coconut Island"
       :place/location :ids.places/carribean}
      #inst "1000-01-01"]]))
