(ns myapp.core-test
  (:require [tsys]
            [clojure.test :refer :all]))

(deftest test-dojo
  (tsys/ensure-app)

  (tsys/match
   {:uri "/"}
   {:status 200})


  (tsys/match
   {:uri "/ups"}
   {:status 404})


  


  )

