(ns nhss.core
  (:require [cognitect.transit :as t]
            [clojure.java.io   :as io]))

(defn write-transit-levels []
  (with-open [out (io/output-stream "levels.json")]
    (t/write (t/writer out :json) (read-string (slurp "levels.edn")))))
