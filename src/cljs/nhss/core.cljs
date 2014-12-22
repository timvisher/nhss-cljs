(ns nhss.core
  (:require-macros [cljs.core.async.macros :as am])
  (:require [cljs.core.async     :as a]
            [nhss.transformation :as transformation]
            [nhss.levels         :as levels]
            [nhss.ui             :as ui]
            [weasel.repl         :as ws-repl]

            [nhss.util           :refer [js-trace! trace!]]))


(when (= "localhost" (-> js/document .-location .-hostname))
  (enable-console-print!)
  (ws-repl/connect "ws://localhost:9001"
                   :print #{:repl :console}))

(def level-chan (a/chan))

(levels/standard-level level-chan :2a)

(am/go-loop []
  (let [level          (a/<! level-chan)
        new-level-chan (a/chan)
        command-chan   (ui/init level new-level-chan)]
    (transformation/make-nhss-process command-chan new-level-chan))
  (recur))
