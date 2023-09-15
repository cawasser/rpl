(ns mvs.dashboard.ui.medusa-gauge
  (:require [cljfx.composite :as composite]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.coerce :as coerce]
            [cljfx.fx.control :as fx.control])
  (:import [eu.hansolo.medusa Gauge Gauge$SkinType]))


(def dashboard (Gauge$SkinType/DASHBOARD))
(def level (Gauge$SkinType/LEVEL))
(def nasa (Gauge$SkinType/NASA))
(def simple-digital Gauge$SkinType/SIMPLE_DIGITAL)
(def space-x (Gauge$SkinType/SPACE_X))


(def props
  (merge
    {}
    (composite/props Gauge
      :title [:setter lifecycle/scalar :default "Gauge"]
      :value [:setter lifecycle/scalar :default 0]
      :unit [:setter lifecycle/scalar :default "Unit"]
      :skin-type [:setter lifecycle/scalar :default simple-digital])))


(def lifecycle
  (lifecycle/annotate
    (composite/describe Gauge
      :ctor []
      :props props)
    :gauge))



(comment
  (class (composite/props Gauge
           :title [:setter lifecycle/scalar :default "Gauge"]
           :value [:setter lifecycle/scalar :default 0]
           :unit [:setter lifecycle/scalar :default "Unit"]))
  (class (LevelSkin. (Gauge.)))


  ())

