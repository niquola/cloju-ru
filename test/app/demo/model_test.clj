(ns app.demo.model-test
  (:require [app.demo.model :as subj]
            [re-frame.core :as rf]
            [tsys]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))


(deftest demo-test

  (tsys/reset-app-db)

  (rf/dispatch [:click])

  @tsys/app-db

  (def model (rf/subscribe [:counter]))

  @model

  (is (= "Clicked 2 tiems" (:display @model)))


  )
