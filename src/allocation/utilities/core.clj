(ns allocation.utilities.core
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; File IO Support
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn io-slurp-string
  "Takes in a filename (or path) as a string and
  slurps its contents into edn"
  [file]
  (try
    (->> file
         io/resource
         slurp
         edn/read-string)
    (catch Exception e (log/error "Unable to slurp" file "reason" (.getMessage e)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Flattening/Denormalizing Results
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn mix-in [m orig]
  (map #(merge m %) orig))

(defn denormalize-map->vector
  "takes a map val(key):vector(value) and returns a vector of maps of 3 k/v pairs:

  :key   - the original value of the key
  :index - the integer index to the 'value'
  :value - the value in the original vector

  for example, assume 'm' is:
      {:key-1 [1 2 3 4]}

  (denormalize-map m) returns:
      [{:key :key-1 :index 0 :value 1}
       {:key :key-1 :index 1 :value 2}
       {:key :key-1 :index 2 :value 3}
       {:key :key-1 :index 3 :value 4}]

  The function is especially useful a prep for writing a map into a database,
  typically handled in Clojure a one map per row, collected in a vector or seq."

  [m]
  (let [[k v] m]
    (map-indexed (fn [idx each-v]
                   {:key k :index idx :value each-v})
      v)))


(defn denormalize-map->map
  [m]
  (map (fn [[k v]] (merge {:key k} v)) m))


(defn denormalize-results
  "helper function to map (denormalize-map) over a large result set, where
  each entry is a map that needs to be denormalized. This is provided to
  ensure we get all the denorm'ed data back in a SINGLE vector (mapcat), al la:

    for example, assume 'm' is:
      {:key-1 [1 2 3 4] :key-2 [10 20]}

  (denormalize-results m) returns:
      [{:key :key-1 :index 0 :value 1}
       {:key :key-1 :index 1 :value 2}
       {:key :key-1 :index 2 :value 3}
       {:key :key-1 :index 3 :value 4}
       {:key :key-2 :index 0 :value 10}
       {:key :key-2 :index 1 :value 20}]
"

  [results]
  (mapcat denormalize-map->vector results))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Packaging "messages" as [ {key} {value} ]
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn package-message
  "correctly reassembles the message value and it's key

  NOTE: the message content (v) MUST go FIRST in order to use
  the 'thread-first' macro"

  [v k]
  [k v])



