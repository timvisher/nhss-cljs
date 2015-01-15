(ns nhss.transformation
  (:require-macros [cljs.core.async.macros :as am])
  (:require [clojure.string    :as string]
            [cognitect.transit :as t]
            [nhss.levels       :as levels]
            [cljs.core.async   :as a]

            [nhss.util :refer [js-trace! trace! print-level! print-cells!]]))

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

(defn direction-kind [direction]
  (let [cardinal (select-keys (directional-position-diffs) [:n :s :e :w])]
    (if (contains? cardinal direction)
      :cardinal
      :intercardinal)))

(defn floor-space? [level position]
  (= (levels/get-cells-position-string (:floor level) position)
     (levels/get-position-string level position)))

(defn diagonal-room? [level start-position target-position]
  {:pre [(diagonal-diff? start-position target-position)]}
  (let [diagonal-neighbor-positions (get-diagonal-path-neighbors start-position target-position)]
    (->> diagonal-neighbor-positions
         (filter (partial floor-space? level))
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
    (let [target-position-string       (levels/get-position-string level target-position)
          target-floor-position-string (levels/get-cells-position-string (:floor level) target-position)
          start-position-string        (levels/get-position-string level start-position)]
      ;; must start with player or boulder
      (if (or (= (:player levels/features) start-position-string)
              (= (:boulder levels/features) start-position-string))
       (if (= :cardinal (direction-kind direction))
         ;; cardinal
         (if (not= target-position-string target-floor-position-string)
           ;; only possible valid move is boulder to hole
           (and (= (:boulder levels/features) start-position-string)
                (= (:hole levels/features) target-position-string))
           :floor)

         ;; intercardinal
         (and (= (:player levels/features) start-position-string)
              (diagonal-room? level start-position target-position)
              (= target-position-string target-floor-position-string)))))))

(defn set-position-string [level position position-string]
  (update-in level [:cells]
             (fn [cells]
               (assoc-in cells
                         (reverse position)
                         position-string))))

(defn has-down-stair? [level-string]
  (some (partial = (:down-stair levels/features)) level-string))

(defn has-up-stair? [level-string]
  (some (partial = (:up-stair levels/features)) level-string))

(defn covered-cell [level position]
  (levels/get-cells-position-string (:floor level) position))

(defn new-target-position-string [level start-position target-position]
  (let [start-string (levels/get-position-string level start-position)
        target-string (levels/get-position-string level target-position)]
    (if (and (= (:boulder levels/features) start-string)
             (= (:hole levels/features) target-string))
      (levels/get-cells-position-string (:floor level) target-position)
      start-string)))

(defn set-player-position [level position-1 position-2]
  (let [[player-position?] (filterv (fn [pos]
                                      (= (:player levels/features)
                                         (levels/get-position-string level pos)))
                                    [position-1 position-2])]
    (if player-position?
      (assoc level :player-position player-position?)
      level)))

(defn transform-level
  "Assumes caller has already checked validity of transformation with
legal-transformation?"
  [level start-position target-position]
  (let [new-start-position-string  (covered-cell level start-position)
        new-target-position-string (new-target-position-string level start-position target-position)
        new-level                  (set-position-string level
                                                        target-position
                                                        new-target-position-string)
        new-level                  (set-position-string new-level
                                                        start-position
                                                        new-start-position-string)
        new-level                  (set-player-position new-level
                                                        start-position
                                                        target-position)]
    new-level))

(defn maybe-transform-level [level start-position direction]
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
      (let [new-level (maybe-transform-level level (:player-position level) direction)]
        (if new-level
          (a/>! output-chan {:level new-level})
          (a/>! output-chan {:level level :errors []}))))
    (recur)))
