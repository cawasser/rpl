(ns gentle-intro-to-clojure)


; The Clojure Programming Language (https://clojure.org)
;
; Clojure is a dynamic, general-purpose programming language, combining the
; approachability and interactive development of a scripting language with
; an efficient and robust infrastructure for multithreaded programming.
; Clojure is a compiled language, yet remains completely dynamic – every
; feature supported by Clojure is supported at runtime. Clojure provides
; easy access to the Java frameworks, with optional type hints and type
; inference, to ensure that calls to Java can avoid reflection.
;
; Clojure is a dialect of Lisp, and shares with Lisp the code-as-data
; philosophy and a powerful macro system. Clojure is predominantly a
; functional programming language, and features a rich set of immutable,
; persistent data structures. When mutable state is needed, Clojure offers
; a software transactional memory system and reactive Agent system that
; ensure clean, correct, multithreaded designs.
;
; I hope you find Clojure's combination of facilities elegant, powerful,
; practical and fun to use.
;
; Rich Hickey
;    - author of Clojure and CTO Cognitect


; Keys to Clojure:
;
; Dynamic
; Compiled
; Java interoperation
; Immutable data
; Lisp

; https://clojure.org/about/rationale


; comments start with a single semi-colon, like this ';'



; Clojure has all the 'usual suspects', also known as 'data literals'

"strings"      ; strings
1              ; integers/longs
12.0           ; doubles
12N            ; BigInt
3/7            ; ratios
0xa0           ; hex
true           ; booleans
false
#"\w+"         ; regular expressions (regex)

; and they are "real" Java types under the hood

(class "strings")
(class 1)
(class 12.0)
(class 12N)
(class true)
(class 0xa0)
(class 3/7)
(class #"\w+")


; Clojure also has a 'new' kind of thing:

; keywords
:this-is-a-keyword

; keywords are kind of like 'enums', they evaluate to themselves

; how is this useful? well, in Clojure we use them a lot as the
; keys in hash-maps

{:this-is-the-key "and this is the value"}


; which leads us to Collections


; Clojure has 4 basic collection types

'(1 2 "three" 4)                ; lists
[1 2 3 "four"]                  ; vectors
#{1 "two" 3 4}                  ; sets of unique values (with REAL "set-math" support!)
{:a 1 :b "two"}                 ; and hash-maps (key/value pairs)

; NOTE: we can type this data directly into our program,
; no "builders" needed



; notice that all Clojure collections are heterogeneous; they can hold 
; values of any kind
;
; including other collections!

["name" {:first-name "bob" :last-name "wilkins"
         :posts #{8768 9809 7765 8787}}]

; this makes Clojure collections VERY flexible

; in fact, hash-maps can use any type as the KEY and any type AS the VALUE:

{:a-keyword "to a string"
 "or a string to an int" 3
 117 :or-an-int-to-a-keyword
 :a-hash-map {:one 1 :two 2 :three 3
              :more-hash-maps {:deep1 1 :deep2 "two" :deep3 #{1 2}}}}

; and you can spread these collections over multiple lines
; (just like XML or JSON...)

; keywords also have another interesting property in Clojure:
;     they are functions that take a hash-map and return the value of
;     that keyword as the key!

(def m {:a-keyword "to a string"
        "or a string to an int" 3
        117 :or-an-int-to-a-keyword
        :a-hash-map {:one 1 :two 2 :three 3
                     :more-hash-maps {:deep1 1 :deep2 "two"
                                      :deep3 #{1 2}}}})

; as if   m.:a-hash-map()
(:a-hash-map m)
(:more-hash-maps (:a-hash-map m))
(:deep2 (:more-hash-maps (:a-hash-map m)))

; but this can be hard to read and understand, so Clojure adds the notion of 'threading'

(-> m
    :a-hash-map
    :more-hash-maps
    :deep2)

; 'threading' means "pass each result into the next function"

; also note that if the function takes just 1 parameter, you can drop the parentheses
;    (although you CAN if you prefer)



; the core library also has a function for doing this:

(get-in m [:a-hash-map :more-hash-maps :deep2])        ; kind of like XPath

; but threading is more general, since each 'step' can be any function we want

(-> m
    :a-hash-map
    count)

; which is the same as:

(count (:a-hash-map m))


; we can 'add' new key/value pairs with ASSOC (associate):
(assoc m :new-key "new value")
(def new-m (assoc m :new-key "new value"))

; note that 'm' is unchanged - IMMUTABILITY

; assoc has a 'pathed' cousin: assoc-in
(assoc-in m [:a-hash-map :more-hash-maps :deep4] 4)

; NOTE: 'assoc' is an "UPSERT" (replace if found, insert if not found)
(assoc-in m [:a-hash-map :more-hash-maps :deep1] 4)


; what if you want to 'change' something in the hash-map, but you need the original value
; as part of the new value?
;
; i.e., "add 10 to the existing value"
;
(update {:name "James" :age 26} :age inc)


; and there is, of course, a "pathed" version
(update-in m [:a-hash-map :more-hash-maps :deep1] + 10)
(update-in m [:a-hash-map :more-hash-maps :deep1] inc)
(update-in m [:a-hash-map :more-hash-maps :deep1] dec)

; update-in takes the collection, the path, and a function to run using the existing value

(get-in m [:a-hash-map :more-hash-maps :deep1])

; we can also remove things from a hash-map using DISSOC (dissociate):
(dissoc m "or a string to an int")

; NOTE: there is NO pathed version of dissoc. Why? You can just combine dissoc
; with update or update-in:
(update {:a {:b 0 :c {"1" 1}}} :a dissoc :b)

(update-in {:a {:b 0 :c {"1" 1}}} [:a] dissoc :b)
(update-in {:a {:b 0 :c {"1" 1}}} [:a :c] dissoc "1")


; earlier we noted that we can type this data directly into our program
;
; this differs from having to use code to assemble things:

(def m2
  (-> {}
      (assoc :a-keyword "to a string")
      (assoc "or a string to an int" 3)
      (assoc 117 :or-an-int-to-a-keyword)
      (assoc :a-hash-map (-> {}
                             (assoc :one 1)
                             (assoc :two 2)
                             (assoc :three 3)
                             (assoc :more-hash-maps
                               (-> {}
                                   (assoc :deep1 1)
                                   (assoc :deep2 "two")
                                   (assoc :deep3 (set (vector 1 2)))))))))

; more code, less readable for exactly the same thing
(= m m2)

; NOTE: this is REAL "value equality" - two things are the same if they
; contain exactly the same data


; you can also see that Clojure is very forgiving with delimiters, especially 
; commas ',' 
;
; you are free to use them if you want, but they are treated as "whitespace" by
; the system and just ignored.

; so, this

[1 2 3 4]

; and this

[1, 2, 3, 4]

; are the same thing:

(= [1 2 3 4] [1, 2, 3, 4])

[1 2 3]
[1 ,,,,,, 3 4]


; Function calls might look weird if you've only ever worked with languages where that first
; paren was to the 'right' of the first item, like this 
;
;          get-in( m, [:a-hash-map :more-hash-maps :deep1] )


; but Clojure is different. It is a LISP (http://jmc.stanford.edu/articles/lisp/lisp.pdf)
; and in a LISP, the name of the function is just the first item in a list.

; Yup, LISP programs are actually just "lists", collections of stuff that the compiler
; treats as a program!

; Data, not text, sometimes described as "Data is Code. Code is Data."




; You've already seen how we can just type stuff into the editor and execute it
; using the REPL. We'll talk more about the REPL in other sessions. For now, just know
; that the REPL is a tool for developing software interactively.


; let's see how we can add some 'variables' to our software

(def x 100)

; def, short for 'define', is the function for creating 'named values', what other
; languages might call 'variables.' Since Clojure is IMMUTABLE, 'x' can't be changed*

(def y x)         ; make y have the same value as x
x
y
(= y x)

(+ 2 x)           ; adding 2 to 'x' doesn't change the value of x 
(def z (+ 2 x))   ; make z, have the value of 'x + 2'
z

; * at the REPL, you can re-evaluate a line, so you can change your mind as you
;   write your code, but in 'production' code you are discouraged from such
;   behavior.
;
; like this:

(def x 500)

x                 ; now 'x has a new value, 500
y                 ; but y and z don't change, because we did NOT re-run their def's
z
(= y x)

; This gives the REPL tremendous power for interactive development, but don't
; abuse this power. We prefer NOT to redefine names in our production code, save
; re-def for figuring things out and then do it "the right way".



; defining functions


; No special syntax for this, Clojure just uses a function for defining other functions,
; called 'defn' (define-function)

(defn f [a]
  (+ a 5))

; we define function 'f' which takes 1 parameter (always expressed in a vector) 'a',
; and returns (+ a 5)
;
;    NOTE: no explicit 'return' in Clojure, the last evaluated expression is always returned

(class f)

(f 1)
(f (f 1))
(-> 1
    f
    f
    f)


; Clojure also has map, filter, and reduce, like any good FP language

(map f [0 1 2 3])
(filter even? [0 1 2 3])
(filter int? [0 1 2 3])
(filter boolean? [0 1 2 3])
(reduce + 0 [0 1 2 3])

; and about 200 more...  (see the cheatsheet at https://clojure.org/api/cheatsheet)

(reverse [0 1 2 3])
(remove even? [0 1 2 3])
(take 2 [0 1 2 3])
(take-last 2 [0 1 2 3])
(last [0 1 2 3])
(first [0 1 2 3])
(nth [0 1 2 3] 2)
(count [0 1 2 3])
(interpose 7 [0 1 2 3])
(interpose "," ["one" "two" "three"])
(partition 2 [0 1 2 3 4])
(partition-all 2 [0 1 2 3 4])
(range 13)
(range 4 7)
(partition-all 3 (range 13))

; lots of little, single-purpose functions give us lots of 'lego' to compose
; into software:


         ; "It is better to have 100 functions on 10 data structures than to have
         ;  10 functions on 100 data structures"  - Alan Perlis


; you may also have noticed that we don't get a vector '[]' back. Why?
;
; it's called the 'Sequence Abstraction' and it's a key to Clojure's power, even
; as compared to other LISPs.
;
; a sequence is nothing more than a 'generic' collection that supports (first) and (rest)
;

(first [0 1 2 3])
(rest [0 1 2 3])

(first "string")
(rest "string")

(first {:a "one" :b 2 :three #{"three"}})    ; why a vector here? Because order IS important
(rest {:a "one" :b 2 :three #{"three"}})

(->> {:a "one" :b 2 :three #{"three"}}
  rest
  (into {}))

;  these functions work on ANY collection;
;
; we DON'T have (first-vec) and (first-string) and (first-hashmap)
;    because we don't NEED THEM.

; all other functions are built on-top of these

; and "everything" is treated as a sequence:
   ; collections
   ; directories
   ; files
   ; xml
   ; json
   ; database results



; before we leave off, a little about Java Interop

; since Clojure compiles to Java, even when using the REPL, we can call any Java code
; whenever we like (as long as it's in the classpath**)

; ** more on classpaths in another session



; call a static method, such as Math.abs(), like this:
(Math/abs -2)

; calling a Java method, like s.toUpperCase(), uses 'dot-notation'
;
(.toUpperCase "this string is all lowercase")

; how does this wok?
(class "this string is all lowercase")


; you can work with Java Objects directly

; call a constructor:
;
(String. "a java string")

(def s (String. "a java string"))
(class s)

; when calling a method, "this" is passed as the 1st parameter:
; s.substring( 3, 7);
;
(.substring s 3 7)

; interop works both ways. pass Clojure data to Java methods
(String. (apply str (interpose "," ["one" "two" "three"])))

; pass Java data to Clojure functions
(interpose "," (String. "a java string"))

; or even
;
(->> "a java string"
     String.
     (interpose ",")
     reverse
     (apply str)
     String.)

; data transformation... FTW


; let's have one last bit of fun before we go...

; write a function that reads a book off the Internet and shows: 
;    - the 10 most used words 
;
; 
;    - the 10 least used words?

(def les-miserables "https://www.gutenberg.org/files/135/135-0.txt")
(def moby-dick "https://www.gutenberg.org/files/2701/2701-0.txt")
(def complete-shakespeare "https://www.gutenberg.org/files/100/100-0.txt")


; how would this work?
;   1. open the URI
;   2. read the file
;   3. convert to lower case
;   4. break into words
;   5. group the words by frequency of occurance
;   6. sort by "most"/"least"
;   7. take the first/last 10 answers




                                   ; thanks to rosettacode.org
(->> moby-dick
     java.net.URI.
     slurp
     .toLowerCase
     (re-seq #"\w+")
     frequencies
     (sort-by val >)
     (take 10))





; what if we wanted to see the longest word(s)?

(->> moby-dick
     java.net.URI.
     slurp
     .toLowerCase
     (re-seq #"\w+")
     (into #{})
     (group-by count)
     (sort-by first >)
     (take 3))





; Namespaces. we define it using a function. this adds data to the runtime
; environment, specifically creating a place that holds other definition. All code must
; be compiled into a namespace
;
; NOTE: Clojure is NOT file oriented, it is "stream oriented" meaning it compiles AND runs
; code as soon as it "sees" it.

; (ns gentle-intro-to-clojure)
*ns*
namespace
(all-ns)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

