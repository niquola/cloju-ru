(ns db.copy
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [cheshire.core :as json])

  (:import org.postgresql.copy.CopyManager
           java.nio.charset.StandardCharsets
           java.sql.Connection
           java.util.zip.GZIPInputStream))


;; (set! *warn-on-reflection* true)

(defn read-stream [stream f & [acc]]
  (let [bs (java.io.BufferedInputStream. stream)
        gz (java.util.zip.GZIPInputStream. bs)
        br (java.io.BufferedReader. (java.io.InputStreamReader. gz java.nio.charset.StandardCharsets/UTF_8))]
    (loop [acc acc]
      (if-let [l (.readLine br)]
        (recur (f acc l))
        acc))))

(defn open-stream [^String uri]
  (if (str/starts-with? uri "/")
    (java.io.FileInputStream. uri)
    (.openStream (java.net.URL. uri))))


(defn to-stream [^java.io.BufferedReader br tr]
  (proxy [java.io.InputStream] []
    (read
      ([] (assert false "Unexpected"))
      ([^bytes bs]
       (if-let [l (.readLine br)]
         (let [res (cheshire.core/parse-string l keyword)
               fhir (.getBytes (str (cheshire.core/generate-string (tr res)) "\n"))
               len (alength fhir)
               buf-len (alength bs)]
           (if (> buf-len len)
             (System/arraycopy fhir 0 bs 0 len)
             (assert false "Unexpected"))
           len)
         -1))
      ([^bytes bs off len]
       (assert false "Unexpected")))))

(defn load-from-url
  [{conn :conn uri :uri ctx :ctx table-name :table :as opts}]
  (with-open [^java.io.InputStream src (open-stream uri)
              gz (java.util.zip.GZIPInputStream. src)]
    (let [db {:connection conn}]
      (let [pg-conn  (.unwrap ^java.sql.Connection conn org.postgresql.PGConnection)
            cm       (org.postgresql.copy.CopyManager. pg-conn)
            ^java.io.InputStream fhir-s (let [br (java.io.BufferedReader. (java.io.InputStreamReader. gz java.nio.charset.StandardCharsets/UTF_8))
                                              conv identity]
                                          (to-stream br (fn [x] (conv x))))
            copy-sql (format "COPY \"%s\" (resource) FROM STDIN csv quote e'\\x01' delimiter e'\\t'" table-name)
            cnt (.copyIn cm copy-sql fhir-s)]
        cnt))))
