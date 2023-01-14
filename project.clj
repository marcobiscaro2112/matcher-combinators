(defproject nubank/matcher-combinators "3.7.2"
  :description "Library for creating matcher combinator to compare nested data structures"
  :url "https://github.com/nubank/matcher-combinators"
  :license {:name "Apache License, Version 2.0"}

  :repositories [["publish" {:url "https://clojars.org/repo"
                             :username :env/clojars_username
                             :password :env/clojars_passwd
                             :sign-releases false}]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/spec.alpha "0.3.218"]
                 [org.clojure/math.combinatorics "0.1.6"]
                 [midje "1.10.9" :exclusions [org.clojure/clojure]]]

  :source-paths ["src/clj" "src/cljc"]
  :test-paths   ["test/clj" "test/cljc"]

  :profiles {:dev {:plugins [[com.github.clojure-lsp/lein-clojure-lsp "1.3.17"]
                             [lein-project-version "0.1.0"]
                             [lein-midje "3.2.2"]
                             [lein-cljsbuild "1.1.8"]
                             [lein-ancient "0.7.0"]
                             [lein-doo "0.1.11"]]
                   :dependencies [[org.clojure/test.check "1.1.1"]
                                  [org.clojure/clojurescript "1.11.60"]
                                  [org.clojure/core.rrb-vector "0.1.2"]
                                  [orchestra "2021.01.01-1"]]
                   :source-paths ["dev"]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}

  :aliases {"format"          ["clojure-lsp" "format" "--dry"]
            "format-fix"      ["clojure-lsp" "format"]
            "clean-ns"        ["clojure-lsp" "clean-ns" "--dry"]
            "clean-ns-fix"    ["clojure-lsp" "clean-ns"]
            "lint"            ["do" ["format"] ["clean-ns"]]
            "lint-fix"        ["do" ["format-fix"] ["clean-ns-fix"]]
            "test-phantom"    ["doo" "phantom" "test"]
            "test-advanced"   ["doo" "phantom" "advanced-test"]
            "test-node-watch" ["doo" "node" "node-test"]
            "test-node"       ["doo" "node" "node-test" "once"]}
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src/cljs" "src/cljc" "test/cljs" "test/cljc"]
                        :compiler {:output-to "target/out/test.js"
                                   :output-dir "target/out"
                                   :main matcher-combinators.doo-runner
                                   :optimizations :none}}
                       ;; Node.js requires :target :nodejs, hence the separate
                       ;; build configuration.
                       {:id "node-test"
                        :source-paths ["src/cljs" "src/cljc" "test/cljs" "test/cljc"]
                        :compiler {:output-to "target/node_out/test.js"
                                   :output-dir "target/node_out"
                                   :main matcher-combinators.doo-runner
                                   :optimizations :none
                                   :target :nodejs}}]})
