(ns nhss.core
  (:require [cognitect.transit :as t]
            [clojure.string    :as string]
            [clojure.java.io   :as io]))

(defn slurp-level [level-id]
  (slurp (format "levels/%s.txt" (name level-id))))

(defn features []
  {:down-stair \>
   :up-stair   \<
   :space      \·
   :boulder    \0
   :hole       \^
   :player     \@
   :empty      \space
   :wall       #{\┴ \┼ \┤ \┌ \├ \─ \└ \┐ \┬ \┘ \│}})

(defn level-floor [{:keys [cells]}]
  (mapv (fn [cells]
          (mapv (fn [cell]
                  (cond (or (= (:boulder (features)) cell)
                            (= (:hole (features)) cell))
                        (:space (features))

                        ((:wall (features)) cell)
                        (:empty (features))

                        (= (:player (features)) cell)
                        (:down-stair (features))

                        :default
                        cell))
                cells))
        cells))

(defn read-level [level-string]
  (let [[title info & lines] (string/split level-string #"\n")
        cells                (into [] (map (comp (partial apply vector) seq) lines))
        title                title
        info                 info
        level                {:cells        cells
                              :title        title
                              :info         info}
        level                (assoc level :floor (level-floor level))]
    level))

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
