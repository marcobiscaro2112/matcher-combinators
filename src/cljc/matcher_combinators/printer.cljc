(ns matcher-combinators.printer
  (:refer-clojure :exclude [print])
  (:require [clojure.pprint :as pprint]
            #?(:clj  [matcher-combinators.model]
               :cljs [matcher-combinators.model :refer [ExpectedMismatch
                                                        Mismatch
                                                        Missing
                                                        Unexpected
                                                        TypeMismatch
                                                        InvalidMatcherContext
                                                        InvalidMatcherType]])
            [matcher-combinators.ansi-color :as ansi-color])
  #?(:clj
     (:import [matcher_combinators.model ExpectedMismatch Mismatch Missing
               Unexpected TypeMismatch InvalidMatcherContext InvalidMatcherType])))

(defrecord ColorTag [color expression])

(defmulti markup-expression type)

(defmethod markup-expression Mismatch [{:keys [expected actual]}]
  (list 'mismatch
        (list 'expected (->ColorTag :yellow expected))
        (list 'actual (->ColorTag :red actual))))

(defmethod markup-expression ExpectedMismatch [{:keys [actual expected]}]
  (list 'mismatch
        (->ColorTag :yellow (symbol "expected mismatch from: "))
        (->ColorTag :yellow expected)
        (list 'actual (->ColorTag :red actual))))

(defmethod markup-expression Missing [missing]
  (list 'missing (->ColorTag :red (:expected missing))))

(defmethod markup-expression Unexpected [unexpected]
  (list 'unexpected (->ColorTag :red (:actual unexpected))))

(defmethod markup-expression TypeMismatch [{:keys [actual expected]}]
  (list 'mismatch
        (list 'expected (->ColorTag :yellow (type expected)))
        (list 'actual (->ColorTag :red (type actual)))))

(defmethod markup-expression InvalidMatcherType [invalid-type]
  (list 'invalid-matcher-input
        (->ColorTag :yellow (:expected-type-msg invalid-type))
        (->ColorTag :red (:provided invalid-type))))

(defmethod markup-expression InvalidMatcherContext [invalid-context]
  (list 'invalid-matcher-context
        (->ColorTag :red (:message invalid-context))))

(defmethod markup-expression :default [expression]
  expression)

(defn colorized-print [{:keys [color expression]}]
  (if color
    #?(:clj  (do (ansi-color/set-color color)
                 (pprint/write-out expression)
                 (ansi-color/reset))
       :cljs (pprint/write-out (ansi-color/style expression color)))
    (pprint/write-out expression)))

(defn print-diff-dispatch [expression]
  (let [markup (markup-expression expression)]
    (if (instance? ColorTag markup)
      (colorized-print markup)
      (pprint/simple-dispatch markup))))

(defn pretty-print [expr]
  (pprint/with-pprint-dispatch
    print-diff-dispatch
    (pprint/pprint expr)))

(defn as-string [value]
  (with-out-str
    (pretty-print value)))
