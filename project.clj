(defproject nhss "0.5.0"
  :description  "An implementation of nhss in clojurescript"
  :url          "https://github.com/timvisher/nhss-cljs"
  :license      {:name "CC-BY"
                 :url "https://creativecommons.org/licenses/by/3.0/us/"}
  :dependencies [[org.clojure/clojure        "1.6.0"]
                 [org.clojure/clojurescript  "0.0-2655"]
                 [com.cognitect/transit-cljs "0.8.192"]
                 [om                         "0.8.0-rc1"]
                 [cljs-ajax                  "0.3.3"]
                 [org.clojure/core.async     "0.1.346.0-17112a-alpha"]]
  :source-paths ["src/clj" "src/cljs"]
  :profiles     {:dev     {:plugins      [[com.cemerick/austin       "0.1.5"]
                                          [lein-cljsbuild            "1.0.4"]]
                           :dependencies [[com.cognitect/transit-clj "0.8.259"]
                                          [com.cemerick/piggieback   "0.1.3"]
                                          [weasel                    "0.4.2"]]}}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :cljsbuild    {:builds [{:source-paths ["src/cljs"]
                           :compiler     {:optimizations :whitespace
                                          :pretty-print  true}}]})
