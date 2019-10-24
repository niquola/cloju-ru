(ns app.<page>.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]
   [app.helpers :as helpers]))


(rf/reg-event-fx
 :<page>/index
 (fn [{db :db} [_ phase params]]
   (cond
     (= :init phase)
     {:db db}

     (= :params phase)
     {:db db}

     (= :deinit phase)
     {:db db})))

(rf/reg-sub
 :<page>/index
 (fn [db _] (:<page>/index db)))
