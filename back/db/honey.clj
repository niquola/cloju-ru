(ns db.honey
  (:refer-clojure :exclude [update])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as cs]
            [db.coerce :as coerce]
            [clojure.string :as str]
            [honeysql.core :as sql]
            [clojure.string :as string]
            [honeysql.format :as sqlf]
            [honeysql.helpers :as sqlh])
  (:import org.postgresql.util.PSQLException
           java.sql.BatchUpdateException))


(defmacro pr-error [& body]
  `(try
     ~@body
     (catch java.sql.BatchUpdateException e#
       (if-let [ne# (.getNextException e#)] ;; rethrow exception containing SQL error
         (let [msg# (.getMessage ne#)]
           (throw (java.sql.SQLException. msg#)))
         (do
           (throw e#))))
     (catch org.postgresql.util.PSQLException e#
       (if-let [ne# (.getNextException e#)] ;; rethrow exception containing SQL error
         (let [msg# (.getMessage ne#)]
           (throw (java.sql.SQLException. msg#)))
         (do
           (throw e#))))))

(defmethod sqlf/format-clause :union [[_ maps] _]
  (binding [sqlf/*subquery?* false]
    (string/join " UNION " (map #(str "(" % ")") (map sqlf/to-sql maps)))))

(defmethod sqlf/format-clause :union-all [[_ maps] _]
  (binding [sqlf/*subquery?* false]
    (string/join " UNION ALL " (map #(str "(" % ")") (map sqlf/to-sql maps)))))

;; TODO sanitize logging
(sqlf/register-clause! :returning 230)

(defmethod sqlf/format-clause :returning [[_ fields] sql-map]
  (str "RETURNING "
       (when (:modifiers sql-map)
         (str (sqlf/space-join (map (comp clojure.string/upper-case name)
                                    (:modifiers sql-map)))
              " "))
       (sqlf/comma-join (map sqlf/to-sql fields))))

(sqlf/register-clause! :create-table 1)

(defmethod sqlf/format-clause :create-table [[_ tbl-name] sql-map]
  (str "CREATE TABLE " (sqlf/to-sql tbl-name)))


(sqlf/register-clause! :columns 2)

(defmethod sqlf/format-clause :columns [[_ cols] sql-map]
  (str "("
       (str/join ", " (map #(str/join " " (map name %)) cols))
       ")"))

(sqlf/register-clause! :inherits 3)

(defmethod sqlf/format-clause :inherits [[_ tbls] sql-map]
  (when tbls (str " INHERITS (" (str/join "," (map name tbls)) ")")))


(defmethod sqlf/format-clause :drop-table [[_ tbl-name] sql-map]
  (str "DROP TABLE " (when (:if-exists sql-map) " IF EXISTS ") (sqlf/to-sql tbl-name)))

(sqlf/register-clause! :drop-table 1)

;; UPSERT (from https://github.com/nilenso/honeysql-postgres/)

(sqlf/register-clause! :do-update-set 235)
(sqlf/register-clause! :do-update-set! 235)
(sqlf/register-clause! :do-nothing 235)
(sqlf/register-clause! :upsert 225)

(defmethod sqlf/format-clause :on-conflict-constraint [[_ k] _]
  (let [get-first #(if (sequential? %)
                     (first %)
                     %)]
    (str "ON CONFLICT ON CONSTRAINT " (-> k
                                          get-first
                                          sqlf/to-sql))))

(defmethod sqlf/format-clause :on-conflict [[_ ids] _]
  (let [comma-join-args #(if (nil? %)
                           ""
                           (->> %
                                (map sqlf/to-sql)
                                sqlf/comma-join
                                sqlf/paren-wrap))]
    (str "ON CONFLICT " (comma-join-args ids))))

(defmethod sqlf/format-clause :do-nothing [_ _]
  "DO NOTHING")

(defmethod sqlf/format-clause :do-update-set! [[_ values] _]
  (str "DO UPDATE SET " (sqlf/comma-join (for [[k v] values]
                                           (str (sqlf/to-sql k) " = " (sqlf/to-sql v))))))

(defmethod sqlf/format-clause :do-update-set [[_ values] _]
  (str "DO UPDATE SET "
       (sqlf/comma-join (map #(str (sqlf/to-sql %) " = EXCLUDED." (sqlf/to-sql %))
                             values))))

(defn- format-upsert-clause [upsert]
  (let [ks (keys upsert)]
    (map #(sqlf/format-clause % (find upsert %)) upsert)))

(defmethod sqlf/format-clause :upsert [[_ upsert] _]
  (sqlf/space-join (format-upsert-clause upsert)))

;; END UPSERT

(defmethod sqlf/fn-handler "ilike" [_ col qstr]
  (str (sqlf/to-sql col) " ilike " (sqlf/to-sql qstr)))

(doseq [op ["@@" "@>" "<@" "||" "&&" "->" "->>" "#>>" "#>" "?" "?|" "?&" "#-"]]
  (defmethod sqlf/fn-handler op [_ col qstr]
    (str (sqlf/to-sql col) " " op " " (sqlf/to-sql qstr))))

(defmethod sqlf/fn-handler "not-ilike" [_ col qstr]
  (str (sqlf/to-sql col) " not ilike " (sqlf/to-sql qstr)))

(defn honetize [hsql]
  (cond (map? hsql) (sql/format hsql :quoting :ansi)
        (vector? hsql) (if (keyword? (first hsql)) (sql/format (apply sql/build hsql) :quoting :ansi) hsql)
        (string? hsql) [hsql]))


(defmethod sqlf/fn-handler "<=>" [_ col qstr]
  (str (sqlf/to-sql col) " " "<=>" " " (sqlf/to-sql qstr)))


