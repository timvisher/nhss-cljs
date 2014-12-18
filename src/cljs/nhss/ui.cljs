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

(defn undo-keys []
  {KeyCodes/Z :undo})

(defn event->key [e]
  (get (merge (movement-keys) (undo-keys)) (.-keyCode e) :key-not-found))

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

(defn make-level-view [new-level-chan]
  (fn [app owner]
    (reify
      om/IWillMount
      (will-mount [_]
        (am/go-loop []
          (let [new-level (a/<! new-level-chan)]
            (om/transact! app (fn [_]
                                (:level new-level))))
          (recur)))
      om/IRender
      (render [_]
        (dom/pre #js {:id "level"} (levels/->string app))))))

(defn init [level new-level-chan]
  (def app-state (atom level))
  (def app-history (atom [level]))
  (add-watch app-state :history
             (fn [_ _ _ new-state]
               (when-not (= (last @app-history) new-state)
                 (swap! app-history conj new-state)
                 (.log js/console "Saved new state"))))
  (om/root
   (make-level-view new-level-chan)
   app-state
   {:target (. js/document (getElementById "app"))})
  (let [event-chan                           (a/chan)
        [movement-key-chan other-event-chan] (a/split (apply hash-set (vals (movement-keys))) event-chan)
        undo-key-chan                        (a/filter< (apply hash-set (vals (undo-keys))) other-event-chan)
        command-chan                         (a/map< key->command movement-key-chan)]
    (events/listen (.-body js/document)
                   (.-KEYUP events/EventType)
                   (fn [e]
                     (a/put! event-chan (js-trace! (event->key e)))))
    (am/go-loop []
      (let [undo-command (a/<! undo-key-chan)]
        (.log js/console "called the undo loop")
        (when (> (count @app-history) 1)
          (swap! app-history pop)
          (reset! app-state (last @app-history))))
      (recur))
    command-chan))
