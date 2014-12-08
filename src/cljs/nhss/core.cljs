(ns nhss.core
  (:require [cljs.core.async     :refer [chan]]
            [nhss.transformation :refer [make-nhss-process]]
            [nhss.ui             :refer [init]]))

(init)
