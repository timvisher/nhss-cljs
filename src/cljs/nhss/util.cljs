(ns nhss.util)

(defn trace! [o]
  (println "TRACE: " (pr-str o))
  o)

(defn js-trace! [o]
  (.log js/console "TRACE: " (pr-str o))
  o)
