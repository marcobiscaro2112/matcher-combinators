(ns matcher-combinators.parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [matcher-combinators.core :as core]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.parser :as parser]))

#?(:clj
   (def gen-big-decimal
     (gen/fmap (fn [[integral fractional]]
                 (BigDecimal. (str integral "." fractional)))
               (gen/tuple gen/small-integer gen/nat))))

#?(:clj
   (def gen-java-integer
     (gen/fmap #(Integer/valueOf ^int %) gen/small-integer)))

(def gen-big-int
  (gen/fmap #(* 1N %) gen/small-integer))

(def gen-float
  (gen/fmap #(float %) gen/small-integer))

(def gen-short
  (gen/fmap short gen/small-integer))

(def gen-var (gen/elements (vals (ns-interns #?(:clj 'clojure.core)
                                             #?(:cljs 'cljs.core)))))

(def query-gen
  (gen/one-of [(gen/return nil) gen/string-alphanumeric]))

#?(:clj
   (def gen-uri
     ;; well actually generates a URL, but oh well
     (let [scheme (gen/elements #{"http" "https"})
           authority (gen/elements #{"www.foo.com" "www.bar.com:80"})
           path (gen/one-of [(gen/return nil)
                             (gen/fmap #(str "/" %) gen/string-alphanumeric)])
           args-validation (fn [[_scheme authority path query fragment]]
                             (not (or ;; a URI with just a scheme is invalid
                                   (every? nil? (list authority path query fragment))
                                   ;; a URI with just a scheme and fragment is invalid
                                   (and (not (nil? fragment))
                                        (every? nil? (list authority path query))))))]

       (gen/fmap
        (fn [[scheme authority path query fragment]] (java.net.URI. scheme authority path query fragment))
        (gen/such-that
         args-validation
         (gen/tuple scheme authority path query-gen query-gen))))))

(def gen-scalar (gen/one-of [#?(:clj gen-java-integer)
                             gen/small-integer ;; really a long
                             gen-short
                             gen/string
                             gen/symbol
                             gen-float
                             (gen/double* {:NaN? false})
                             gen/symbol-ns
                             gen/keyword
                             gen/boolean
                             gen/ratio
                             gen/uuid
                             #?(:clj gen-uri)
                             #?(:clj gen-big-decimal)
                             gen-big-int
                             gen/char
                             #?(:clj gen/bytes)
                             ;; only include gen-var for clj (and not cljs) because
                             ;; in cljs every var satisfies Fn, so (fn? any-var)
                             ;; returns true, and we end up using the m/pred matcher
                             ;; instead of m/equals.
                             #?(:clj gen-var)]))

(defn gen-distinct-pair [element-generator]
  (gen/such-that (fn [[i j]] (not= i j)) (gen/tuple element-generator)))

(def gen-scalar-pair
  (gen-distinct-pair gen-scalar))

(defspec test-scalars
  (testing "scalar values act as equals matchers"
    (prop/for-all [i gen-scalar]
      (= (core/match i i)
         (core/match (m/equals i) i)))

    (prop/for-all [[i j] gen-scalar-pair]
      (= (core/match i j)
         (core/match (m/equals i) j)))))

(deftest test-maps
  (testing "act as equals matcher"
    (is (= (core/match (m/equals {:a (m/equals 10)}) {:a 10})
           (core/match (m/equals {:a 10}) {:a 10})
           (core/match {:a 10} {:a 10})))))

(deftest test-vectors
  (testing "vectors act as equals matchers"
    (is (= (core/match (m/equals [(m/equals 10)]) [10])
           (core/match (m/equals [10]) [10])
           (core/match [10] [10])))))

(deftest test-chunked-seq
  (testing "chunked sequences act as equals matchers"
    (is (core/match (seq [1 2 3]) [10]))))

(deftest test-lists
  (testing "lists act as equals matchers"
    (is (= (core/match (m/equals [(m/equals 10)]) [10])
           (core/match (m/equals '(10)) [10])
           (core/match '(10) [10])))))

(deftest test-nil
  (testing "`nil` is parsed as an equals"
    (is (= (core/match (m/equals nil) nil)
           (core/match nil nil)))))

#?(:clj
   (deftest test-classes
     (testing "java classes are parsed as an equals"
       (is
        (= (core/match (m/equals java.lang.String) java.lang.String)
           (core/match java.lang.String java.lang.String))))))

#?(:cljs
   (deftest test-types
     (testing "JS types are parsed as an equals"
       (is
        (= (core/match (m/equals js/Date) js/Date)
           (core/match js/Date js/Date))))))

#?(:clj
   (deftest test-object
     (let [an-object (Object.)
           another-object (RuntimeException.)]
       (testing "Objects default to equality matching"
         (is (= (core/match (m/equals an-object)
                            an-object)
                (core/match an-object
                            an-object)))
         (is (= (core/indicates-match? (core/match another-object (Object.)))
                (= another-object (Object.))))))))

#?(:clj
   (deftest mimic-matcher-macro
     (testing "mimic-matcher uses non-namespaced symbol for `-matcher-for`"
       (is (= '-matcher-for
              (->> (macroexpand-1 `(parser/mimic-matcher m/equals Integer))
                   butlast
                   last
                   first))))
     ;; this is a regression test for https://github.com/nubank/matcher-combinators/pull/104
     (testing "mimic-matcher uses non-namespaced symbol for `-match`"
       (is (= '-match
              (-> (macroexpand-1 `(parser/mimic-matcher m/equals Integer))
                  last
                  first))))))
