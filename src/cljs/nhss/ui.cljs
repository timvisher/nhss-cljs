(ns nhss.ui
  (:require [goog.events     :as events]
            [cljs.core.async :as a]
            [clojure.string  :as string]
            [nhss.levels     :as levels]

            [nhss.util :refer [js-trace!]])
  (:import [goog.events KeyCodes]))

(defn movement-keys []
  {KeyCodes/H              :w
   KeyCodes/J              :s
   KeyCodes/K              :n
   KeyCodes/L              :e
   KeyCodes/Y              :nw
   KeyCodes/U              :ne
   KeyCodes/B              :sw
   KeyCodes/N              :se
   KeyCodes/UP             :n
   KeyCodes/RIGHT          :e
   KeyCodes/DOWN           :s
   KeyCodes/LEFT           :w
   KeyCodes/NUM_NORTH_EAST :ne
   KeyCodes/NUM_SOUTH_EAST :se
   KeyCodes/NUM_SOUTH_WEST :sw
   KeyCodes/NUM_NORTH_WEST :nw
   KeyCodes/NUM_WEST       :w
   KeyCodes/NUM_NORTH      :n
   KeyCodes/NUM_EAST       :e
   KeyCodes/NKUM_SOUTH     :s})

(defn event->key [e]
  (get (movement-keys) (.-keyCode e) :key-not-found))

(defn read-level []
  (let [lines (string/split (.-textContent (.getElementById js/document "level")) #"\n")
        cells (into [] (map (comp (partial apply vector) seq) lines))
        title "NetHack Sokoban 1a"
        covered-cell ">"
        info ""]
    (js-trace! {:cells cells
      :title title
      :covered-cell covered-cell
      :info info})))

(defn init [level]
  (set! (.-textContent (.getElementById js/document "level")) (levels/->string level))
  (let [event-chan (a/chan)
        key-chan   (a/filter< (apply hash-set (vals (movement-keys))) event-chan)]
    (events/listen (.-body js/document)
                   (.-KEYUP events/EventType)
                   (fn [e]
                     (a/put! event-chan (event->key e))))
    key-chan))
