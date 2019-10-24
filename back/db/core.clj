(ns db.core
  (:require
   [db.pool :as pool]
   [db.honey :as honey :refer [pr-error]]
   [honeysql.core]
   [db.coerce :as coerce]
   [honeysql.format :as sqlf]
   [ring.util.codec]
   [clojure.walk]
   [clojure.string :as str]
   [clojure.java.jdbc :as jdbc])

  (:import org.postgresql.copy.CopyManager
           java.nio.charset.StandardCharsets
           java.sql.Connection))


(defn env [v]
  (-> v (name)
      (str/upper-case)
      (str/replace #"-" "_")
      (System/getenv)))

(defn db-spec-from-env []
  {:host (env :pghost)
   :port (env :pgport)
   :user (env :pguser)
   :database (env :pgdatabase)
   :password (env :pgpassword)})


(defn database-url [spec]
  (let [conn spec]
    (str "jdbc:postgresql://" (:host conn) ":" (or (:port! conn) (:port conn))
         "/" (:database conn)
         "?user=" (:user conn)
         "&password=" (:password conn) "&stringtype=unspecified")))

(defn datasource [spec]
  (let [ds-opts   (let [database-url (database-url spec)]
                    {:connection-timeout 30000
                     :idle-timeout 10000
                     :minimum-idle       0
                     :maximum-pool-size  30
                     :connection-init-sql "select 1"
                     :data-source.url   database-url})
        ds (pool/create-pool ds-opts)]
    {:datasource ds}))

(defn close-connection [conn]
  (.close (:connection conn)))

(defn shutdown [{conn :datasource}]
  (pool/close-pool conn))

(defn connection
  "open root connection"
  [db-spec]
  {:connection (jdbc/get-connection {:connection-uri (database-url db-spec)})})

(defn with-connection [db f]
  (if-let [conn (jdbc/db-find-connection db)]
    (f conn)
    (with-open [conn (jdbc/get-connection db)]
      (f conn))))

(defmacro from-start [start]
  `(- (System/currentTimeMillis) ~start))


(defn with-transaction [db f]
  (jdbc/with-db-transaction [conn db]
    (f conn)))

(defn query
  "query honey SQL"
  ([db hsql]
   (pr-error
    (let [sql (honey/honetize hsql)
          start (System/currentTimeMillis)]
      (try
        (let [res (jdbc/query db sql)]
          ;; (println :query sql)
          res)
        (catch Exception e
          (println :query sql)
          (throw e))))))
  ([db h & more]
   (query db (into [h] more))))

(defn query-with-timeout
  "query honey SQL"
  ([db timeout hsql]
   (pr-error
    (let [sql (honey/honetize hsql)
          start (System/currentTimeMillis)]
      (try
        (with-connection db (fn [conn]
                              (let [ps  (let [ps (jdbc/prepare-statement conn (first sql))]
                                          (loop [ix 1 values (rest sql)]
                                            (when (seq values)
                                              (jdbc/set-parameter (first values) ps ix)
                                              (recur (inc ix) (rest values))))
                                          (.setQueryTimeout ps timeout)
                                          ps)
                                    res (jdbc/query conn ps)]
                                (println :query sql)
                                res)))
        (catch org.postgresql.util.PSQLException e
          (let [msg (let [m (.getMessage e)]
                      (if (= m "ERROR: canceling statement due to user request")
                        (str "Request time is longer than timeout - " timeout " sec")
                        m))]
            (println :error msg sql))
          (throw e))))))
  ([db timeout h & more]
   (query db (into [h] more))))

(defn query-first [db & hsql]
  (first (apply query db hsql)))

(defn query-value [db & hsql]
  (when-let [row (apply query-first db hsql)]
    (first (vals row))))


(defn execute!
  "execute honey SQL"
  [db hsql]
  (pr-error
   (let [sql (honey/honetize hsql)
         start (System/currentTimeMillis)]
     (try
       (let [res (jdbc/execute! db sql {:transaction? false})]
         (println :exec sql)
         res)
       (catch Exception e
         (println :err e)
         (throw e))))))


(defn exec!
  "execute raw SQL without escape processing"
  [db sql]
  (pr-error
   (let [start (System/currentTimeMillis)]
     (try
       (with-connection db
         (fn [con]
           (let [stmt (.prepareStatement con sql)
                 _    (.setEscapeProcessing stmt false)
                 res  (.execute stmt)]
             (println :exec sql)
             res)))
       (catch Exception e
         (println :error e)
         (throw e))))))

(defn retry-connection
  "open root connection"
  [db-spec & [max-retry timeout]]
  (let [max-retry (or max-retry 20)]
    (loop [retry-num max-retry]
      (let [res (try (let [conn (connection db-spec)] (query conn "SELECT 1") conn)
                     (catch Exception e
                       (println (str "Error while connecting to " (dissoc db-spec :password) " - " (.getMessage e)))))]
        (cond
          res res

          (> 0 retry-num)
          (let [msg (str "Unable to connect to " (dissoc db-spec :password))]
            (println msg)
            (throw (Exception. msg)))

          :else (do
                  (println "Retry connection to " (dissoc db-spec :password))
                  (Thread/sleep (or timeout 2000))
                  (recur (dec retry-num))))))))


(defn with-retry-connection
  [db-spec f & [max-retry timeout]]
  (with-open [c (:connection (retry-connection db-spec max-retry timeout))]
    (f {:connection c})))

(defn- coerce-entry [conn spec ent]
  (reduce (fn [acc [k v]]
            (assoc acc k (cond
                           (vector? v) (coerce/to-pg-array conn v (get-in spec [:columns k :type]))
                           (map? v)    (coerce/to-pg-json v)
                           :else v)))
          {} ent))

(defn insert [db {tbl :table :as spec} data]
  (let [values (if (vector? data) data [data])
        values (map #(coerce-entry db spec %) values)
        res (->> {:insert-into tbl
                  :values values
                  :returning [:*]}
                 (query db))]
    (if (vector? data) res (first res))))

(defn do-update [db {tbl :table pk :pk :as spec} data]
  (let [pk (or pk :id)]
    (->> {:update tbl
          :set (coerce-entry db spec (dissoc data :id))
          :where [:= pk (pk data)]
          :returning [:*]}
         (query-first db))))

(defn delete [db {tbl :table :as spec} id]
  (->> {:delete-from tbl :where [:= :id id] :returning [:*]}
       (query-first db)))


(defn quailified-name [tbl]
  (let [[i1 i2] (str/split (name tbl) #"\." 2)
        tbl (if i2 i2 i1)
        sch (if i2 i1 "public")]
    [sch tbl]))

(defn table-exists? [db tbl]
  (let [tbl (if (map? tbl) (:table tbl) tbl)
        [sch tbl] (quailified-name tbl)]
    (= 1
       (->> {:select [1]
             :from [:information_schema.tables]
             :where [:and [:= :table_schema sch] [:= :table_name (name tbl)]]}
            (query-value db)))))

(defn database-exists? [db db-name]
  (->> {:select [true]
        :from [:pg_database]
        :where [:= :datname (name db-name)]}
       (query-value db)))

(defn user-exists? [db user]
  (let [user (if (map? user) (:user user) user)]
    (->> {:select [true] :from [:pg_catalog.pg_roles] :where [:= :rolname user]}
         (query-value db)
         (some?))))

(defn create-user [db {user :user password :password}]
  (when-not (user-exists? db user)
    (exec! db (format "CREATE USER %s WITH ENCRYPTED PASSWORD '%s'" user password))))

(defn drop-user [db {user :user}]
  (exec! db (format "DROP USER IF EXISTS %s" user)))

(defn drop-database [db {dbname :database :as spec}]
  (println :db/drop-db {:db (:database spec)})
  (exec! db (format "DROP DATABASE IF EXISTS %s" dbname)))


(def raw honeysql.core/raw)


(defn- copy-stream [msgs]
  (let [len (dec (count msgs))
        idx (atom -1)]
    (proxy [java.io.InputStream] []
      (read
        ([] (assert false "Unexpected"))
        ([^bytes bs off len] (assert false "Unexpected"))
        ([^bytes bs]
         (if-let [msg (when (< @idx len) (nth msgs (swap! idx inc)))]
           (let [data (.getBytes msg)
                 len (alength data)
                 buf-len (alength bs)]
             (if (> buf-len len)
               (System/arraycopy data 0 bs 0 len)
               (assert false "Unexpected"))
             len)
           -1))))))

(defn to-line [cols msg]
  (str (str/join "\t" (mapv msg cols)) "\n"))

(defn mk-copy [db batch-size tbl cols]
  (let [st (atom {:cnt 0 :msgs []})]
    (fn [msg]
      (let [state @st
            msgtxt (to-line cols msg)]
        (if (>= (:cnt state) batch-size)
          (let [strm (copy-stream (:msgs state))]
            (println "Load" batch-size)
            (with-connection db
              (fn [conn]
                (let [pg-conn  (.unwrap ^java.sql.Connection conn org.postgresql.PGConnection)
                      cm       (org.postgresql.copy.CopyManager. pg-conn)
                      copy-sql (format "COPY %s (%s) FROM STDIN delimiter e'\\t'"
                                       (name tbl)
                                       (->> cols (mapv name) (str/join ",")))
                      cnt (time (.copyIn cm copy-sql strm))]
                  cnt)))
            (swap! st assoc :cnt 1 :msgs [msgtxt]))
          (swap! st (fn [s] (-> s
                                (clojure.core/update :cnt inc)
                                (clojure.core/update :msgs conj msgtxt)))))))))

(comment

  )
