(ns loco.coloring
  (:require [loco.core :refer :all]
            [loco.constraints :refer :all]))



;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;
; this does a simple coloring of the 5 states of Australia
;
; the point is to make sure each state does not have the same
; color as any of its neighbors. We do this by specifying the
; states, ranged to the number of colors to pick from. The border
; constraint is just a long series of $!= constraints pairing each
; state with those it shares a border with; so WA (Western Aus)
; can't have the same color as NT (Northern Territory) or SA
; (South Aus)
;
; look at the map here:
;         http://ontheworldmap.com/australia/map-of-australia.jpg
;
(defn color-australia [num-colors]
  (let [model [($in :wa 0 num-colors)
               ($in :sa 0 num-colors)
               ($in :nt 0 num-colors)
               ($in :v 0 num-colors)
               ($in :t 0 num-colors)
               ($in :nsw 0 num-colors)
               ($in :q 0 num-colors)

               ($!= :wa :nt)
               ($!= :wa :sa)
               ($!= :nt :sa)
               ($!= :nt :q)
               ($!= :sa :q)
               ($!= :sa :nsw)
               ($!= :sa :v)
               ($!= :q :nsw)
               ($!= :nsw :v)]]

    (into (sorted-map)
          (solution model))))



(color-australia 3)
; => {:nsw 2, :nt 2, :q 0, :sa 1, :t 0, :v 0, :wa 0}

(color-australia 2)
; => {:nsw 2, :nt 2, :q 0, :sa 1, :t 0, :v 0, :wa 0}



; this doesn't prove much, and it isn't flexible



; make a function that can color any "map" as expressed by 2 data
; structures:
;
;   states -> a vector of keys representing the states on the map
;
;   borders -> a map of state keys to a vector of the states it borders
;
(defn color-states [states borders num-colors]
  (let [state-c  (for [i states]
                   ($in i 0 (dec num-colors)))
        border-c (for [i borders
                       s (val i)]
                   ($!= (key i) s))
        model    (concat state-c border-c)]

    (into (sorted-map)
          (solution model))))


(def aus-states [:wa :nt :sa :q :nsw :v :t])

(def aus-borders {:wa  [:nt :sa]
                  :nt  [:sa :q]
                  :sa  [:q :nsw :v]
                  :q   [:nsw]
                  :nsw [:v]})

(color-states aus-states aus-borders 3)
  ; => {:nsw 1, :nt 1, :q 0, :sa 2, :t 0, :v 0, :wa 0}
  ;
  ; notice we got a different answer than the last time...


; this means we can color ANY map!

(def us-states [:AL :AK :AZ :AR :CA :CO :CT :DE :FL :GA :HI :ID :IL :IN :IA :KS :KY :LA :ME :MD :MA
                :MI :MN :MS :MO :MT :NE :NV :NH :NJ :NM :NY :NC :ND :OH :OK :OR :PA :RI :SC :SD :TN
                :TX :UT :VT :VA :WA :WV :WI :WY])

(def us-borders {:AL [:MS :TN :GA :FL] :AK [] :AZ [:CA :NV :UT :NM] :AR [:TX :OK :MO :TN :MS :LA]
                 :CA [:OR :NV :AZ]
                 :CO [:UT :WY :NE :KS :OK :NM] :CT [:NY :MA :RI] :DE [:MD :PA :NJ] :FL [:AL :GA]
                 :GA [:AL :TN :NC :SC :FL]
                 :HI [] :ID [:OR :WA :MT :WY :UT :NV] :IL [:MO :IA :WI :IN] :IN [:IL :MI :OH :KY]
                 :IA [:NE :SD :MN :WI :IL]
                 :KS [:CO :NE :MO :OK] :KY [:MO :IL :IN :OH :WV :VA :TN] :LA [:TX :AR :MS] :ME [:NH]
                 :MD [:WV :PA :DE :VA]
                 :MA [:NY :VT :NH :RI :CT] :MI [:WI :OH :IN] :MN [:SD :ND :WI :IA] :MS [:LA :AR :TN :AL]
                 :MO [:OK :KS :NE :IA :IL]
                 :MT [:ID :ND :SD :WY] :NE [:CO :WY :SD :IA :MO :KS] :NV [:CA :OR :ID :UT :AZ]
                 :NH [:VT :ME :MA] :NJ [:PA :NY :DE]
                 :NM [:AZ :CO :TX] :NY [:VT :MA :NJ :PA] :NC [:TN :VA :SC :GA] :ND [:MT :MN :SD]
                 :OH [:IN :MI :PA :WV :KY]
                 :OK [:NM :CO :KS :MO :AR :TX] :OR [:WA :ID :NV :CA] :PA [:OH :NY :NJ :DE :MD :WV]
                 :RI [:CT :MA] :SC [:GA :NC]
                 :SD [:WY :MT :ND :MN :IA :NE] :TN [:AR :MO :IL :IN :OH :WV :VA :GA :AL :MS]
                 :TX [:NM :OK :AR :LA] :UT [:NV :ID :WY :CO :AZ] :VT [:NY :NH :MA]
                 :VA [:KY :WV :MD :NC :TN] :WA [:ID :OR] :WV [:KY :OH :PA :MD :VA] :WI [:MN :MI :IL :IA]
                 :WY [:ID :MT :ND :SD :NE :CO :UT]})

(color-states us-states us-borders 3)
   ; => {}     < --- three colors isn't enough!

(color-states us-states us-borders 5) ; ?

;
; what's the smallest number of colors to map the US?
;





; passing in 2 different structures is clunky, lets just use the
; borders map and figure out the states from it!
;
(defn color-map [states-and-borders num-colors]
  (let [state-c  (for [i (keys states-and-borders)]
                   ($in i 0 (dec num-colors)))
        border-c (for [i states-and-borders
                       s (val i)]
                   ($!= (key i) s))
        model    (concat state-c border-c)]

    (into (sorted-map)
          (solution model))))


(color-map us-borders 10)



; another point that is easy to miss - the order of data in the input
; map DOES NOT MATTER!
;
; The practical upshot of this is that you can build a whole program to
; create you data structure in the best way for your problem, and loco
; will find solutions anyway!

