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
   ))

(def *db (atom nil))

(def app-db re-test/app-db)

(defn reset-db []
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

(defn dispatch [req]
  (myapp.core/dispatch @*app req))

(defn match [req exp-resp]
  (let [resp (dispatch req)]
    (matcho.core/match resp exp-resp)
    resp))

(defn db [] @*db)


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



(comment
  @*db
  (ensure-db)
  (ensure-app)
  *app

  (match {:uri "/"}
         {:status 300})

  )



