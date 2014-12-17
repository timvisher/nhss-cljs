(ns nhss.ui
  (:require-macros [cljs.core.async.macros :as am])
  (:require [goog.events         :as events]
            [cljs.core.async     :as a]
            [clojure.string      :as string]
            [nhss.levels         :as levels]
            [nhss.transformation :as transformation]
            [om.core             :as om :include-macros true]
            [om.dom              :as dom]

            [nhss.util :refer [js-trace! trace!]])
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
   KeyCodes/NUM_SOUTH      :s})

(defn event->key [e]
  (get (movement-keys) (.-keyCode e) :key-not-found))

(defn read-level []
  (let [level-string         (.-textContent (.getElementById js/document "level"))
        [title info & lines] (string/split level-string #"\n")
        cells                (into [] (map (comp (partial apply vector) seq) lines))
        title                title
        info                 info]
    {:cells        cells
     :title        title
     :info         info}))

(defn key->command [key]
  {:level (read-level)
   :direction key})

(defn level-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [new-level-chan (om/get-state owner :new-level-chan)]
        (am/go-loop []
          (let [new-level (a/<! new-level-chan)]
            (om/transact! app (fn [_]
                                (:level new-level))))
          (recur))))
    om/IRender
    (render [_]
      (dom/pre #js {:id "level"} (levels/->string app)))))

(defn init [level new-level-chan]
  (def app-state level)
  (om/root
   level-view
   app-state
   {:state  {:new-level-chan new-level-chan}
    :target (. js/document (getElementById "app"))})
  (let [event-chan   (a/chan)
        key-chan     (a/filter< (apply hash-set (vals (movement-keys))) event-chan)
        command-chan (a/map< key->command key-chan)]
    (events/listen (.-body js/document)
                   (.-KEYUP events/EventType)
                   (fn [e]
                     (a/put! event-chan (event->key e))))
    command-chan))
