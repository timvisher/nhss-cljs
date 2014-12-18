(ns nhss.transformation
  (:require-macros [cljs.core.async.macros :as am])
  (:require [clojure.string    :as string]
            [cognitect.transit :as t]
            [nhss.levels       :as levels]
            [cljs.core.async   :as a]

            [nhss.util :refer [js-trace! trace! print-level!]]))

(defn get-cells-position-string [cells position]
  (let [[x y] position]
    (nth (nth cells y) x)))

(defn get-position-string [level position]
  (get-cells-position-string (:cells level) position))

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
      (if (position= level position (:player (levels/features)))
        position
        (recur next-positions)))))

(defn directional-position-diffs []
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
  (apply-position-diff (direction (directional-position-diffs)) current-position))

(defn position-diff [[p1-x p1-y] [p2-x p2-y]]
  [(- p2-x p1-x) (- p2-y p1-y)])

(defn diagonal-diff? [start-position target-position]
  (some (partial = (position-diff start-position target-position))
        (vals (select-keys (directional-position-diffs) [:ne :se :sw :nw]))))

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

(defn transformations-whitelist []
  {:cardinal      {(:player (levels/features)) {(:space (levels/features)) (:player (levels/features))}
                   (:boulder (levels/features)) {(:space (levels/features)) (:boulder (levels/features))
                                                (:hole (levels/features)) (:boulder (levels/features))}}
   :intercardinal {(:player (levels/features))
                   {true
                    {(:space (levels/features))
                     (:player (levels/features))}}}})

(defn direction-kind [direction]
  (let [cardinal (select-keys (directional-position-diffs) [:n :s :e :w])]
    (if (contains? cardinal direction)
      :cardinal
      :intercardinal)))

(defn diagonal-room? [level start-position target-position]
  {:pre [(diagonal-diff? start-position target-position)]}
  (let [diagonal-room-chars #{(:space (levels/features))
                              (:up-stair (levels/features))
                              (:down-stair (levels/features))}]
    (->> (diagonal-path-neighbor-strings level start-position target-position)
         (filter diagonal-room-chars)
         count
         (< 0))))

(defn valid-position? [{:keys [cells]} [x y :as position]]
  (and (every? pos? position)
       (< x (count (first cells)))
       (< y (count cells))))

;;; TODO This function's a mess. :(
(defn legal-transformation? [level start-position direction target-position]
  {:pre [(= (direction (directional-position-diffs))
            (position-diff start-position target-position))]}
  (if (and (valid-position? level start-position)
           (valid-position? level target-position))
      (let [target-position-string (get-position-string level target-position)
            start-position-string  (get-position-string level start-position)]
        (if (= :cardinal (direction-kind direction))
          (get-in (transformations-whitelist)
                  [(direction-kind direction)
                   start-position-string
                   target-position-string])
          (get-in (transformations-whitelist)
                  [(direction-kind direction)
                   start-position-string
                   (diagonal-room? level start-position target-position)
                   target-position-string])))))

(defn set-position-string [level position position-string]
  (update-in level [:cells]
             (fn [cells]
               (assoc-in cells
                         (reverse position)
                         position-string))))

(defn has-down-stair? [level-string]
  (some (partial = (:down-stair (levels/features))) level-string))

(defn has-up-stair? [level-string]
  (some (partial = (:up-stair (levels/features))) level-string))

(defn covered-cell [level position]
  (get-cells-position-string (:floor level) position))

(defn new-target-position-string [level start-position target-position]
  (let [start-string (get-position-string level start-position)
        target-string (get-position-string level target-position)]
    (if (and (= (:boulder (levels/features)) start-string)
             (= (:hole (levels/features)) target-string))
      (get-cells-position-string (:floor level) target-position)
      start-string)))

(defn transform-level
  "Assumes caller has already checked validity of transformation with
legal-transformation?"
  [level start-position target-position]
  (let [start-position-string      (get-position-string level start-position)
        new-start-position-string  (covered-cell level start-position)
        target-position-string     (get-position-string level target-position)
        new-target-position-string (new-target-position-string level start-position target-position)
        new-covered-cell           target-position-string
        new-level                  (set-position-string level
                                                        target-position
                                                        new-target-position-string)
        new-level                  (set-position-string new-level
                                                        start-position
                                                        new-start-position-string)]
    new-level))

(defn maybe-transform-level [level start-position direction]
  {:pre [(let [direction-whitelist ((direction-kind direction) (transformations-whitelist))
               start-position-string (get-position-string level start-position)]
           (contains? direction-whitelist start-position-string))]}
  (let [target-position (to-target-position direction start-position)]
    (if (legal-transformation? level start-position direction target-position)
      (transform-level level start-position target-position)
      (let [second-target-position (to-target-position direction target-position)]
        (if (legal-transformation? level target-position direction second-target-position)
          (-> level
              (transform-level target-position second-target-position)
              (transform-level start-position target-position)))))))

(defn make-nhss-process
  "Takes proposed transformations of levels and outputs them or errors
they produced to output-chan.

input-chan should take messages of the form {:level l :direction d}.

output-chan will contain messages of the form {:level l :errors e}."
  [input-chan output-chan]
  (am/go-loop []
    (let [{:keys [level direction]} (a/<! input-chan)]
      (let [new-level (maybe-transform-level level (player-position level) direction)]
        (if new-level
          (a/>! output-chan {:level new-level})
          (a/>! output-chan {:level level :errors []}))))
    (recur)))
