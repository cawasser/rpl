(ns provenance.frinj-units
  (:require [frinj.repl :refer :all]))

; see https://github.com/martintrojer/frinj/wiki
;
;

(frinj-init!)



; https://github.com/martintrojer/frinj/wiki/User-Guide#fj

(fj 1)
;=> #frinj.core.fjv{:v 1, :u {}}

(fj 1 :inch)
;=> #frinj.core.fjv{:v 127/5000, :u {"m" 1}}
(/ 5280 12)
(/ 127 5000)

(fj 10 :miles :per :hour)
;=> #frinj.core.fjv{:v 2794/625, :u {"m" 1, "s" -1}}

; NOTE: skipping the :per (which implies division) produces 'm*h', not 'm/h'
(fj 10 :miles :hour)
;=> #frinj.core.fjv{:v 57936384N, :u {"m" 1, "s" 1}}

; looks like "m" is for miles, 1 means numerator, "s" for seconds, -1 for denominator?


;;;;;;;;;;;;;;;;;
; Q: can we attach the 'frinj type' as meta-data?
(comment
  #_(def speed ^{:fj [:miles :per :hour]} 10)
  ,)
; Syntax error reading source: Metadata can only be applied to IMetas

; so, NO. not to scalars anyway
; but it does work with a map
(def speed ^{:fj [:miles :per :hour]} {:value 10})
(meta speed)
(:value speed)

; per https://clojure.org/reference/metadata
(def m ^:hi [1 2 3])
(meta (with-meta m {:bye true}))
m



; we can attach meta-data to 'symbols'
; but I think this only works at the 'top level'
(def speed 10)
(def speed-meta (with-meta 'speed {:fj [:miles :per :hour]}))
(eval speed-meta)
(apply fj (into [(eval speed-meta)] (:fj (meta speed-meta))))







; examples from https://github.com/martintrojer/frinj/blob/master/examples/examples.clj

; fill a room with water
(-> (fj 10 :feet 12 :feet 8 :feet :to :gallons) str)

; ~7181 gallons!


; how many pound of water is that?
(-> (fj 10 :feet 12 :feet 8 :feet :water :to :pounds) str)

; ~60,000 lbs!


; what if the floor could only hold 2 tons, how much deep could you fill that room?
;    NB: 'fj_' <-> 'fj-/' (division)
(-> (fj_ (fj 2 :tons)
      (fj 10 :feet 12 :feet :water))
  (to :feet) str)

; only 1/2 foot deep :(


