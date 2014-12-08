(ns nhss.core
  (:require [clojure.string :as string]
            [cognitect.transit :as t]
            [nhss.levels :as levels]))

(defn level-features []
  {:down-stair      ">"
   :space           "·"
   :boulder         "0"
   :hole            "^"
   :player          "@"})

(defn get-position-string [level position]
  (let [[x y] position]
    (nth (nth (:cells level) y) x)))

(defn position= [level position position-string]
  (= (get-position-string level position) position-string))

(defn player-position [level]
  (let [height (count (:cells level))
        width (count (first (:cells level)))
        positions (->> (map (fn [y]
                              (map (fn [x]
                                     [x,y])
                                   (range width)))
                            (range height))
                       (apply concat)
                       sort)]
    (loop [[position & next-positions] positions]
      (if (position= level position (:player (level-features)))
        position
        (recur next-positions)))))

(defn transformations []
  {:n  [ 0 -1]
   :ne [ 1 -1]
   :e  [ 1  0]
   :se [ 1  1]
   :s  [ 0  1]
   :sw [-1  1]
   :w  [-1  0]
   :nw [-1 -1]})

(defn apply-position-diff [[x-diff y-diff :as position-diff] [x y :as position]]
  [(+ x x-diff) (+ y y-diff)])

(defn to-target-position [direction current-position]
  (apply-position-diff (direction (transformations)) current-position))

(defn position-diff [[p1-x p1-y] [p2-x p2-y]]
  [(- p2-x p1-x) (- p2-y p1-y)])

(defn diagonal-diff? [start-position target-position]
  (some (partial = (position-diff start-position target-position))
        (vals (select-keys (transformations) [:ne :se :sw :nw]))))

(defn get-diagonal-path-neighbors [start-position target-position]
  {:pre [(diagonal-diff? start-position target-position)]}
  (let [position-diff (position-diff start-position target-position)
        position-diffs [(assoc-in position-diff [0] 0)
                        (assoc-in position-diff [1] 0)]]
    (map apply-position-diff position-diffs (repeat start-position))))

(defn diagonal-path-neighbor-strings [level start-position target-position]
  {:pre [(diagonal-diff? start-position target-position)]}
  (let [diagonal-neighbor-positions (get-diagonal-path-neighbors start-position target-position)]
    (sort (map (partial get-position-string level) diagonal-neighbor-positions))))

(defn trace! [o]
  (println "TRACE: " (pr-str o))
  o)

(defn transformations-whitelist []
  {:cardinal      {(:player (level-features)) {(:space (level-features)) (:player (level-features))}
                   (:boulder (level-features)) {(:space (level-features)) (:boulder (level-features))
                                                (:hole (level-features)) (:boulder (level-features))}}
   :intercardinal {(:player (level-features))
                   {(:space (level-features))
                    {(:space (level-features))
                     (:player (level-features))}}}})

(defn direction-kind [direction]
  (let [cardinal (select-keys (transformations) [:n :s :e :w])]
    (if (contains? cardinal direction)
      :cardinal
      :intercardinal)))

(defn some-diagonal-path-neighbor-space [level start-position target-position]
  {:pre [(diagonal-diff? start-position target-position)]}
  (->> (diagonal-path-neighbor-strings level start-position target-position)
       (filter (partial = (:space (level-features))))
       first))

;;; TODO This functions a mess. :(
(defn legal-transformation? [level start-position direction target-position]
  {:pre [(= (direction (transformations))
            (position-diff start-position target-position))]}
  (let [target-position-string (get-position-string level target-position)
        start-position-string  (get-position-string level start-position)]
    (if (= :cardinal (direction-kind direction))
      (get-in (transformations-whitelist)
              [(direction-kind direction)
               start-position-string
               target-position-string])
      (or (get-in (transformations-whitelist)
                  [(direction-kind direction)
                   start-position-string
                   (some-diagonal-path-neighbor-space level start-position target-position)
                   target-position-string])
          ))))

(defn set-position-string [level position position-string]
  (update-in level [:cells]
             (fn [cells]
               (assoc-in cells
                         (reverse position)
                         position-string))))

;;; TODO this needs to be expanded. currently doesn't handle boulder
;;; dropping down a hole
(defn transform-level
  "Assumes caller has already checked validity of transformation with
legal-transformation?"
  [level start-position target-position]
  ;; {:pre [(= (:space (level-features))
  ;;           (get-position-string level target-position))]}
  (let [start-position-string      (get-position-string level start-position)
        new-start-position-string  (:covered-cell level)
        target-position-string     (get-position-string level target-position)
        new-target-position-string start-position-string
        new-covered-cell           target-position-string
        new-level                  (set-position-string level
                                                        target-position
                                                        new-target-position-string)
        new-level                  (set-position-string new-level
                                                        start-position
                                                        new-start-position-string)
        new-level                  (assoc new-level :covered-cell new-covered-cell)]
    new-level))

(defn row-column-ids []
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn print-level!
  "Convenience function for printing a level"
  [level]
  (println " " (string/join (take (count (first (:cells level))) (row-column-ids))))
  (doseq [[row-id line] (map (fn [row-id line]
                               [row-id line])
                             (row-column-ids)
                             (:cells level))]
    (println row-id (string/join line))))

(defn maybe-transform-level [level start-position direction]
  {:pre [(let [direction-whitelist ((direction-kind direction) (transformations-whitelist))
               start-position-string (get-position-string level start-position)]
           (contains? direction-whitelist start-position-string))]}
  (let [target-position (to-target-position direction start-position)]
    (if (legal-transformation? level start-position direction target-position)
      (transform-level level start-position target-position)
      (let [second-target-position (to-target-position direction target-position)]
        (if (legal-transformation? level target-position direction second-target-position)
          (transform-level (transform-level level target-position second-target-position) start-position target-position))))))
