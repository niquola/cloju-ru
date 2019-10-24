(ns build
  (:require [cambada.uberjar :as uberjar]))


(defn -main []
  (uberjar/-main
   "-a" "all"
   "-p" "ui/build.prod"
   "--app-group-id" "app"
   "--app-artifact-id" "app"
   "--app-version" "0.0.1"
   "-m" "clojure.main"))

(comment
  (-main)
  )


