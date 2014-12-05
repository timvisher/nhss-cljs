(ns nhss.core
  (:require [cognitect.transit :as t]
            [clojure.java.io   :as io]))

(defn write-transit-levels-to-out [out]
  (t/write (t/writer out :json) (read-string (slurp "levels.edn"))))

(defn write-transit-levels-to-file []
  (with-open [out (io/output-stream "levels.json")]
    (write-transit-levels-to-out out)))

(defn write-transit-levels-to-string []
  (let [out (java.io.ByteArrayOutputStream. 4096)]
    (write-transit-levels-to-out out)
    (str out)))
