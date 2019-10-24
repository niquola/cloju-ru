(ns db.core-test
  (:require [db.core :as sut]
            [clj-yaml.core]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(defn dump [x]
  (spit "/tmp/res.yaml"
        (clj-yaml.core/generate-string
         x)))

(deftest db.core-test
  (sut/db-spec-from-env)

  (def db-spec (sut/db-spec-from-env))

  (defonce db (sut/connection db-spec))


  (matcho/match
   (sut/query db "select 1 a")
   [{:a 1}])

  (matcho/match
   (sut/query-first db "select 1 a")
   {:a 1})

  (matcho/match
   (sut/query-value db "select 1 a")
   1)

  (sut/table-exists? db "users")
  (sut/table-exists? db "informational_schema.tables")

  (sut/exec! db
             "
drop table if exists tests;
create table tests (
 id serial primary key,
 resource jsonb
);

")

  (sut/insert db {:table :tests} {:id 2 :resource {:name "John"}})
  (sut/insert db {:table :tests} {:id 1 :resource {:name "John"}})

  (sut/do-update db {:table :tests} {:id 1 :resource {:name "Ivan"}})

  (matcho/match
   (sut/query db (merge {:select [:*]}
                        {:from [:tests]
                         :order-by [:id]}))
   [{:id 1} {:id 2}])

  )

