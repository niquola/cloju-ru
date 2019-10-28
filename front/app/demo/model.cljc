(ns app.demo.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

;; event handler
;; update db
(defn user-event [{db :db :as fx} _]
  {:db (update db :counter (fn [i] (inc (or i 1))))})

;; view model
(defn counter-model [db _]
  (let [cnt (get-in db [:counter])]
    {:display (str "Clicked " (or cnt 0) " times!")}))

(comment

  (user-event {:db {}} [:click])

  (counter-model {:counter 5} [:counter])

  )

(rf/reg-event-fx :click user-event)
(rf/reg-sub :counter counter-model)

(def page-key :demo/index)

;; page
(defn index [{db :db} [_ phase params]]
  (println "Page demo" phase params)
  (cond
    (= :init phase)
    {:db db}

    (= :params phase)
    {:db db}

    (= :deinit phase)
    {:db db}))

(defn page-model [db _]
  (get db page-key))

(rf/reg-event-fx page-key index)
(rf/reg-sub page-key page-model)
