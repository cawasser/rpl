(ns ui-data-specs)


; REQUEST
;
; type -> REQUEST
;
; contents -> { key value }
;
; actions -> :primary -> "Submit", #(submit) | :secondary  -> "", ()
;
; key -> :requestor-id
;
; :requestor-id -> keyword?
;
; value -> #{:cells}
;
; :cells -> [:channel :time-slot]
;
; :channel -> integer? | [integer?]
;
; :time-slot -> integer? | #{integer?}
;
;
;
; GRID
;
; type -> ALLOCATION-GRID
;
; content -> [[:allocation]]
;
; :allocation -> #{:requestor-id}
;

(def requests {})
(def current-grid [])
(def potential-grid [])

(defn submit [])


[:down
 [:label.left "Requests to consider"]
 [:grid requests]
 [:button.right.primary "Submit" submit]
 [:across
  [:down
   [:label.center "Current Allocations"]
   [:grid current-grid]]
  [:down
   [:label.center "Potential Allocations"]
   [:grid potential-grid]]]]



