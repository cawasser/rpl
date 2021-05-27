(ns sample.core
  (:require [frinj.repl :refer :all]))


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
