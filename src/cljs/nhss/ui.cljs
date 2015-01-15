(ns nhss.ui
  (:require-macros [cljs.core.async.macros :as am])
  (:require [goog.events         :as events]
            [cljs.core.async     :as a]
            [clojure.string      :as string]
            [nhss.levels         :as levels]
            [nhss.transformation :as transformation]
            [om.core             :as om :include-macros true]
            [om.dom              :as dom]
            [goog.string         :as gstring]

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

(defn key->command [key]
  {:level (:current-level @app-state)
   :direction key})

(defn level->code-forest [level]
  (let [cells (:cells level)
        cell-codes (map (fn [row]
                         (map (fn [cell]
                                (dom/code (cond (= (:player (levels/features)) cell)
                                                #js {:className "cell player"}

                                                (= (:hole (levels/features)) cell)
                                                #js {:className "cell trap"}

                                                (#{(:up-stair (levels/features))
                                                   (:down-stair (levels/features))}
                                                 cell)
                                                #js {:className "cell stair"}

                                                ((:wall (levels/features))
                                                 cell)
                                                #js {:className "cell wall"}

                                                (= (:door (levels/features))
                                                   cell)
                                                #js {:className "cell door"}

                                                (= (:scroll (levels/features))
                                                   cell)
                                                #js {:className "cell scroll"}

                                                :default
                                                #js {:className "cell"})
                                          (if (= " " cell) (gstring/unescapeEntities "&nbsp;") cell)))
                              row))
                       cells)]
    (flatten (interleave cell-codes (repeat (dom/br nil))))))

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
        (apply dom/div #js {:id "level"} (level->code-forest app))))))

(defn level-option-view []
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (dom/option #js {:value (:title app)} (:title app))))))

(defn level-select-view []
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (apply dom/select #js {:onChange (fn [e]
                                           (let [level-title (-> e .-target .-value)
                                                 level-id    (keyword (last (string/split level-title #" ")))]
                                             (swap! app-state assoc :current-level (level-id (:levels @app-state)))))
                               :value (:title (:current-level app))}
               (om/build-all (level-option-view) (sort-by :title (def *charnock* (vals (:levels app))))))))))

(defn make-app-view [new-level-chan]
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (dom/div nil
                 (om/build (level-select-view) app)
                 (om/build (make-level-view new-level-chan) (:current-level app)))))))

;;; TODO this is getting out of hand
(defn init [levels new-level-chan]
  (def app-state (atom {:current-level  (:2a levels)
                        :levels         levels}))
  (def app-history (atom [(:current-level @app-state)]))
  (add-watch app-state :history
             (fn [_ _ _ new-state]
               (when-not (= (last @app-history) (:current-level new-state))
                 (swap! app-history conj (:current-level new-state)))))
  (om/root
   (make-app-view new-level-chan)
   app-state
   {:target (. js/document (getElementById "app"))})
  (let [event-chan                           (a/chan)
        [movement-key-chan other-event-chan] (a/split (apply hash-set (vals (movement-keys))) event-chan)
        undo-key-chan                        (a/filter< (apply hash-set (vals (undo-keys))) other-event-chan)
        command-chan                         (a/map< key->command movement-key-chan)]
    (events/listen (.-body js/document)
                   (.-KEYUP events/EventType)
                   (fn [e]
                     (a/put! event-chan (event->key e))))
    (am/go-loop []
      (let [undo-command (a/<! undo-key-chan)]
        (when (> (count @app-history) 1)
          (swap! app-history pop)
          (swap! app-state (fn [app-state]
                             (assoc app-state :current-level (last @app-history))))))
      (recur))
    command-chan))
