(ns test-runner
  (:require [cognitect.test-runner]
            [clojure.test :as test])
  (:gen-class))

(defn -main [& args]
  (println "Run aidbox tests")
  (apply cognitect.test-runner/-main args))

(comment

  (-main)

  (cognitect.test-runner/test {})

  )
