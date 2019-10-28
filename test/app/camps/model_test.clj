(ns app.camps.model-test
  (:require [app.camps.model :as sut]
            [re-frame.core :as rf]
            [tsys]
            [matcho.core :as matcho]
            [clojure.test :refer :all]
            [zenform.core :as zf]))

(defn not-empty? [x] (not (empty? x)))
(defn not-nil? [x] (not (nil? x)))


(defn input [k v]
  (rf/dispatch [:zf/set-value sut/form-path [k] v]))

(defn form []
  (zf/eval-form (get-in @tsys/app-db sut/form-path)))

(deftest db-test

  ;; (tsys/restart-app)

  (tsys/ensure-app)

  (tsys/truncate :camps)

  ;; (tsys/reset-app-db)
  (tsys/re-open "/camps/new")

  (def page (rf/subscribe [:camps/new]))


  @page

  (:form (:camps/new @tsys/app-db))

  (keys @tsys/app-db)

  (matcho/match @page {:form {}})

  (input :summary "This camp is dedicated to ...")

  (matcho/match (form) {:errors {[:display] {:required string?}}})

  (input :display "My camp")

  (matcho/match (form) {:errors empty?})

  (rf/dispatch [:camps/create])


  )
