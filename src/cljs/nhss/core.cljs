(ns nhss.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async     :as a]
            [nhss.transformation :as transformation]
            [nhss.levels         :as levels]
            [nhss.ui             :as ui]
            [weasel.repl         :as ws-repl]

            [nhss.util           :refer [js-trace! trace!]]))


(when (= "file:" (-> js/document .-location .-protocol))
  (enable-console-print!)
  (ws-repl/connect "ws://localhost:9001"))

(let [new-level-chan (a/chan)
      command-chan   (ui/init (levels/standard-level :1a) new-level-chan)]
  (transformation/make-nhss-process command-chan new-level-chan))
