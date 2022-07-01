(ns loco.dummy-solver)



(defn compute-fn [ctx [k event]]
  [k (assoc event :answer [42])])



(comment
  (compute-fn {} [1 {:event 1 :puzzle []}])

  ())