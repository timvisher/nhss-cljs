(ns nhss.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async     :refer [<!]]
            [nhss.transformation :refer [make-nhss-process]]
            [nhss.ui             :as ui]))

(let [key-chan (ui/init)]
  (go-loop [key (<! key-chan)]
    (.log js/console key)
    (recur (<! event-chan))))
