(ns spec-play.test.test-sut
  (:require [spec-play.interface :as sut]
            [clojure.spec.alpha :as s]
            [clojure.test :refer :all]))


(deftest call-sut
  (is (s/valid? sut/data-title-spec "title")))

(deftest call-sut-2
  (is (s/valid? sut/data-title-spec "title")))

(deftest call-sut-get-title
  (is (s/valid? sut/data-title-spec (sut/get-title)))
  (is (s/valid? sut/title-spec (sut/get-title))))



