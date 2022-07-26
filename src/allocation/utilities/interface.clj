(ns allocation.utilities.interface
  (:require [allocation.utilities.core :as core]
            [allocation.utilities.durable_storage :as ds]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; File IO Support
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn io-slurp-string [file]
  (core/io-slurp-string file))



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
  (core/mix-in m orig))

(defn denormalize-map->vector [m]
  (core/denormalize-map->vector m))


(defn denormalize-map->map [m]
  (core/denormalize-map->map m))


(defn denormalize-results [results]
  (core/denormalize-results results))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Packaging "messages" as [ {key} {value} ]
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn package-message [v k]
  (core/package-message v k))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; Durable Storage (ds/...)
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn reset-token
  "calls the given host:port and requests a new CSRF token for
  use in later calls"
  [host port]
  (ds/reset-token host port))


(defn get-current-allocations
  "Gets the current-allocations from the given host:port"
  [host port]
  (ds/get-current-allocations host port))


(defn get-open-requests
  "Gets the open-requests from the given host:port"
  [host port]
  (ds/get-open-requests host port))


(defn send-results-to-durable-storage
  "Sends any kind of data (your responsibility to format correctly)
  to the given host:port/uri for storage in some sort of durable format"
  [host port uri message]
  (ds/send-results-to-durable-storage host port uri message))


(defn get-aois
  "Gets the (sensor) aois from the given host:port"
  [host port]
  (ds/get-aois host port))


(defn get-platforms
  "Gets the (sensor) platforms from the given host:port"
  [host port]
  (ds/get-platforms host port))


(defn re-hydrate
  "Turns a"
  [map-to-stringified]
  (ds/re-hydrate map-to-stringified))