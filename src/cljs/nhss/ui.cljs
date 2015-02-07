(ns nhss.ui
  (:require-macros [cljs.core.async.macros :as am])
  (:require [goog.events              :as events]
            [cljs.core.async          :as a]
            [clojure.string           :as string]
            [nhss.levels              :as levels]
            [nhss.standard-level-data :as standard-level-data]
            [nhss.transformation      :as transformation]
            [om.core                  :as om :include-macros true]
            [om.dom                   :as dom]

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
  {:level     (:level @app-state)
   :direction key})

(defn cell-code [cell]
  (dom/code (cond (= (:player levels/features) cell)
                  #js {:className "cell player"}

                  (= (:hole levels/features) cell)
                  #js {:className "cell trap"}

                  (#{(:up-stair levels/features)
                     (:down-stair levels/features)}
                   cell)
                  #js {:className "cell stair"}

                  ((:wall levels/features)
                   cell)
                  #js {:className "cell wall"}

                  (= (:door levels/features)
                     cell)
                  #js {:className "cell door"}

                  (= (:scroll levels/features)
                     cell)
                  #js {:className "cell scroll"}

                  :default
                  #js {:className "cell"})
            cell))

(def cell-view
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (cell-code app)))))

(def row-view
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (apply dom/div #js {:className "row"}
               (om/build-all cell-view app))))))

(def level-option-view
  (fn [title owner]
    (reify
      om/IRender
      (render [_]
        (dom/option #js {:value title} title)))))

(def level-select-view
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (apply dom/select #js {:onChange (fn [e]
                                           (let [level-title (.. e -target -value)
                                                 level-id    (keyword level-title)]
                                             (swap! app-state assoc :level (level-id standard-level-data/levels))))
                               :onKeyDown (fn [e]
                                            (.preventDefault e))
                               :value    (:current-title app)}
               (om/build-all level-option-view (:titles app)))))))

(def info-view
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (dom/div nil
                 (dom/code #js {:className "info"} app))))))

(def level-view
  (fn [app owner]
    (reify
      om/IWillMount
      (will-mount [_]
        (am/go-loop []
          (let [new-level (a/<! (:new-level-chan app))]
            (om/update! app [:level] (:level new-level)))
          (recur)))
      om/IRender
      (render [_]
        (apply dom/div #js {:id "level"}
               (om/build info-view (:info (:level app)))
               (om/build-all row-view (:cells (:level app))))))))

(def app-view
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (dom/div nil
                 (om/build level-select-view {:current-title (:title (:level app))
                                              :titles        (:titles app)})
                 (om/build level-view app))))))

;;; TODO this is getting out of hand
(defn init [new-level-chan]
  (def app-state (atom {:level          ((keyword "NetHack Sokoban 2a") standard-level-data/levels)
                        :new-level-chan new-level-chan
                        :titles         (mapv :title (sort-by :title (vals standard-level-data/levels)))}))
  (def app-history (atom [(:level @app-state)]))
  (add-watch app-state :history
             (fn [_ _ _ new-state]
               (when-not (= (last @app-history) (:level new-state))
                 (swap! app-history conj (:level new-state)))))
  (om/root
   app-view
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
                             (assoc app-state :level (last @app-history))))))
      (recur))
    command-chan))
