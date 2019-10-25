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

  (tsys/truncate :camps)

  (tsys/create-resource :camps {:id "mt" :display "Monad transformers"})
  (tsys/create-resource :camps {:id "vjs" :display "Vui.js futurre"})

  (tsys/match
   {:uri "/camps/vjs"}
   {:status 200
    :body {:id "vjs"}})

  


  )

