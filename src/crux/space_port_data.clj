(ns crux.space-port-data)



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
