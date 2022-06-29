(ns parse.first-example
  (:require [instaparse.core :as insta]))

; http://gigasquidsoftware.com/blog/2013/05/01/growing-a-language-with-clojure-and-instaparse/

(def parser
  (insta/parser "number = #'-?[0-9]+'"))


(parser "1")
(parser "1008")
(parser "0876")
(parser "1 5")
(parser "-1")
(parser "----1")




(def parser
  (insta/parser "name = #'[a-zA-Z ]+'"))
(parser "one")
(parser "One Two")
(parser "as many words as I want")


(def parser
  (insta/parser
    "expr = number | vector
     vector = snumber+ number
     <snumber> = (number space)*
     <space> = <#'[ ,;]+'>
     number = #'[0-9]+'"))

(parser "1")
(parser "1 2")
(parser "1, 2")
(parser "1      1")
(parser "1 1 ")

(def transform-options
  {:number read-string
   :vector (comp vec list)
   :expr   identity})

(defn parse [input]
  (->> (parser input) (insta/transform transform-options)))

(parse "1 2 3 4")
(parse "1, 2, 3, 4")
(parse "1      4")

(identity [1 2 3 4])




(def parser
  (insta/parser
    "expr = number | vector | operation
     operation = operator space+ vector
     operator = '+' | '-' | '*' | '/'
     vector = snumber+ number
     <snumber> = (number space)*
     <space> = <#'[ ]+'>
     number = #'[0-9]+'"))

(parser "+ 1 2 3 4")



(defn choose-operator [op]
  (case op
    "+" +
    "-" -
    "*" *
    "/" /))

(def transform-options
  {:number    read-string
   :vector    (comp vec list)
   :operator  choose-operator
   :operation apply
   :expr      identity})

(defn parse [input]
  (->> (parser input) (insta/transform transform-options)))

(parse "+ 1 2 3 4")
(+ 1 2 3 4)




; more like Clojure
(def parser
  (insta/parser
    "sexpr = ( <'('> expr <')'> )
     expr = number | vector | operation
     operation = operator space+ vector
     operator = '+' | '-' | '*' | '/'
     vector = (snumber+ number) | sexpr
     <snumber> = (number space)*
     <space> = <#'[ ]+'>
     number = #'[0-9]+'"))


(def transform-options
  {:number    read-string
   :vector    (comp vec list)
   :operator  choose-operator
   :operation apply
   :expr      identity
   :sexpr     identity})


(parser "(+ 1 2 3 4)")

(parse "(+ 1 2 3 4)")
(parse "+ 1 2 3 4")
(parse "(+ 1 2 3 4")

(parse "(+ 1 (+ 1 1) 3 4)")



; https://blog.taylorwood.io/2018/05/17/instaparse.html


(def bowling-score-parser
  (insta/parser
    "S = F F F F F F F F F 10TH
     F = OPEN | CLOSED
     OPEN = ROLL ROLL
     CLOSED = STRIKE | SPARE
     10TH = OPEN |
            SPARE (STRIKE | ROLL) |
            STRIKE (SPARE | ROLL ROLL) |
            STRIKE STRIKE (STRIKE | ROLL)
     STRIKE = 'X'
     SPARE = ROLL '/'
     ROLL = PINS | MISS
     PINS = #'[1-9]'
     MISS = '-'"))

(bowling-score-parser "6271X9-8/XX3572-/X")
(bowling-score-parser "XXXXX6/XX7/XX5")
(bowling-score-parser "XXXXXXXXXXXX")
(bowling-score-parser "XXXXXXXXXX-/")
(bowling-score-parser "XXXXXXXXX--")
(bowling-score-parser "9-9-9-9-9-9-9-9-9-9-")
(bowling-score-parser "X7/9-X-88/-6XXX81")
(bowling-score-parser "X7/9-X-88/-6XX-/X")
(bowling-score-parser "X-/X5-8/9-X811-4/X")

;; region ; part 0
; some notional binary command set
; all commands are 1 or 2 digits
;
;
; part 1
; digit-0      meaning
;   1           turn the radio on
;   0           see part 2
;
; part 2
; digit-1     meaning
;   0           turn volume down 1 step
;   1           turn volume up 1 step
;   2           tune up 1Mhz
;   3           tune down 1MHz
;   4           turn radio off
;
; examples
;   "1"  - valid
;   "0"  - NOT valid
;   "00" - valid
;   "10" - NOT valid
;; endregion

(def parser
  (insta/parser
    "expr = turn-on | complex
    turn-on = <'1'>
    <complex> = (<'0'> command)
    <command> = vol-up | vol-down | tune-up | tune-down | turn-off
    vol-down = <'0'>
    vol-up = <'1'>
    tune-up = <'2'>
    tune-down = <'3'>
    turn-off = <'4'>"))

(parser "1")
(parser "00")
(parser "01")
(parser "02")
(parser "03")
(parser "04")
(parser "05")


; let's go further

; 1 - turn-on
; 00xyyyy - adjust-volume, x = 0 -> down x = 1 -> up, y = amt
; 01xyyyy - adjust tuning, x = 0 -> down x = 1 -> up, y = amt
; 02 - turn-off


(def parser
  (insta/parser
    "expr = turn-on | complex
    turn-on = <'1'>
    <complex> = (<'0'> command)
    <command> = volume | tuner | turn-off
    volume = (<'0'> (up | down) amount)
    tuner = (<'1'> (up | down) amount)
    down = <'0'>
    up = <'1'>
    turn-off = '2'
    amount = #'[0-9]+'"))


(parser "1")
(parser "02")
(parser "0005")
(parser "0013")
(parser "01065")

;; region ; commands for the XML validation
;
; <rule value='x' meaning='y' />
;

; example rules:
;
; <rule-set>
;   <rule value='1' meaning='turn-on' />
;   <rule value='04' meaning='turn-off' />
;   <rule value='01' meaning='volume-up' />
;   ...
; <rule-set/>
;
;
;; endregion





;; region

(def parser
  (insta/parser "number = #'[0-9]+'"))

(def transform-options
  {:number read-string})


(defn parse [input]
  (->> (parser input) (insta/transform transform-options)))

(parser "1")




(defn convert-value [value]
  (condp = value
    "1" (str "<rule value='" value "' meaning='turn-on'/>")
    "04" (str "<rule value='" value "' meaning='turn-on'/>")))


(def ->xml
  {:number convert-value})


(defn parse->xml [input]
  (->> (parser input) (insta/transform ->xml)))

(parse->xml "1")
(parse->xml "0")
(parse->xml "04")

;; endregion




; something like what we start with
;
(def header
  "Header
  Int  4 Message Length
  Byte 1 Source
  Int 4 Message ID
  Short 2 Transaction ID")

; we want to end with something like this
;
(def xml-rules
  "<item name='Header'>
     <field type='Int' size='4' name='Message Length'/>
     <field type='Byte' size='1' name='Source'/>
     <field type='Int' size='4' name='Message ID'/>
     <field type='Short' size='2' name='Transaction ID'/>
  </item>")


(def parser
  (insta/parser
    "header = ('Header' <new-line> field*)
    field = <whitespace> type <whitespace> size <whitespace> name (<whitespace> | <new-line> | Epsilon)
    type = 'Int' | 'Byte' | 'Short'
    size = #'[0-9]+'
    name = #'[a-zA-Z ]+'
    new-line = '\n'
    whitespace = #'\\s+'"))


(parser "Header\nInt 4 Dummy Name")
(parser header)


(defn convert-header [value]
  (str "<header name='" value "'>"))

(defn convert-field [type size name]
  (println "convert-field" type size name)
  (str "<field type='" type "' size='" size "' name='" name "'/>"))

(defn convert-type [value]
  (str "type='" value "' "))

(defn convert-size [value]
  (str "size='" value "' "))

(defn convert-name [value]
  (str "name='" value "' "))

(def ->xml
  {:header convert-header
   :field  convert-field})



(defn parse->xml [input]
  (->> (parser input) (insta/transform ->xml)))

(parse->xml "Header\nInt 4 Dummy Name")
(parse->xml header)
