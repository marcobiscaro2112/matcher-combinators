(ns matcher-combinators.parser
  (:require [matcher-combinators.core :as core]
            [matcher-combinators.matchers :as matchers])
  #?(:cljs (:import goog.Uri)
     :clj  (:import (clojure.lang IPersistentMap)
                    (java.util.regex Pattern))))

#?(:cljs
(extend-protocol
  core/Matcher

  goog.Uri
  (-matcher-for
    ([_] matchers/cljs-uri))

  js/Set
  ;; for some reason, js/Set does not satisfy ISet, so we need to use an explicit matcher for it,
  ;; otherwise it would fall into the default matchers/equals case
  (-matcher-for
    ([_] matchers/set-embeds))

  default
  (-match [this actual]
    (core/match ((core/-matcher-for this) this) actual))
  (-matcher-for
    ([this]
     (cond
       (fn? this)
       matchers/pred

       (map? this)
       matchers/embeds

       (regexp? this)
       matchers/regex

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
