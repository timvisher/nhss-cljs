(ns nhss.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async     :as a]
            [nhss.transformation :as transformation]
            [nhss.levels         :as levels]
            [nhss.ui             :as ui]

            [nhss.util           :refer [js-trace!]]))

(let [key-chan (ui/init (levels/standard-level :1a))]
  (go-loop []
    (let [key (a/<! key-chan)
          level (ui/read-level)]
      (js-trace! key))
    (recur)))
