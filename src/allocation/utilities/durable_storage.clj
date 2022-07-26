(ns allocation.utilities.durable_storage
  (:require [clj-http.client :as client]
            [cheshire.core :as ch]
            [clojure.tools.logging :as log]
            [safely.core :refer [safely]]))


(def token (atom nil))
(def cookie-store (clj-http.cookies/cookie-store))

(defn- retrieve-csrf-token-from-server [host port]
   (let [body        (-> (client/get
                           (str "http://" host ":" port "/")
                           {:throw-exceptions false
                            :cookie-store     cookie-store
                            :accept           :edn})
                         :body)
         token-regex (str "csrfToken = \"(.+?)\"")]
        (reset! token
                (-> token-regex
                    re-pattern
                    (re-find body)
                    second))))


(defn- get-csrf-token
  "take the response body from a get request and pull out the csrf token so it can be used in later
  POST calls to the same server

  code from

  https://www.brettlischalk.com/posts/using-clj-http-when-a-web-app-has-csrf-protection"

  [host port]
  (if (nil? @token)
    (retrieve-csrf-token-from-server host port)
    @token))



(defn- get-from-uri
  "call a server at a given host:port/URI"
  [host port uri]
  ; TODO: (get-from-uri ...) should be specified by a config for an exit channel
  (-> (client/get
        (str "http://" host ":" port "/" uri)
        {:accept :edn})
      :body
      (ch/parse-string true)))



(defn get-current-allocations
  "go to service-local storage and get the current value of the allocation (grid)"
  [host port]
  (->>
    (get-from-uri host port "current-allocations")
    :allocations
    :allocations
    clojure.edn/read-string))



(defn get-open-requests [host port]
  (->>
    (get-from-uri host port "requests")
    :requests
    (map (juxt :id :requests))
    (into {})
    (map (fn [[k v]] {k (clojure.edn/read-string v)}))
    (apply merge)))


(def last-message (atom nil))


(defn send-results-to-durable-storage
  "because our results set might be HUGE, we should put them into
  durable storage"
  [host port uri message]

  (reset! last-message message)

  (log/info "send-results-to-durable-storage" (str "http://" host ":" port "/" uri) (keys (:message message)))

  (get-csrf-token host port)
  (safely
    (-> (client/post
            (str "http://" host ":" port "/" uri)
            {:headers          {"X-CSRF-Token" @token}
             :form-params      message
             :content-type     :json
             :throw-exceptions false
             :cookie-store     cookie-store}))

    :on-error
    :max-retries 2
    :retry-delay [:fix 3000]
    :failed? #(do
                 (retrieve-csrf-token-from-server host port)
                 (= 403 (:status %)))))


(defn re-hydrate [map-to-stringified]
  (zipmap (keys map-to-stringified)
    (map clojure.edn/read-string (vals map-to-stringified))))



(defn get-aois [host port]
  (->>
    (get-from-uri host port "aoi-update")
    :aoi-update
    (map (juxt :id :aoi_grid))
    (into {})
    (map (fn [[k v]] {k (clojure.edn/read-string v)}))
    (apply merge)))


(defn get-platforms [host port]
  (->>
    (get-from-uri host port "platform-update")
    :platform-update))



(defn reset-token []
  (reset! token nil))


; get the csrf-token from the vanilla server
(comment
  (def cookie-store (clj-http.cookies/cookie-store))
  (def body (-> (client/get
                  (str "http://localhost:5000/")
                  {:throw-exceptions false
                   :cookie-store     cookie-store
                   :accept           :edn})
                :body))
  (get-csrf-token)

  ())

; use the token to call the POST /potential-allocations
(comment
  (def message {:event-name "potential-allocations"})


  ())

