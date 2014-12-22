(ns nhss.levels
  (:require-macros [cljs.core.async.macros :as am])
  (:require [cognitect.transit :as t]
            [clojure.string    :as string]
            [goog.string       :as gstring]
            [goog.string.format]
            [ajax.core         :as ajax]
            [cljs.core.async   :as a]

            [nhss.util :refer [js-trace! trace! print-level! print-cells!]]))

(defn put-to-chan [chan [ok response]]
  (if ok
    (a/put! chan response)))

(defn standard-levels-string [response-chan]
  (ajax/ajax-request
   {:uri             "nhss/levels.json"
    :method          :get
    :handler         (partial put-to-chan response-chan)
    :response-format (ajax/transit-response-format {:reader (t/reader :json)
                                                    :type   :json})}))

(defn standard-levels<! []
  (let [response-chan (a/chan)
        levels-chan   (a/chan)]
    (standard-levels-string response-chan)
    (am/go-loop []
      (let [resp (a/<! response-chan)]
        (a/put! levels-chan resp))
      (recur))
    levels-chan))

(defn standard-level [level-chan level-id]
  (let [resp-chan (standard-levels<!)]
    (am/go-loop []
      (let [std-levels (a/<! resp-chan)]
        (a/put! level-chan (level-id std-levels)))
      (recur))))

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

(defn level-floor [{:keys [cells]}]
  (mapv (fn [cells]
          (mapv (fn [cell]
                  (cond (or (= (:boulder (features)) cell)
                            (= (:hole (features)) cell))
                        (:space (features))

                        ((:wall (features)) cell)
                        (:empty (features))

                        (= (:player (features)) cell)
                        (:down-stair (features))

                        :default
                        cell))
                cells))
        cells))

(defn read-level [level-string]
  (let [[title info & lines] (string/split level-string #"\n")
        cells                (into [] (map (comp (partial apply vector) seq) lines))
        title                title
        info                 info
        level                {:cells        cells
                              :title        title
                              :info         info}
        level                (assoc level :floor (level-floor level))]
    level))
