(ns allocation.service-handler
  (:require [clojure.pprint :refer :all]
            [rolling-stones.core :as sat :refer [! NOT AND OR XOR IFF IMP NOR NAND
                                                 at-least at-most exactly]]
            [clojure.tools.logging :as log]
            [allocation.utilities.interface :as util]
            [digest :as d]
            [cprop.core :refer [load-config]]
            [cprop.source :as source]))

; region ; Message Handling Support
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn lookup-keyset [message-def]
  (:key-set message-def))


(defn create-key [message-def event]
  (let [key-set (lookup-keyset message-def)
        result  (->> event
                  ((apply juxt key-set))
                  (zipmap key-set))]
    ;(log/info "create-key" result)
    result))

(defn- construct-message [sha time-stamp]
  {:event-name  "sensor-allocations"
   :sha         sha
   :computed-at time-stamp})

; endregion


; region ; Rules Engine
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- make-grid [rows cols]
  (into (sorted-set)
    (for [r (range rows)
          c (range cols)]
      [r c])))


(defn- triple-contains?
  "returns true is the [row col type] (triple) is for the same location
  as 'cell' [row col]"
  [triple cell]
  (not-empty
    (filter (fn [[t-r t-c _ :as t]]
              (if (= [t-r t-c] cell)
                t nil))
      triple)))


(defn platform-location [[start-row start-col] direction time-t]
  (condp = direction
    "horz" [start-row (mod (+ start-col time-t) 10)]
    "vert" [(mod (+ start-row time-t) 10) start-col]
    "geo" [start-row start-col]
    [0 0]))


