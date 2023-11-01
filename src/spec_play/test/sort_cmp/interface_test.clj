(ns sort-cmp.interface-test
  (:require [sort-cmp.interface :as sut]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]))


(deftest call-sut
  (is (s/valid? sut/data-title-spec "title")))


(deftest call-sut-2
  (is (s/valid? sut/data-title-spec "title")))


(deftest call-sut-get-title
  (is (s/valid? sut/data-title-spec (sut/get-title)))
  (is (s/valid? sut/title-spec (sut/get-title))))


(deftest call-num-sort-success
  (is (-> (stest/check `spec-play.interface/num-sort)
        first
        :clojure.spec.test.check/ret
        :pass?)))


(deftest call-num-sort-failure
  (is (= false
       (-> (stest/check `spec-play.interface/num-sort-special)
         first
         :clojure.spec.test.check/ret
         :pass?))))


(comment
  (spec-play.sut/num-sort [10 4 17 2])
  (spec-play.sut/num-sort-special [10 4 17 2])
  (spec-play.sut/num-sort-special [10 4 3 17 2])


  ())
