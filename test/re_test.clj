(ns re-test
  (:require [re-frame.core :as rf]
            [re-frame.interop]
            [re-frame.router]
            [re-frame.subs]
            [clojure.string :as str]))

(defonce queue (atom re-frame.interop/empty-queue))

(def ^{:dynamic true, :private true} *handling* false)

(defn- dequeue!
  [queue-atom]
  (let [queue @queue-atom]
    (when (seq queue)
      (if (compare-and-set! queue-atom queue (pop queue))
        (peek queue)
        (recur queue-atom)))))

(defn dispatch [argv]
  (swap! queue conj argv)
  (when-not *handling*
    (binding [*handling* true]
      (loop []
        (when-let [queue-head (dequeue! queue)]
          (do
            (println "handle " queue-head)
            (re-frame.router/dispatch-sync queue-head))
          (recur))))))

(alter-var-root (var re-frame.core/dispatch) (constantly dispatch))
(alter-var-root (var re-frame.router/dispatch) (constantly dispatch))
;; turn of the subs cache
(alter-var-root (var re-frame.subs/cache-lookup)
                (fn [_]
                  (fn [& args]
                    #_(println ">>> Cache lookup" args)
                    )))

(def app-db re-frame.db/app-db)

;; May need to implement it or remove?
(defn clear-subscription-cache! [])
