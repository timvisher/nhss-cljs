(ns nhss.standard-level-data
  (:require [nhss.levels    :as levels]
            [clojure.string :as string]))

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

(defn deflevel [title info & level]
  (let [cells (level->cells level)
        floor (cells->floor cells)
        level {:id    (title->id title)
               :title title
               :info  info
               :cells cells
               :floor floor}]
    (assoc level :player-position (level->player-position level))))

(def levels
  {:1a (deflevel
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
   :1b (deflevel
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
   :2a (deflevel
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
   :2b (deflevel
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
   :3a (deflevel
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
   :3b (deflevel
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
   :4a (deflevel
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
   :4b (deflevel
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
         "     └──┴──┘     └─────┘  ")})
