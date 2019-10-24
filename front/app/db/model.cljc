(ns app.db.model
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]))

(def page-key :db/index)

(rf/reg-event-fx
 page-key
 (fn [{db :db} [_ phase params]]
   (cond
     (= :init phase)
     {:db (-> (assoc db page-key {:status :progress})
              (assoc :db/filter (get-in params [:params :q])))
      :json/fetch {:uri "/db/tables"
                   :params (:params params)
                   :success {:event :tables/loaded}}}

     (= :params phase)
     {:db (update db page-key merge {:status :progress})
      :json/fetch {:uri "/db/tables"
                   :params (:params params)
                   :success {:event :tables/loaded}}}

     (= :deinit phase)
     {:db db})))


(rf/reg-event-db
 :tables/loaded
 (fn [db [_ {data :data}]]
   (-> db
       (assoc-in [page-key :tables] data)
       (assoc-in [page-key :status] :loaded))))

(rf/reg-event-fx
 :db/filter-table
 (fn [{db :db} [_ q]]
   {:dispatch-debounce {:event [:redirect/set-params {:q q}]
                        :delay 500
                        :key :tables-search}
    :db (assoc db :db/filter q)}))

(defn page-subs [db _]
  (let [m (get db page-key)
        tbls (:tables m)
        cols (keys (first tbls))]
    {:search (:search m)
     :status (:status m)
     :tables {:columns (->> cols (mapv (fn [k] {:id (name k) :title (name k)})))
              :rows (->> tbls
                         (mapv (fn [r]
                                 {:id (:table_name r)
                                  :vals (for [c cols]
                                          {:id c
                                           :value (get r c)})})))}}))

(rf/reg-sub page-key page-subs)

(rf/reg-sub :db/filter (fn [db _] (get db :db/filter)))

(comment
  (println "Here")

  (rf/dispatch [page-key :init {}])





  )
