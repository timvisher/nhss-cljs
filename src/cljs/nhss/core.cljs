(ns nhss.core
  (:require [clojure.string :as string]
            [cognitect.transit :as t]))

(defn standard-levels-string []
  ;; Fudge for now so I don't need to bring a server up that I can
  ;; request from.
  ;; See nhss/core.clj:write-transit-levels-to-string
  "[\"^ \",\"~:1a\",\"┌─┬────┐ ┌────┐\\n│<│@···└─┘····│\\n│^├┐·00····0··│\\n│^││··00│·0·0·│\\n│^││····│·····│\\n│^│└───┬┘0────┤\\n│^│    │······│\\n│^└────┘······│\\n│··^^^^0000···│\\n│··┌───┐······│\\n└──┘   └──────┘\",\"~:1b\",\"┌────┐  ┌───┐ \\n│····│  │···│ \\n│·0··└──┘·0·│ \\n│·0······0··│ \\n│··┌─┐@┌─┐0·│ \\n├──┴─┴─┼─┘·─┴┐\\n│··^^^<│·····│\\n│··┌───┤0····│\\n└┐^│   │·0···│\\n │^└───┘·0···│\\n │··^^^^0·0··│\\n │··┌────────┘\\n └──┘         \",\"~:2a\",\" ┌──┐          ┌─────────┐\\n┌┘·@└──────┐   │·········│\\n│··········│   │·········│\\n│·0┌───┐0─·│   │·········│\\n│··│···│·0·│   │····<····│\\n│·0·0····0─┤   │·········│\\n│·0··0··│··│   │·········│\\n│·────0·└┐·│   │·········│\\n│··0···0·│·└┐  │·········│\\n│·──┬0─···0·└──┴────────+┤\\n│···│··0─·0·^^^^^^^^^^^^·│\\n│··0······┌──────────────┘\\n└───┐··│··│               \\n    └──┴──┘               \",\"~:2b\",\"┌────┬────┐       ┌─────────┐\\n│····│····└─┐     │·········│\\n│··00│00···@│     │·········│\\n│·····0···┌─┘     │·········│\\n│····│····│       │····<····│\\n├─·──┼────┴┐      │·········│\\n│··0·│·····│      │·········│\\n│·00·│0·0·0│      │·········│\\n│··0·····0·│      │·········│\\n│·000│0··0·└───────────────+┤\\n│····│··0·0·^^^^^^^^^^^^^^^·│\\n└────┴──────────────────────┘\",\"~:3a\",\"  ┌─┬────┐          \\n┌─┘·│····│          \\n│···0····├─┬───────┐\\n│·─·00─00│·│·······│\\n│·00─······│·······│\\n│·─··0·│···│·······│\\n│····─0├─0─┤···<···│\\n│··00··0···│·······│\\n│·──···─···│·······│\\n│····─0┬───┤·······│\\n└─┐··0·└───┴──────+┤\\n  │··0@^^^^^^^^^^^·│\\n  └────────────────┘\",\"~:3b\",\"┌────────┬───┬─────┐\\n│········│···│·····│\\n│·00··─00│·─·│·····│\\n│··│·0·0·│00·│·····│\\n│─·│··─··│·─·│··<··│\\n│···│─·······│·····│\\n│···│·0·─···┌┤·····│\\n│·0·│0·│···┌┘│·····│\\n├─0·│··└───┴─┴────+┤\\n│··0····^^^^^^^^^^·│\\n│···│·@┌───────────┘\\n└───┴──┘            \",\"~:4a\",\"┌────────────────────────┐\\n│@······^^^^^^^^^^^^^^^^·│\\n│·······┌──────────────┐·│\\n└┬─────·└────┐         │·│\\n │···········│         │·│\\n │·0·0·0·0·0·│         │·│\\n┌┴──────·────┤         │·│\\n│···0·0··0·0·│         │·│\\n│···0········│         │·│\\n└┬───·──────┬┘   ┌─────┤·│\\n │··0·0·0···│  ┌─┤·····│·│\\n │·····0····│  │·+·····│·│\\n │·0·0···0·┌┘  ├─┤·····│·│\\n┌┴─────·─┬─┘   │·+·····+·│\\n│··0·····│     ├─┤·····├─┘\\n│········│     │·+·····│  \\n│···┌────┘     └─┤·····│  \\n└───┘            └─────┘  \",\"~:4b\",\"  ┌──────────────────────┐\\n  │··^^^^^^^^^^^^^^^^^^··│\\n  │··┌────────┬────────┐·│\\n┌─┴┐·│    ┌───┤        │·│\\n│··│0└┐  ┌┘···│        │·│\\n│·····├──┤·0··│        │·│\\n│·00··│··│··0·│        │·│\\n└┐··00│···00·┌┘        │·│\\n │0··0···│0··│   ┌─────┤·│\\n │·00·│··│··0│ ┌─┤·····│·│\\n │·0·0└──┤·0·│ │·+·····│·│\\n │·······│··┌┘ ├─┤·····│·│\\n └──┐·0··│·┌┘  │·+·····+·│\\n    └┬─·─┘·│   ├─┤·····├─┘\\n     │·0···│   │·+·····│  \\n     │@·│··│   └─┤·····│  \\n     └──┴──┘     └─────┘  \"]")

