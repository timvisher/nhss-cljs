(ns nhss.core
  (:require [cognitect.transit :as t]
            [clojure.string    :as string]
            [clojure.java.io   :as io]))

(defn slurp-level [level-id]
  (slurp (format "levels/%s.txt" (name level-id))))

(defn read-level [level-string]
  (let [[title info & lines] (string/split level-string #"\n")
        cells (into [] (map (comp (partial apply vector) seq) lines))
        covered-cell ">"]
    {:cells        cells
     :title        title
     :info         info
     :covered-cell covered-cell}))

(defn transit-write-to-out [levels out]
  (t/write (t/writer out :json) levels))

(defn make-standard-levels []
  (let [standard-level-ids [:1a :1b :2a :2b :3a :3b :4a :4b]]
    (zipmap standard-level-ids
            (map (comp read-level slurp-level) standard-level-ids))))

(defn write-transit-levels-to-file []
  (with-open [out (io/output-stream "levels.json")]
    (transit-write-to-out (make-standard-levels) out)))

(defn write-transit-levels-to-string []
  (let [out (java.io.ByteArrayOutputStream. 4096)]
    (transit-write-to-out (make-standard-levels) out)
    (str out)))
