(ns app.styles
  (:require [garden.core :as garden]))

(defn styles [css]
  [:style (garden/css css)])
