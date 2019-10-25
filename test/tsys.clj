(ns tsys
  (:require 
   [db.core :as dbc]
   [myapp.core]
   [matcho.core]
   [clojure.test :refer :all]

   [cheshire.core :as json]
   [re-frame.core :as rf]
   [re-frame.interop]
   [re-frame.router]
   [re-test]
   [clojure.string :as str]

   [route-map.core]
   [app.routes]

   ))

(def *db (atom nil))

(def app-db re-test/app-db)

(defn reset-app-db []
  (reset! re-frame.db/app-db {}))

(defn ensure-db []
  (if-let [d @*db]
    d
    (let [db-spec (dbc/db-spec-from-env)
          db (dbc/connection db-spec)]
      (with-open [conn (:connection db)]
        (when-not (dbc/database-exists? db "test")
          (db.core/exec! db "create database test")))
      (reset! *db (dbc/datasource (assoc db-spec :database "test"))))))

(defonce *app (atom nil))
(defn ensure-app []
  (when-not @*app
    (ensure-db)
    (reset! *app
            (myapp.core/start {:db (assoc (dbc/db-spec-from-env) :database "test")}))))

(defn stop-app []
  (myapp.core/stop @*app)
  (reset! *app nil))

(defn restart-app []
  (stop-app)
  (ensure-app))

(defn dispatch [req]
  (myapp.core/dispatch @*app req))

(defn match [req exp-resp]
  (let [resp (dispatch req)]
    (matcho.core/match resp exp-resp)
    resp))

(defn db [] @*db)

(defn create-resource [tp res]
  (db.core/create-resource (db) tp res))

(defn read-resource [tp res]
  (db.core/read-resource (db) tp res))

(defn update-resource [tp res]
  (db.core/update-resource (db) tp res))

(defn truncate [tp]
  (db.core/truncate (db) tp))

(defn delete-resource [tp res]
  (db.core/delete-resource (db) tp res))



(defn json-fetch [{:keys [uri token headers is-fetching-path params success error] :as opts}]
  (if (vector? opts)
    (doseq [o opts] (json-fetch o))
    (let [_ (println "REQ:" (or (:method opts) :get) (:uri opts) (when-let [p (:params opts)] (str "?" p)))
          headers (cond-> {"accept" "application/json"}
                    token (assoc "authorization" (str "Bearer " token))
                    (nil? (:files opts)) (assoc "Content-Type" "application/json")
                    true (merge (or headers {})))
          request (-> opts
                      (dissoc :method)
                      (dissoc :body)
                      (assoc :resource (:body opts))
                      (assoc :headers headers)
                      (assoc :query-string (str/join "&" (map (fn [[k v]] (str (name k) "=" v)) (:params opts)))) ;; FIXME: Probably duplicate
                      (assoc :request-method
                             (if-let [m (:method opts)]
                               (keyword (str/lower-case (name m)))
                               :get)))
          resp (dispatch request)]
      (println "RESP:" resp)
      (if (< (:status resp) 299)
        (if-let [ev (:event success)]
          (rf/dispatch [ev (merge success {:request opts :response resp :data (:body resp)})])
          (throw (Exception. (str "[:success :event] is not provided: " opts))))
        (if-let [ev (:event error)]
          (rf/dispatch [ev (merge error {:request opts :response resp :data (:body resp)})])
          (throw (Exception. (str "[:error :event] is not provided: " opts))))))))

(def browser (atom {}))

(rf/reg-fx :json/fetch json-fetch)
(rf/reg-fx :zframes.redirect/redirect
           (fn [opts] (swap! browser assoc :location opts)))

(defn contexts-diff [route old-contexts new-contexts params old-params]
  (let [n-idx new-contexts
        o-idx old-contexts
        to-dispose (reduce (fn [acc [k o-params]]
                             (let [n-params (get new-contexts k)]
                               (if (= n-params o-params)
                                 acc
                                 (conj acc [k :deinit o-params]))))
                           [] old-contexts)

        to-dispatch (reduce (fn [acc [k n-params]]
                              (let [o-params (get old-contexts k)]
                                (cond
                                  (or (nil? o-params) (not (= n-params o-params)))
                                  (conj acc [k :init n-params])
                                  (and o-params (= (:. n-params) route))
                                  (conj acc [k :return n-params])
                                  :else acc)))
                            to-dispose new-contexts)]
    to-dispatch))

(defn open [url & [params]]
  (if-let [route (route-map.core/match [:. url] app.routes/routes)]
    (let [params (assoc (:params route) :params (or params {}))
          route {:match (:match route) :params params :parents (:parents route)}
          contexts (reduce (fn [acc {c-params :params ctx :context route :.}]
                             (if ctx
                               (assoc acc ctx (assoc c-params :. route))
                               acc)) {} (:parents route))
          current-page (:match route)

          old-page     (get-in @app-db [:route-map/current-route :match])
          old-params   (get-in @app-db [:route-map/current-route :params])

          page-ctx-events (cond
                            (= current-page old-page)
                            (cond (= old-params params) []

                                  (= (dissoc old-params :params)
                                     (dissoc params :params))
                                  [[current-page :params params old-params]]

                                  :else
                                  [[current-page :deinit old-params] [current-page :init params]])
                            :else
                            (cond-> []
                              old-page (conj [old-page :deinit old-params])
                              true (conj [current-page :init params])))

          old-contexts (:route/context @app-db)
          context-evs (contexts-diff (:match route) old-contexts contexts params old-params)]

      (swap! app-db assoc :fragment-path url :route-map/current-route route)
      (doseq [ev (into context-evs page-ctx-events)]
        (println "Dispatch" ev)
        (rf/dispatch ev)))
    (println "Not found:" url)))

(defn re-open [url]
  (reset-app-db)
  (open url))



(comment
  @*db
  (ensure-db)
  (ensure-app)
  *app

  (match {:uri "/"} {:status 300})

  (reset-app-db)
  (open "/")

  @app-db

  )