(defn standard-levels []
  (->> (standard-levels-string)
       (t/read (t/reader :json))))

(defn read-level [level-string]
  (let [lines (string/split level-string #"\n")
        cells (map (comp (partial apply vector) seq) lines)]
    (into [] cells)))

(defn get-position-string [level position]
  (let [[x y] position]
    (nth (nth level y) x)))

(defn position= [level position position-string]
  (= (get-position-string level position) position-string))

(defn player-position [level]
  (let [height (count level)
        width (count (first level))
        positions (->> (map (fn [y]
                          (map (fn [x]
                                 [x,y])
                               (range width)))
                            (range height))
                       (apply concat)
                       sort)]
    (loop [[position & next-positions] positions]
      (if (position= level position "@")
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
    (map (partial get-position-string level) diagonal-neighbor-positions)))

;;; Bug
;; (-> (:1a (standard-levels))
;;     (maybe-transform-level ,,, [3 1] :e)
;;     (maybe-transform-level ,,, [4 1] :e)
;;     print-level!)
;; (-> (:1a (standard-levels))
;;     (maybe-transform-level ,,, [3 1] :e)
;;     (maybe-transform-level ,,, [4 1] :e)
;;     (maybe-transform-level ,,, [5 1] :se)
;;     print-level!)

(defn legal-transformation? [level start-position direction target-position]
  (let [target-position-string (get-position-string level target-position)
        start-position-string (get-position-string level start-position)]
    (or (and (some (partial = direction) [:ne :se :sw :nw]) ; This cannot be an or
             (some (partial = "·") (diagonal-path-neighbor-strings level start-position target-position)))
        (or (and (= "@" start-position-string)
                 (= "·" target-position-string))
            (and (some (partial = direction) [:n :s :e :w])
                  (= "0" start-position-string)
                  (or (= "^" target-position-string)
                      (= "·" target-position-string)))))))


(defn transform-level [level start-position target-position]
  (let [start-position-string (get-position-string level start-position)
        new-level (assoc-in level (reverse target-position) (get-position-string level start-position))
        new-level (assoc-in new-level (reverse start-position) "·")]
    new-level))

(defn row-column-ids []
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn print-level!
  "Convenience function for printing a level"
  [level]
  (println " " (string/join (take (count (first level)) (row-column-ids))))
  (doseq [[row-id line] (map (fn [row-id line]
                               [row-id line])
                             (row-column-ids)
                             level)]
    (println row-id (string/join line))))

(defn maybe-transform-level [level start-position direction]
  (let [target-position (to-target-position direction start-position)]
    (if (legal-transformation? level start-position direction target-position)
      (transform-level level start-position target-position)
      (let [second-target-position (to-target-position direction target-position)]
        (if (legal-transformation? level target-position direction second-target-position)
          (transform-level (transform-level level target-position second-target-position) start-position target-position))))))
