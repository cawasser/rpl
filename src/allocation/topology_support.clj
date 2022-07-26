(ns allocation.topology-support)


(defn the-xd [the-fn xf]
  (fn
    ([] (xf))
    ([result] (xf result))
    ([result event]
     (xf result (the-fn event)))))


