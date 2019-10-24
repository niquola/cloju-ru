(ns myapp.core
  (:require [web.core]
            [db.core]
            [route-map.core :as route-map])
  (:gen-class))

(defn index [ctx]
  {:status 200
   :body "Hello"})


(defn camps [{db :db {{q :q} :params} :request}]
  {:status 200
   :body {:message "Here"}})

(def routes
  {:GET #'index
   "camps" {:GET #'camps}})


(defn handler [{req :request :as ctx}]
  (let [route   (route-map/match [(or (:request-method req) :get) (:uri req)] routes)]
    (if-let [handler (:match route)]
      (handler ctx)
      {:status 404
       :body (str [(or (:request-method req) :get) (:uri req)] "not found" route)})))

(defn start [cfg]
  (let [ctx (atom {:cfg cfg})
        db (when (:db cfg) (db.core/datasource (:db cfg)))
        _ (swap! ctx assoc :db db)
        disp (fn [req] (handler (assoc @ctx :request req)))
        _ (swap! ctx assoc :dispatch disp)
        web (when (:web cfg) (web.core/start {:port 8887} disp))
        _ (swap! ctx assoc :web web)]
    ctx))

(defn stop [ctx]
  (try
    (when-let [srv (:web @ctx)] (srv))
    (catch Exception e))
  (try
    (when-let [db (:db @ctx)] (db.core/shutdown db))
    (catch Exception e)))

(defn dispatch [ctx req]
  ((:dispatch @ctx) req))

(defn db-from-env []
  (db.core/db-spec-from-env))

(defn -main [& args]
  (start {:db (db-from-env) 
          :web {}}))

(comment
  (db.core/db-spec-from-env)

  (def ctx (start {:db (db.core/db-spec-from-env)
                   :web {}}))

  (:db @ctx)

  (dispatch ctx {:uri "/"})
  (dispatch ctx {:uri "/db/tables" :params {:q "class"}})

  (stop ctx)

  


  )
