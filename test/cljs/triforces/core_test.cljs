;;; This namespace is used for testing purpose. It use the
;;; clojurescript.test lib.
(ns triforces.core-test
  (:require-macros [cemerick.cljs.test :as m :refer (deftest testing are)])
  (:require [cemerick.cljs.test :as t]
            [triforces.core :refer (vec-len)]))

(deftest normalize-vector
  (testing "(vec-len 3 4)"
    (is (= 5 (vec-len 3 4)))))
