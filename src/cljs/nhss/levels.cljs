(ns nhss.levels
  (:require-macros [cljs.core.async.macros :as am])
  (:require [cognitect.transit :as t]
            [clojure.string    :as string]
            [goog.string       :as gstring]
            [goog.string.format]
            [ajax.core         :as ajax]
            [cljs.core.async   :as a]

            [nhss.util :refer [js-trace! trace! print-level! print-cells!]]))

(defn ->string [level]
  (gstring/format "%s\n%s\n%s"
                  (:title level)
                  (:info level)
                  (string/join "\n" (map string/join (:cells level)))))

(defn features []
  {:down-stair \>
   :up-stair   \<
   :space      \·
   :boulder    \0
   :hole       \^
   :player     \@
   :empty      \space
   :wall       #{\┴ \┼ \┤ \┌ \├ \─ \└ \┐ \┬ \┘ \│}})
