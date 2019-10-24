(ns ^:figwheel-hooks app.dev
  (:require [app.core :as core]
            [re-frisk.core :refer [enable-re-frisk!]]
            [devtools.core :as devtools]))

(devtools/install!)
(enable-console-print!)
(enable-re-frisk!)
(core/init!)

(defn ^:after-load reload []
  (println "Reload")
  (core/mount-root))

