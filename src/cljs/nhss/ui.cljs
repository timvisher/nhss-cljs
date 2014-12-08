(ns nhss.ui
  (:require [goog.events :as events]
            [cljs.core.async :refer [put! chan]])
  (:import [goog.events KeyCodes]))

(defn keys []
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
  (get (keys) (.-keyCode e) :key-not-found))

(defn init []
  (let [event-chan (chan)]
    (events/listen (.-body js/document)
                   (.-KEYUP events/EventType)
                   (fn [e]
                     (put! event-chan (event->key e))))))
