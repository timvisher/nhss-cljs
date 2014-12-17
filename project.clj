(defproject nhss "0.2.0-SNAPSHOT"
  :description  "An implementation of nhss in clojurescript"
  :url          "https://github.com/timvisher/nhss-cljs"
  :license      {:name "CC-BY"
                 :url "https://creativecommons.org/licenses/by/3.0/us/"}
  :dependencies [[org.clojure/clojure        "1.6.0"]
                 [org.clojure/clojurescript  "0.0-2411"]
                 [com.cognitect/transit-cljs "0.8.192"]
                 [org.clojure/core.async     "0.1.346.0-17112a-alpha"]]
  :source-paths ["src/clj" "src/cljs"]
  :profiles     {:dev     {:plugins      [[com.cemerick/austin       "0.1.5"]
                                          [lein-cljsbuild            "1.0.3"]]
                           :dependencies [[com.cognitect/transit-clj "0.8.259"]]}}
  :cljsbuild    {:builds [{:source-paths ["src/cljs"]
                             :compiler     {:optimizations :whitespace
                                            :pretty-print  true}}]})
