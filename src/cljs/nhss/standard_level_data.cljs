(ns nhss.standard-level-data
  (:require [nhss.levels    :as levels]
            [clojure.string :as string]
            [nhss.util      :refer [trace! print-cells!]]))

(defn level->cells [level]
  (into [] (map (comp (partial apply vector) seq) level)))

(defn cells->floor [cells]
  (mapv (fn [cells]
          (mapv (fn [cell]
                  (cond (or (= (:boulder levels/features) cell)
                            (= (:hole levels/features) cell))
                        (:space levels/features)

                        ((:wall levels/features) cell)
                        (:empty levels/features)

                        (= (:player levels/features) cell)
                        (:down-stair levels/features)

                        :default
                        cell))
                cells))
        cells))

(defn title->id [title]
  (keyword (last (string/split title #" "))))

(defn position= [level position position-string]
  (= (levels/get-position-string level position) position-string))

(defn level->player-position [level]
  (let [height    (count (:cells level))
        width     (count (first (:cells level)))
        positions (->> (map (fn [y]
                              (map (fn [x]
                                     [x,y])
                                   (range width)))
                            (range height))
                       (apply concat)
                       sort)]
    (loop [[position & next-positions] positions]
      (if (position= level position (:player levels/features))
        position
        (recur next-positions)))))

(defn apply-diff [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn potential-neighbors [node]
  (map (partial apply-diff node) [[-1 -1] [0 -1] [1 -1]
                                  [-1  0]        [1  0]
                                  [-1  1] [0  1] [1  1]]))

(defn floor? [floor node]
  (not= (:empty levels/features)
        (levels/get-cells-position-string floor node)))

(defn valid-position? [cells [x y :as position]]
  (and (every? pos? position)
       (< x (count (first cells)))
       (< y (count cells))))

(defn neighbors [floor node]
  {:pre [(floor? floor node)]}
  (filterv (partial floor? floor) (filter (partial valid-position? floor) (potential-neighbors node))))

(defn first-node [floor]
  (let [positions (for [y (range (count floor)) x (range (count (first floor)))] [x y])]
    (first (filter (partial floor? floor) positions))))

(defn floor->graph [floor]
  (loop [node-queue    #{(first-node floor)}
         visited-nodes #{}
         graph         {}]
    (if (empty? node-queue)
      graph
      (let [current-node   (first node-queue)
            next-nodes     (apply hash-set (next node-queue))
            node-neighbors (neighbors floor (first node-queue))]
        (recur (into next-nodes (filter (complement visited-nodes) node-neighbors))
               (conj visited-nodes current-node)
               (assoc graph current-node node-neighbors))))))

(defn deflevel [title info & level]
  (let [cells       (level->cells level)
        floor       (cells->floor cells)
        floor-graph (floor->graph floor)
        level       {:id          (title->id title)
                     :title       title
                     :info        info
                     :cells       cells
                     :floor       floor
                     :floor-graph floor-graph}]
    (assoc level :player-position (level->player-position level))))

(def levels
  {(keyword "NetHack Sokoban 1a")
   (deflevel
     "NetHack Sokoban 1a"
     ""
     "┌─┬────┐ ┌────┐"
     "│<│@···└─┘····│"
     "│^├┐·00····0··│"
     "│^││··00│·0·0·│"
     "│^││····│·····│"
     "│^│└───┬┘0────┤"
     "│^│    │······│"
     "│^└────┘······│"
     "│··^^^^0000···│"
     "│??┌───┐······│"
     "└──┘   └──────┘")

   (keyword "NetHack Sokoban 1b")
   (deflevel
     "NetHack Sokoban 1b"
     ""
     "┌────┐  ┌───┐ "
     "│····│  │···│ "
     "│·0··└──┘·0·│ "
     "│·0······0··│ "
     "│··┌─┐@┌─┐0·│ "
     "├──┴─┴─┼─┘·─┴┐"
     "│··^^^<│·····│"
     "│··┌───┤0····│"
     "└┐^│   │·0···│"
     " │^└───┘·0···│"
     " │··^^^^0·0··│"
     " │??┌────────┘"
     " └──┘         ")

   (keyword "NetHack Sokoban 2a")
   (deflevel
     "NetHack Sokoban 2a"
     ""
     " ┌──┐          ┌─────────┐"
     "┌┘·@└──────┐   │·········│"
     "│··········│   │·········│"
     "│·0┌───┐0─·│   │·········│"
     "│··│···│·0·│   │····<····│"
     "│·0·0····0─┤   │·········│"
     "│·0··0··│··│   │·········│"
     "│·────0·└┐·│   │·········│"
     "│··0···0·│·└┐  │·········│"
     "│·──┬0─···0·└──┴────────+┤"
     "│···│··0─·0·^^^^^^^^^^^^·│"
     "│··0······┌──────────────┘"
     "└───┐··│··│               "
     "    └──┴──┘               ")

   (keyword "NetHack Sokoban 2b")
   (deflevel
     "NetHack Sokoban 2b"
     ""
     "┌────┬────┐       ┌─────────┐"
     "│····│····└─┐     │·········│"
     "│··00│00···@│     │·········│"
     "│·····0···┌─┘     │·········│"
     "│····│····│       │····<····│"
     "├─·──┼────┴┐      │·········│"
     "│··0·│·····│      │·········│"
     "│·00·│0·0·0│      │·········│"
     "│··0·····0·│      │·········│"
     "│·000│0··0·└───────────────+┤"
     "│····│··0·0·^^^^^^^^^^^^^^^·│"
     "└────┴──────────────────────┘")

   (keyword "NetHack Sokoban 3a")
   (deflevel
     "NetHack Sokoban 3a"
     ""
     "  ┌─┬────┐          "
     "┌─┘·│····│          "
     "│···0····├─┬───────┐"
     "│·─·00─00│·│·······│"
     "│·00─······│·······│"
     "│·─··0·│···│·······│"
     "│····─0├─0─┤···<···│"
     "│··00··0···│·······│"
     "│·──···─···│·······│"
     "│····─0┬───┤·······│"
     "└─┐··0·└───┴──────+┤"
     "  │··0@^^^^^^^^^^^·│"
     "  └────────────────┘")

   (keyword "NetHack Sokoban 3b")
   (deflevel
     "NetHack Sokoban 3b"
     ""
     "┌────────┬───┬─────┐"
     "│········│···│·····│"
     "│·00··─00│·─·│·····│"
     "│··│·0·0·│00·│·····│"
     "│─·│··─··│·─·│··<··│"
     "│···│─·······│·····│"
     "│···│·0·─···┌┤·····│"
     "│·0·│0·│···┌┘│·····│"
     "├─0·│··└───┴─┴────+┤"
     "│··0····^^^^^^^^^^·│"
     "│···│·@┌───────────┘"
     "└───┴──┘            ")

   (keyword "NetHack Sokoban 4a")
   (deflevel
     "NetHack Sokoban 4a"
     "With a Bag of Holding"
     "┌────────────────────────┐"
     "│@······^^^^^^^^^^^^^^^^·│"
     "│·······┌──────────────┐·│"
     "└┬─────·└────┐         │·│"
     " │···········│         │·│"
     " │·0·0·0·0·0·│         │·│"
     "┌┴──────·────┤         │·│"
     "│···0·0··0·0·│         │·│"
     "│···0········│         │·│"
     "└┬───·──────┬┘   ┌─────┤·│"
     " │··0·0·0···│  ┌─┤·····│·│"
     " │·····0····│  │·+·····│·│"
     " │·0·0···0·┌┘  ├─┤·····│·│"
     "┌┴─────·─┬─┘   │·+·····+·│"
     "│··0·····│     ├─┤·····├─┘"
     "│········│     │·+·····│  "
     "│···┌────┘     └─┤·····│  "
     "└───┘            └─────┘  ")

   (keyword "NetHack Sokoban 4b")
   (deflevel
     "NetHack Sokoban 4b"
     "With an Amulet of Reflection"
     "  ┌──────────────────────┐"
     "  │··^^^^^^^^^^^^^^^^^^··│"
     "  │··┌────────┬────────┐·│"
     "┌─┴┐·│    ┌───┤        │·│"
     "│··│0└┐  ┌┘···│        │·│"
     "│·····├──┤·0··│        │·│"
     "│·00··│··│··0·│        │·│"
     "└┐··00│···00·┌┘        │·│"
     " │0··0···│0··│   ┌─────┤·│"
     " │·00·│··│··0│ ┌─┤·····│·│"
     " │·0·0└──┤·0·│ │·+·····│·│"
     " │·······│··┌┘ ├─┤·····│·│"
     " └──┐·0··│·┌┘  │·+·····+·│"
     "    └┬─·─┘·│   ├─┤·····├─┘"
     "     │·0···│   │·+·····│  "
     "     │@·│··│   └─┤·····│  "
     "     └──┴──┘     └─────┘  ")

   (keyword "Evil Variant 2a")
   (deflevel
     "Evil Variant 2a"
     ""
     " ┌──┐          ┌─────────┐"
     "┌┘·@└──────┐   │·········│"
     "│··········│   │·········│"
     "│·0┌───┐0─·│   │·········│"
     "│··│···│·0·│   │····<····│"
     "│·0·0····0─┤   │·········│"
     "│·0··0··│··│   │·········│"
     "│·────0·└┐·│   │·········│"
     "│··0···0·│·└┐  │·········│"
     "│·──┐0─···0^└──┴────────+┤"
     "│···│··0─·^^^^^^^^^^^^^^·│"
     "│··0······┌──────────────┘"
     "└───┐··│··│               "
     "    └──┴──┘               ")

   (keyword "Evil Variant 2b")
   (deflevel
     "Evil Variant 2b"
     ""
     " ┌──┐          ┌─────────┐"
     "┌┘·@└──────┐   │·········│"
     "│··········│   │·········│"
     "│·0┌───┐0─·│   │·········│"
     "│··│···│·0·│   │····<····│"
     "│·0·0····0─┤   │·········│"
     "│·0··0··│··│   │·········│"
     "│·───00·└┐·│   │·········│"
     "│··0···0·│·└┐  │·········│"
     "│·──┐0─··0·^└──┴────────+┤"
     "│···│··0─·^^^^^^^^^^^^^^·│"
     "│··0······┌──────────────┘"
     "└───┐··│··│               "
     "    └──┴──┘               ")})