(defn- convert-aois [aois]
  (->> aois
    vals
    (apply clojure.set/union)
    (map (fn [[row col sensor-type time-t]]
           {time-t #{[row col sensor-type]}}))
    (apply merge-with clojure.set/union)
    (into (sorted-map))))


(defn coverage [[rows columns]
                sensor_type
                [point-row-offset point-col-offset]
                [row-offset column-offset]]
  (into #{}
    (for [col (range (+ column-offset point-col-offset)
                (+ column-offset point-col-offset columns))
          row (range (+ row-offset point-row-offset)
                (+ row-offset point-row-offset rows))]
      [(mod row 10) (mod col 10) sensor_type])))


(defn- one-coverage [platform_id sensor_id starting_location platform_path
                     sensor_size sensor_type time-t row col]
  {:p-name platform_id
   :s-name sensor_id
   :coverage
   (coverage
     sensor_size
     sensor_type
     [row col]
     (platform-location starting_location platform_path time-t))})


(defn- coverages-at [time-t
                     {:keys [platform_id sensor_id starting_location platform_path
                             sensor_type sensor_size sensor_steering]}]

  (let [[row-offset-min row-offset-max] (first sensor_steering)
        [col-offset-min col-offset-max] (second sensor_steering)]

    (cond
      (and (= 0 row-offset-min row-offset-max)
        (= 0 col-offset-min col-offset-max))
      [(one-coverage platform_id sensor_id starting_location platform_path
         sensor_size sensor_type time-t 0 0)]

      (= 0 row-offset-min row-offset-max)
      (for [col (range col-offset-min col-offset-max)]
        (one-coverage platform_id sensor_id starting_location platform_path
          sensor_size sensor_type time-t 0 col))

      (= 0 col-offset-min col-offset-max)
      (for [row (range row-offset-min row-offset-max)]
        (one-coverage platform_id sensor_id starting_location platform_path
          sensor_size sensor_type time-t row 0))

      :else
      (for [row (range row-offset-min row-offset-max)
            col (range col-offset-min col-offset-max)]
        (one-coverage platform_id sensor_id starting_location platform_path
          sensor_size sensor_type time-t row col)))))


(defn- make-coverages [time-t x]
  (->> x
    (coverages-at time-t)
    (map-indexed vector)
    (map (fn [[idx c]]
           (assoc c :c-idx idx)))))


(defn- reformat [sensor cooked-sensors]
  (let [{:keys [p-name s-name coverage]} (get cooked-sensors sensor)]
    (into {}
      (for [cell coverage]
        {cell #{{:platform p-name :sensor s-name}}}))))


(defn- build-symbolics [{:keys [p-name s-name c-idx]} time-t]
  (keyword (str p-name "-" s-name "-" time-t "-" c-idx)))


(defn- build-group [{:keys [p-name s-name]} time-t]
  (keyword (str p-name "-" s-name "-" time-t)))


(defn- make-symbolic-sensors
  ; TODO: needs to depend on time-t - does it?
  [time-t sensors]
  (into {}
    (map (fn [s]
           {(build-symbolics s time-t) (assoc s
                                         :time-t time-t
                                         :name (build-symbolics s time-t)
                                         :group (build-group s time-t))})
      sensors)))


(defn- find-coverings [symbolic-sensors cell]
  (remove nil?
    (map (fn [[k {:keys [coverage]}]]
           (if (some? (triple-contains? coverage cell)) k))
      symbolic-sensors)))


(defn- find-coverings-by-type [symbolic-sensors sensor-type cell]
  (->> symbolic-sensors
    (filter (fn [[k {:keys [coverage]}]]
              (if (some? (not-empty
                           (filter (fn [[t-r t-c t-t :as t]]
                                     (if (and (= [t-r t-c] cell)
                                           (= t-t sensor-type))
                                       t nil))
                             coverage)))
                k)))
    (map first)))


(defn- build-grid-rules [time-t-aois symbolic-sensors grid]
  (for [cell grid]
    (if-let [[_ _ sensor-type] (first (triple-contains? time-t-aois cell))]
      (at-least 1 (into #{} (find-coverings-by-type symbolic-sensors sensor-type cell)))
      (at-least 0 (into #{} (find-coverings symbolic-sensors cell))))))


(defn- build-platform-rules [sensors]
  (let [groups  (into #{} (map (fn [[_ v]] (:group v)) sensors))
        details (vals sensors)]
    (map (fn [group]
           (let [g (filter #(= (:group %) group) details)]
             (exactly 1 (into #{} (map :name g)))))
      groups)))


(defn- reformat-sensors [sensor]
  (let [{:keys [p-name s-name coverage]} sensor]
    (into {}
      (for [cell coverage]
        {cell #{{:platform p-name :sensor s-name}}}))))


(defn- reformat-solution [time-t rules]
  (->> rules
    sat/solve-symbolic-formula
    sat/true-symbolic-variables
    (map #(reformat-sensors %))
    (apply merge-with clojure.set/union)
    (map (fn [[k v]] {:time-t time-t :cell k :coverage v}))))


(defn- make-rules [time-t time-t-aois symbolic-sensors]
  (apply merge
    (build-grid-rules time-t-aois symbolic-sensors (make-grid 10 10))
    (build-platform-rules symbolic-sensors)))


(defn- compute-at [sensors time-t-aois time-t]
  (let [symbols (make-symbolic-sensors time-t (->> sensors
                                                (map #(make-coverages time-t %))
                                                flatten))]
    (->> symbols
      (make-rules time-t time-t-aois)
      sat/solve-symbolic-formula
      sat/true-symbolic-variables
      (map #(reformat % symbols))
      (apply merge-with clojure.set/union)                  ; merge by sensor-type
      (map (fn [[[r c _] v]] {[r c] v}))                    ; drop off the sensor-types
      (apply merge-with clojure.set/union)                  ; merge across all senor types
      (map (fn [[[r c _] v]] {:time time-t :cell [r c] :coverage v}))
      (into [])
      (sort-by (juxt :time :cell)))))


(defn compute [aois sensors from-time to-time time-stamp]
  (log/info "compute" (keys aois) sensors)
  (let [time-range     (range from-time to-time)
        converted-aois (convert-aois aois)]
    (->> time-range
      (map #(compute-at sensors (get converted-aois %) %))
      flatten
      (map (fn [r] (assoc r :computed_at time-stamp)))
      (sort-by (juxt :time :cell)))))



; endregion


; region ; Durable Storage


(defn- store-results [host port result-message results]
  (log/info "STORE-RESULTS >>>>>>>>>>>>>>>>>>>>>>>"
    (->> results (map :time) (into (sorted-set))))
  (util/send-results-to-durable-storage
    host port "sensor-allocations"
    {:message (assoc result-message :sensor-allocations (pr-str results))}))

; endregion


; region ; Public Interfaces
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; these support debugging the service using the repl
;
(def last-results (atom nil))
(def last-event (atom nil))
(def last-compute-event (atom nil))
(def last-message-def (atom nil))

;(def environment (delay (load-config
;                          :merge
;                          [(source/from-env)])))
;
;(defn app-config []
;  (log/info "Loading config file in Sensor-allocs; is prod?: " (:prod (force environment)))
;  (if (not ((force environment) :prod))     ;if its NOT prod
;    "localhost"                             ;vanilla is @ localhost
;    (get-in (force environment) [:docker-names :vanilla])))      ;otherwise use vanilla docker name


(defn operation [message-def [k event]]
  (log/info "SENSOR-ALLOCATION" event)

  (reset! last-event event)
  (reset! last-message-def message-def)

  (let [host           "localhost"
        port           5000
        aois           (util/get-aois host port)
        platforms      (util/get-platforms host port)
        ; TODO: find the 'last' time-t for the aois, so we know how many
        ; hours to predict
        to-time        10
        time-stamp     (str (java.time.LocalDateTime/now))
        results        (compute aois platforms 0 to-time time-stamp)
        sha            (d/sha-256 (pr-str platforms aois))
        result-message (construct-message sha time-stamp)
        result-key     (create-key message-def result-message)]

    ;(log/info "compute result-key" (create-key message-def results))
    ;(log/info "compute key-set" (:key-set message-def))
    ;(log/info "compute timestamp" time-stamp)
    ;(log/info "compute result-message" (count result-message))

    (store-results host port result-message results)

    [result-key result-message]))

; endregion


; region ; Rich Comments

; generate a default set of sensor-allocations for bootstrapping the system
(comment
  (do
    (def time-t 0)
    (def host "localhost")
    (def port 5000)
    (def aois (util/get-aois host port))
    (def converted-aois (convert-aois aois))
    (def time-t-aois (get converted-aois time-t))
    (def platforms (util/get-platforms host port))
    (def sensors platforms)
    (def from-time 0)
    (def to-time 10)
    (def time-stamp (str (java.time.LocalDateTime/now))))
  ;(def message-def {:name        "sensor-allocations"
  ;                  :type        :rabbitmq
  ;                  :channel     "sensor-allocs-view"
  ;                  :spec        [:views/sensor-allocations "sensor-allocations-error"]
  ;                  :key-set     [:event-name :sha :computed-at]
  ;                  :forbidden   #{}
  ;                  :valid-as-of {:sha "<some git sha>" :date "<date>"}}))
  (do
    (require '[clojure.inspector])
    (def filename "./development/default-data/comms-sensor-allocations.edn"))

  (spit filename
    (pr-str (compute aois sensors 0 10 time-stamp)))


  (-> filename
    slurp
    clojure.edn/read-string
    clojure.inspector/inspect-table)


  ())


;run one-off computations using the current aoi and platform data in durable storage
(comment
  (do
    (def time-t 0)
    (def host "localhost")
    (def port 5000)
    (def aois (util/get-aois host port))
    (def converted-aois (convert-aois aois))
    (def time-t-aois (get converted-aois time-t))
    (def platforms (util/get-platforms host port))
    (def sensors platforms)
    (def from-time 0)
    (def to-time 10)
    (def time-stamp (str (java.time.LocalDateTime/now)))
    (def message-def {:name        "sensor-allocations"
                      :type        :rabbitmq
                      :channel     "sensor-allocs-view"
                      :spec        [:views/sensor-allocations "sensor-allocations-error"]
                      :key-set     [:event-name :sha :computed-at]
                      :forbidden   #{}
                      :valid-as-of {:sha "<some git sha>" :date "<date>"}}))

  (operation message-def [{:event-type "dummy-event"} {}])

  (defn arrange-results [r]
    (->> r
      (map (fn [{:keys [time cell coverage computed_at]}]
             (let [[row col _] cell]
               (map (fn [{:keys [platform sensor]}]
                      {:platform platform :sensor sensor :time time
                       :cell [row col] :computed_at computed_at})
                 coverage))))
      flatten
      (sort-by (juxt :time :platform :sensor :cell))
      (map (juxt :time :cell :platform :sensor :computed_at))
      (sort-by (juxt first second))))

  (defn by-cells [r]
    (->> r
      (map (fn [{:keys [time cell coverage]}]
             (let [[row col _] cell]
               {:cell [row col] :time time :coverage coverage})))
      (sort-by (juxt :time :cell))))

  (def results (compute-at sensors (get converted-aois 3) 3))
  (by-cells results)

  (arrange-results (compute-at sensors (get converted-aois 0) 0))
  (arrange-results (compute-at sensors (get converted-aois 1) 1))
  (arrange-results (compute-at sensors (get converted-aois 2) 2))
  (arrange-results (compute-at sensors (get converted-aois 3) 3))

  (arrange-results (compute aois sensors 0 4 time-stamp))


  ; re-working (compute-at)
  (let [symbols (make-symbolic-sensors time-t (->> sensors
                                                (map #(make-coverages time-t %))
                                                flatten))]
    (->> symbols
      (make-rules time-t time-t-aois)
      sat/solve-symbolic-formula
      sat/true-symbolic-variables
      (map #(reformat % symbols))
      (apply merge-with clojure.set/union)                  ; merge by sensor-type
      (map (fn [[[r c _] v]] {[r c] v}))                    ; drop off the sensor-types
      (apply merge-with clojure.set/union)                  ; merge across all senor types
      (map (fn [[[r c _] v]] {:time time-t :cell [r c] :coverage v}))
      (into [])
      (sort-by (juxt :time :cell))))



  (def results (compute aois sensors 0 4 (str (java.time.LocalDateTime/now))))

  (->> results
    (map :time)
    (into #{}))


  (let [aois      (util/get-aois host port)
        platforms (util/get-platforms host port)
        ; TODO: find the 'last' time-t for the aois, so we know how many
        ; hours to predict
        to-time   10]
    (with-types/compute aois platforms 0 to-time))




  (make-symbolic-sensors 0 platforms)

  (def c-aois (convert-aois aois))

  ; from (compute ...)
  (let [c-aois     (convert-aois aois)
        time-range (range from-time to-time)]
    (sort-by (juxt :time :cell)
      (flatten
        (map #(compute-at platforms (get c-aois %) %)
          time-range))))

  ; from (operation ...)
  (let [aois           (util/get-aois host port)
        platforms      (util/get-platforms host port)
        ; TODO: find the 'last' time-t for the aois, so we know how many
        ; hours to predict
        to-time        10
        time-stamp     (str (java.time.LocalDateTime/now))
        results        (compute aois platforms 0 to-time time-stamp)
        sha            (d/sha-256 (pr-str platforms aois))
        result-message (construct-message sha time-stamp)
        result-key     (create-key message-def result-message)]

    results)
  ;(store-results result-message results))

  ())


; what's getting into the DB?
(comment
  (def allocs (let [aois           (util/get-aois host port)
                    platforms      (util/get-platforms host port)
                    ; TODO: find the 'last' time-t for the aois, so we know how many
                    ; hours to predict
                    to-time        10
                    time-stamp     (str (java.time.LocalDateTime/now))
                    results        (compute aois platforms 0 to-time time-stamp)
                    sha            (d/sha-256 (pr-str platforms aois))
                    result-message (construct-message sha time-stamp)
                    result-key     (create-key message-def result-message)]

                results))

  (def result-message (let [aois       (util/get-aois host port)
                            platforms  (util/get-platforms host port)
                            time-stamp (str (java.time.LocalDateTime/now))
                            sha        (d/sha-256 (pr-str platforms aois))]
                        (construct-message sha time-stamp)))

  {:sensor-allocations
   (map (juxt :time :cell :coverage)
     (mapv #(zipmap (keys %) (map pr-str (vals %)))
       allocs))}


  (count allocs)
  (count (into #{} (map (fn [x] #{(:time x)}) allocs)))

  (util/send-results-to-durable-storage
    "localhost" 5000 "sensor-allocations"
    {:message (assoc result-message :sensor-allocations (pr-str allocs))})


  ())


; all the aoi needs?
(comment
  (do
    (def host "localhost")
    (def port 5000)
    (def aois (util/get-aois host port)))

  (->> aois
    vals
    (apply clojure.set/union)
    (sort-by (juxt (fn [[_ _ x]] x) first second)))


  ())


; endregion
