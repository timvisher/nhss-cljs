(ns nhss.util
  (:require [clojure.string :as string]))

(defn trace! [o]
  (println "TRACE: " (pr-str o))
  o)

(defn js-trace! [o]
  (.log js/console "TRACE: " (pr-str o))
  o)

(defn row-column-ids []
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn print-cells!
  "Convenience function for printing cells."
  [cells]
  (println " " (string/join (take (count (first cells)) (row-column-ids))))
  (doseq [[row-id line] (map (fn [row-id line]
                               [row-id line])
                             (row-column-ids)
                             cells)]
    (println row-id (string/join line) row-id))
  (println " " (string/join (take (count (first cells)) (row-column-ids)))))

(defn print-level!
  "Convenience function for printing a level"
  [level]
  (print-cells! (:cells level)))
