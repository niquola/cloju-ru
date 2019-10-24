(ns user
  (:require [cider-nrepl.main]
            [clojure.java.shell :as shell]
            [cider-nrepl.main]
            [figwheel.main.api]
            [build]
            [clojure.string :as str]))

(defn start-nrepl []
  (println "Starting nrepl...")
  (cider-nrepl.main/init
   ["refactor-nrepl.middleware/wrap-refactor"
    "cider.nrepl/cider-middleware"
    "cider.piggieback/wrap-cljs-repl"]))

(defn -main [& args]
  (start-nrepl))

(def dev-output-dir "front.build/public")

(defn figwheel []
  (build/link-files dev-output-dir)
  (figwheel.main.api/start
   {:id "app"
    :options {:main 'app.dev
              :pretty-print  true
              :source-map true
              :asset-path "js"
              :output-to (str dev-output-dir "/app.js")
              :output-dir (str dev-output-dir "/js")
              :optimizations :none}
    :config {:mode :serve
             :open-url false
             :watch-dirs ["front"]}})

  (build/shell "rm -f front.build/*")
  ;; (build/shell "ln -s build.dev build")
  )

(defn cljs-repl []
  (figwheel.main.api/cljs-repl "app"))

(defn server []
  ;; (dojo.core/start {:db (dojo.core/db-from-env)
  ;;                   :web {}})
  )


(comment
  (figwheel)

  (figwheel.main.api/stop-all)

  (cljs-repl)

  (def srv (server))

  ;; run figwheel
  ;; run server
  ;; for ui open http://localhost:8887/static/index.html

  (dojo.core/stop srv)
  )
