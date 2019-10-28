(ns myapp.core-test
  (:require [tsys]
            [clojure.test :refer :all]))

(deftest test-myapp
  (tsys/restart-app)

  ;; (tsys/ensure-app)

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

  (tsys/match
   {:uri "/camps"}
   {:status 200
    :body [{:id "mt"}]})

  (tsys/match
   {:uri "/camps" :request-method :post
    :resource {:id "c-1" :display "ups" :summary "hi"}}
   {:status 200
    :body {:id "c-1"}})

  (tsys/match
   {:uri "/camps/c-1"}
   {:status 200
    :body {:id "c-1"}})



  )

