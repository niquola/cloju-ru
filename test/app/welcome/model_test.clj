(ns app.welcome.model-test
  (:require [app.welcome.model]
            [re-frame.core :as rf]
            [tsys]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(defn not-empty? [x] (not (empty? x)))
(defn not-nil? [x] (not (nil? x)))

(deftest db-test

  (tsys/ensure-app)

  (rf/dispatch [:welcome/index :init {}])

  (def page (rf/subscribe [:welcome/index]))

  (matcho/match
   @page
   {:title "Dashboard"}) 


  )
