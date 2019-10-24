(ns app.<page>.model-test
  (:require [app.<page>.model :as model]
            [clojure.test :refer :all]
            [re-frame.core :as rf]
            [re-frame.db]
            [headless-server :as hs]
            [world :as world]
            [matcho.core :as matcho]))

(deftest test-<page>
  (reset! re-frame.db/app-db {})

  (rf/dispatch [:<page>/index :init {}])

  (def m (rf/subscribe [:<page>/new]))

  (matcho/match @m 1))

