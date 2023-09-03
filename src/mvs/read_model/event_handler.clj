(ns mvs.read-model.event-handler)


(defmulti event-handler :event/type)

