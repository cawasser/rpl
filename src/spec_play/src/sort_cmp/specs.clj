(ns sort-cmp.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :sort-cmp/long-title string?)
(s/def :data-packet/long-title string?)
(s/def ::doc-string string?)

(s/fdef sort-cmp.interface/num-sort
  :args (s/cat :coll (s/coll-of number?))
  :ret  (s/coll-of number?)
  :fn   (s/and
          #(= (-> % :ret)
             (-> % :args :coll sort))
          #(= (-> % :ret count)
             (-> % :args :coll count))))

(s/fdef sort-cmp.interface/num-sort-special
  :args (s/cat :coll (s/coll-of number?))
  :ret  (s/coll-of number?)
  :fn   (s/and
          #(= (-> % :ret)
             (-> % :args :coll sort))
          #(= (-> % :ret count)
             (-> % :args :coll count))))
