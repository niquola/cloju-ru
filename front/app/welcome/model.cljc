(ns app.welcome.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

(def page-key :welcome/index)

(defn index [{db :db} [_ phase params]]
  (cond
    (= :init phase)
    {:db (assoc db page-key {:status :progress})
     :json/fetch {:uri "/camps"
                  :params (:params params)
                  :success {:event ::loaded}
                  :error   {:event ::failed}}}

    (= :params phase)
    {:db db}

    (= :deinit phase)
    {:db db}))


(rf/reg-event-db
 ::loaded
 (fn [db [_ {data :data}]]
   (-> db
       (assoc-in [page-key :data] data)
       (assoc-in [page-key :status] :loaded))))

(rf/reg-event-db
 ::failed
 (fn [db [_ {data :data}]]
   (-> db
       (assoc-in [page-key :error] data)
       (assoc-in [page-key :status] :failed))))

(rf/reg-event-fx page-key index)

(defn page-subs [db & args] (get db page-key))

(rf/reg-sub page-key page-subs)

(comment
  (println "Here")

  (rf/dispatch [page-key :init {}])


  )
