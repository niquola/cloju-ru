(ns myapp.core
  (:require [web.core]
            [db.core]
            [route-map.core :as route-map]
            [clojure.string :as str])
  (:gen-class))

(defn index [ctx]
  {:status 200
   :body "Hello"})

(defn camps [{db :db {{q :q} :params} :request}]
  {:status 200
   :body (db.core/query-resources db (cond->
                                         {:select [:*] :from [:camps]}
                                       (and q (not (str/blank? q)))
                                       (assoc :where [:ilike (db.core/raw "resource::text")
                                                      (str "%" q "%")])))})

(defn get-camp [{db :db {{id :id} :route-params :as req} :request}]
  (if-let [res (db.core/read-resource db :camps {:id id})]
    {:status 200
     :body res}
    {:status 404}))

(defn create-camp [{db :db {data :resource} :request}]
  (let [res (db.core/create-resource db :camps (update data :id (fn [x] (or x (java.util.UUID/randomUUID)))))]
    {:status 200
     :body res}))

(def routes
  {:GET #'index
   "camps" {:GET #'camps
            [:id] {:GET #'get-camp}
            :POST #'create-camp}})


(comment
  (route-map/match [:get "/users/u-1"]
                   {"users" {[:id] {:GET 'handler}}})

  (route-map/match [:get "/"] routes)

  )

(defn handler [{req :request :as ctx}]
  (let [route   (route-map/match [(or (:request-method req) :get) (:uri req)] routes)]
    (if-let [handler (:match route)]
      (handler (update ctx :request assoc :route-params (:params route)))
      {:status 404
       :body (str [(or (:request-method req) :get) (:uri req)] "not found" route)})))

(defn migrate [db]
  (db.core/resource-table db :camps))

(defn start [cfg]
  (let [ctx (atom {:cfg cfg})
        db (when (:db cfg) (db.core/datasource (:db cfg)))
        _ (swap! ctx assoc :db db)
        disp (fn [req] (handler (assoc @ctx :request req)))
        _ (swap! ctx assoc :dispatch disp)
        web (when (:web cfg) (web.core/start {:port 8887} disp))
        _ (swap! ctx assoc :web web)]
    (migrate db)
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
  (start {:db (db-from-env) :web {}}))

(comment
  (db.core/db-spec-from-env)

  (def ctx (start {:db (db.core/db-spec-from-env) :web {}}))

  (def db (:db @ctx))

  (dispatch ctx {:uri "/"})
  (dispatch ctx {:uri "/db/tables" :params {:q "class"}})
  (dispatch ctx {:uri "/" :params {:q "class"}})


  (migrate db)

  (db.core/query db "select * from camps")


  (db.core/create-resource db :camps {:id "clj" :display "Land of Clojure"})
  (db.core/truncate db :camps)

  (doseq [[i disp] [[1 "python"] [2 "ruby"] [3 "clojure"]]]
    (db.core/create-resource
     db :camps
     {:id (str "i-" i) :display disp}))

  (stop ctx)

  )
