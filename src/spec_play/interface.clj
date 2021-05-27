(ns spec-play.interface
  (:require [spec-play.sut :as core]
            [spec-play.specs]))


(def title-spec :data-packet/long-title)
(def doc-string-spec :spec-play.specs/doc-string)
(def data-title-spec :data-packet/long-title)

(defn get-title []
  (core/get-title))

(comment
  (require '[clojure.spec.alpha :as s])

  (s/valid? title-spec (core/get-title))
  ,)
