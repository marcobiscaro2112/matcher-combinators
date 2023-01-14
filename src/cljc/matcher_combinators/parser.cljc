(ns matcher-combinators.parser
  (:require [matcher-combinators.core :as core]
            [matcher-combinators.matchers :as matchers])
  #?(:cljs (:import goog.Uri)
     :clj  (:import (clojure.lang IPersistentMap)
                    (java.util.regex Pattern))))

#?(:cljs
(extend-protocol
  core/Matcher

  function
  (-matcher-for
    ([_] matchers/pred))

  goog.Uri
  (-matcher-for
    ([_] matchers/cljs-uri))

  js/RegExp
  (-matcher-for
    ([_] matchers/regex))

  default
  (-match [this actual]
    (core/match ((core/-matcher-for this) this) actual))
  (-matcher-for
    ([this]
     (cond
       (satisfies? IMap this)
       matchers/embeds

       (or (satisfies? ISet this)
           ; why js/Set does not satisfy ISet is beyond me
           (= js/Set (type this)))
       matchers/set-equals

       ;; everything else uses equals by default
       :else
       matchers/equals))
    ([this t->m]
     (matchers/lookup-matcher this t->m)))))

#?(:clj (do
(defmacro mimic-matcher [matcher t]
  `(extend-type ~t
     core/Matcher
     (~'-matcher-for
      ([this#] ~matcher)
      ([this# t->m#] (matchers/lookup-matcher this# t->m#)))
     (~'-match [this# actual#]
      (core/match (~matcher this#) actual#))))

;; default for most objects
(mimic-matcher matchers/equals Object)

;; nil is a special case
(mimic-matcher matchers/equals nil)

;; regex
(mimic-matcher matchers/regex Pattern)

;; collections
(mimic-matcher matchers/embeds IPersistentMap)

;; functions are special, too
(extend-type clojure.lang.Fn
  core/Matcher
  (-matcher-for
    ([_] matchers/pred)
    ([_ _] matchers/pred))
  (-match [this actual]
    (core/match (matchers/pred this) actual)))))
