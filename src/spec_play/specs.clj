(ns spec-play.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :spec-play.sut/long-title string?)
(s/def :data-packet/long-title string?)
(s/def ::doc-string string?)

