(ns matcher-combinators.printer-test
  (:require [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            #?(:clj [colorize.core :as colorize])
            [matcher-combinators.model :as model]
            [matcher-combinators.printer :as printer]))

(def simple-double (gen/double* {:infinite? false
                                 :NaN?      false}))

(def scalars
  (gen/one-of [gen/small-integer gen/large-integer simple-double gen/char-ascii
               gen/string-ascii gen/ratio gen/boolean gen/keyword
               gen/keyword-ns gen/symbol gen/symbol-ns gen/uuid]))
(def any (gen/recursive-gen gen/container-type scalars))

(defspec markup-expression-ignores-regular-clojure-expressions
  (prop/for-all [expression any]
    (= expression (printer/markup-expression expression))))

(defspec markup-expression-marks-up-mismatches-in-yellow-and-red
  (prop/for-all [expected any
                 actual   any]
    (or (= expected actual)
        (= (list 'mismatch
                 (list 'expected (printer/->ColorTag :yellow expected))
                 (list 'actual (printer/->ColorTag :red actual)))
           (printer/markup-expression (model/->Mismatch expected actual))))))

(defspec markup-expression-marks-up-missing-values-in-red
  (prop/for-all [expected any
                 actual   any]
    (or (= expected actual)
        (= (list 'missing
                 (printer/->ColorTag :red expected))
           (printer/markup-expression (model/->Missing expected))))))

(defspec markup-expression-marks-up-unexpected-values-in-red
  (prop/for-all [expected any
                 actual   any]
    (or (= expected actual)
        (= (list 'unexpected
                 (printer/->ColorTag :red expected))
           (printer/markup-expression (model/->Unexpected expected))))))

(defspec pprint-uses-as-string
  (prop/for-all [exp any]
    (= (with-out-str (pprint/pprint exp))
       (printer/as-string exp))))

#?(:clj (def one-in-red (colorize/red 1)))
;; output in cljs is slightly different, because we print everything as a string (which includes double quotes)
#?(:cljs (def one-in-red "\"[31m1[0m\""))

(deftest test-as-string
  (testing "when an expression can be marked up, uses color tags to as-string in color"
    (is (= (str "(unexpected " one-in-red ")\n")
           (printer/as-string (list 'unexpected (printer/->ColorTag :red 1)))))
    (testing "when printing a regular expression, just pprint it"
      (is (= "{:aaaaaaaaaaaa [100000000 100000000 100000000 100000000 100000000],\n :bbbbbbbbbbbb [200000000 200000000 200000000 200000000 200000000]}\n"
             (printer/as-string {:aaaaaaaaaaaa [100000000 100000000 100000000 100000000 100000000]
                                 :bbbbbbbbbbbb [200000000 200000000 200000000 200000000 200000000]}))))))
