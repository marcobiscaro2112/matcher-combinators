(ns matcher-combinators.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [matcher-combinators.cljs-example-test]
            [matcher-combinators.core-test]
            [matcher-combinators.matchers-test]
            [matcher-combinators.parser-test]
            [matcher-combinators.printer-test]
            [matcher-combinators.standalone-test]
            [matcher-combinators.test-test]))

(enable-console-print!)

(doo-tests 'matcher-combinators.cljs-example-test
           'matcher-combinators.core-test
           'matcher-combinators.matchers-test
           'matcher-combinators.parser-test
           'matcher-combinators.printer-test
           'matcher-combinators.standalone-test
           'matcher-combinators.test-test)
