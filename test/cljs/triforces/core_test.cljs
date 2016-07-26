;;; This namespace is used for testing purpose. It use the
;;; clojurescript.test lib.
(ns triforces.core-test
  (:require-macros [cemerick.cljs.test :as m :refer (deftest testing are)])
  (:require [cemerick.cljs.test :as t]
            [triforces.core :refer (vector-add)]))

(deftest normalize-vector
  (testing "(vector-add [3 4] [1 2])"
    (is (= [4 6] (vector-add [3 4] [1 2]))) ))
