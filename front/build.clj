(ns build
  (:require [cljs.build.api :as api]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))


(def prod-out-dir "build.prod/public")
(def prod-opts {:output-dir    "target/out"
                :aot-cache true
                :parallel-build true
                :infer-externs true
                :optimizations :advanced})

(defn prod-config [cfg]
  (merge-with merge prod-opts cfg))


(defn shell [cmd]
  (println "$ " cmd)
  (let [res (apply shell/sh (str/split cmd #"\s+"))]
    (if (= 0 (:exit res))
      (str/replace (:out res) #"\s*\n$" "")
      (throw (Exception. (pr-str res))))))

(defn link-files [build-dir]
  (shell (format "mkdir -p %s" build-dir))
  (doseq [[trg src] {"index.html"       "index.html"
                     "assets"            "assets"}]

    (shell (format "rm -f %s/%s" build-dir trg)) 
    (shell (format "ln -s ../../resources/public/%s %s/%s" src build-dir trg))))
 
(defn build []
  (println "build into" prod-out-dir)
  (shell (format "mkdir -p %s" prod-out-dir))
  (println "build into" )
  (time 
   (api/build
    "src"
    (prod-config
     {:output-to    (str prod-out-dir "/app.js")
      :main 'app.prod
      :externs []})))

  (link-files prod-out-dir)
  (shell "rm -f build")
  (shell "ln -s build.prod build"))

(defn -main []
  (build)
  (System/exit 0))

(comment
  (build))
