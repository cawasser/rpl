(ns mvs.dashboard.ui.state)


(def app-db (atom {}))


(defn init [sub-key data]
  (swap! app-db assoc sub-key data))


(defn add-row [sub-key row]
  (swap! app-db assoc sub-key (conj (sub-key @app-db) row)))

