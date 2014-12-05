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

(defn to-target-position [direction current-position]
  (let [[x y] current-position
        [x-diff y-diff] (direction (transformations))]
    [(+ x x-diff) (+ y y-diff)]))

(defn legal-transformation? [level start-position direction target-position]
  true)

(defn transform-level [level start-position target-position]
  (let [start-position-string (get-position-string level start-position)
        new-level (assoc-in level (reverse target-position) (get-position-string level start-position))
        new-level (assoc-in new-level (reverse start-position) "·")]
    new-level))

(defn print-level!
  "Convenience function for printing a level"
  [level]
  (doseq [line level]
    (println (string/join line))))

(defn maybe-transform-level [level start-position direction]
  (let [target-position (to-target-position direction start-position)]
    (if (legal-transformation? level start-position direction target-position)
      (transform-level level start-position target-position)
      (let [second-target-position (to-target-position direction target-position)]
        (if (legal-transformation? level target-position direction second-target-position)
          (transform-level (transform-level level target-position second-target-position) start-position target-position))))))
