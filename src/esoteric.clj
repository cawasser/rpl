(ns esoteric)


;
;  PART 1
;


; playing with some of the more 'esoteric' functions
; in the Clojure core library
;
; also, listen to the 'Functional Design in Clojure' podcast
;
;     https://clojuredesign.club
;
;   especially episodes: 76, 77, and 78
;

(def games [{:name    "basketball"
             :results {:winner "steve"}
             :players [{:player "bob" :score 14}
                       {:player "steve" :score 15}]}

            {:name    "air-hockey"
             :results {:winner "sally"}
             :players [{:player "sally" :score 4}
                       {:player "steve" :score 0}]}])



(map :name games)
(map :results games)





; but what if we want:  ([<game> <winner>][<game> <winner>]) ?






;------
; we just build a function that does that thing..

(map (fn [game]
       [(:name game) (get-in game [:results :winner])])
  games)













;------
; but that anonymous function is kind of ugly
;
; this isn't that hard a problem, why so much code?

(map #([(:name %) (get-in % [:results :winner])]) games)













;;;;;;;;;;;;;;;;;;
;
; enter JUXT
;
; for juxtapose:
;    "an act or instance of placing two elements close
;     together or side by side" - Wikipedia
;
;;;;;;;;;;;;;;;;;;


; 'juxt' is a HOF (higher-order function)
;
; HOF:  function that takes FUNCTIONS as parameters and/or
; returns a FUNCTION as it's result
;
;    NB: by this definition, 'map' is a HOF!
;
; specifically, 'juxt' returns a single function that, in effect, runs each
; parameter function on the single parameter passed to the 'juxt'
;





(juxt :name #(get-in % [:results :winner]))


; we can use it by passing a value

((juxt :name #(get-in % [:results :winner])) (first games))





; or better yet, map it over a whole collection

(map (juxt :name #(get-in % [:results :winner])) games)



; juxt runs ':name' over games first
;
; '#(get-in % ...)' is run over games second
;
; and then juxt combines the results, "pairwise", into a collection of
; collections




; it is equivalent to:
;
(partition 2                       ; 4) and finally group things "pairwise"
  (interleave                    ; 3) we combine the results
    (map :name games)       ; 1) we run :name over games first
    (map #(get-in %           ; 2) then we run #(get-in ...) over games second
            [:results :winner]) games)))


; pretty hard to comprehend, huh?
;
; AND the pairs are not a vector (maybe that's important) either!
;
; 'juxt' reads a whole lot better, and (with some practice) it is
; LOTS more understandable






;------
; we can make the 'get-in' a little nicer using '->'
; instead of (get-in...)
;
(map (juxt :name #(-> % :results :winner)) games)











;------
; but we STILL have that ugly (slightly smaller, granted)
; anonymous function!
;
;        #(-> % :results :winner)
;
; can we do anything about that?







;;;;;;;;;;;;;;;;;;
;
; enter COMP
;
; for compose
;
;;;;;;;;;;;;;;;;;;




; 'comp' returns a functions that runs a 'pipeline' of the
; parameter functions over the same input, the
; result of one being the input to the next, like the -> or ->>
; macros


(map (comp :winner :results) games)





;------
; important: the order of the parameters to comp is BACKWARDS
; compared to how you would write the get-in or -> !
;
; in other words: comp applies the functions in right-to-left order,
; so more like if you'd written the functions 'nested':

(:winner (:results (first games)))



; which ever way you use to remember the order:
;
;     "opposite of ->", or
;     "opposite of 'get-in'", or
;     "same as nesting"
;
; just remember it!







;------
; now we can combine juxt and comp:



(map (juxt :name (comp :winner :results)) games)



; so much simpler and easier to read and comprehend





; is it really the answer we wanted?

(=
  ; original, ugly version
  (map (fn [game]
         [(:name game) (get-in game [:results :winner])])
    games)

  ; new, improved version
  (map (juxt :name (comp :winner :results)) games))

; YUP!



;------
; let's ask something a little harder
;
; "who played in each game?"
;
;    ie, (["basketball" ["bob" "steve"]]
;         ["air-hockey" ["sally" "steve"]])






(map (juxt :name (comp #(into [] %) #(map :player %) :players)) games)






; wow!
;
; let's break this down

(map                   ; a) map over the collection
  (juxt                  ; c) we want a vector of 2 things:
    :name                  ; the game's :name, and
    (comp                  ; the game's :player ...
      #(into [] %)           ; 3) put the :player into a vector...
      #(map :player %)       ; 2) we want all the :player out of :players
      :players))             ; 1) get the :players out of each game
  games)               ; b) and the collection is 'games'



; tricky. here is an alternative:

(map (fn [game]
       [(:name game)
        (->> game
          :players
          (map :player)
          (into []))])
  games)


; which do you find easier to understand?












;;;;;;;;;;;;;;;;;;
;
; anything else?
;
;;;;;;;;;;;;;;;;;;


; lets say you are working on the UI and making 'get' calls
; to the server for some data. how this typically works is
; we pass in a handler function that will be called and passed
; the result we get back from the server
;

(defn call-server
  "this function ignore the parameter and just returns an
  integer 0 <= x <= 4

  this is our FAKE server implementation"

  [url]
  (rand-int 4))


(defn my-get
  "get 'calls the server' and passes the result to a 'handler' function
  provided by the caller"

  [url handler]
  (handler (call-server url)))



(my-get "/api" #(prn %))







;------
; but what if you need more context for your handler function?
;
; well... you could just hard-code your 'context':
;
(defn hc-handler [result]
  (nth [:zero :one :two :three :four] result))



; since it takes only 1 parameter, this works:

(my-get "/api" hc-handler)







;------
; okay. can we NOT hard-code the context?
;
; but that means we need another parameter!
;
; something like:

(def kw-vec [:zero :one :two :three :four])

(defn nhc-handler [context result]
  (nth context result))


; hmmm... now what?


;
; this handler DOESN'T take ONE parameter! it takes TWO!
;
; we can only pass a handler to 'my-get' that takes ONE

(my-get "/api" nhc-handler)






;;;;;;;;;;;;;;;;;;
;
; enter PARTIAL
;
;;;;;;;;;;;;;;;;;;

; partial is a HOF that returns a function with some parameters
; ALREADY BOUND!
;

(partial nhc-handler kw-vec)


; partial returns a kind of anonymous 'nhc-handler' where the
; first param is already bound to the vector


(my-get "/api" (partial nhc-handler kw-vec))









;------
; or maybe you want to keep track of the results, like this:

(def results (atom []))

(defn atom-handler
  "add the new result onto the context collection (an atom)"

  [context result]
  (swap! context conj result))

(my-get "/api" (partial atom-handler results))




; can we do both?
;
; SURE!

(def r-atom (atom []))
(def r-vec [:alpha :bravo :charlie :delta :echo])

(defn combined-handler
  " do two things with the result:
       - conj to 'history', and
       - convert to keyword"

  [the-atom the-vector result]

  (swap! the-atom conj result)
  (nth the-vector result))

(my-get "/api" (partial combined-handler r-atom r-vec))

@r-atom




; but partial can bind any number of parameters!

(def ch-atom (atom []))

(defn chained-handler
  "convert the value to a keyword and conj THAT onto 'history'"

  [the-atom the-vector result]

  (->> result
    (nth the-vector)
    (swap! the-atom conj)))


(my-get "/api" (partial chained-handler ch-atom r-vec))



; be sure to checkout the 'Functional Design in Clojure' podcast
;
;     https://clojuredesign.club
;
;   especially episodes: 76, 77, and 78
;
