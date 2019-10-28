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


(defn loaded [db [_ {data :data}]]
  (-> db
      (assoc-in [page-key :data] data)
      (assoc-in [page-key :status] :loaded)))

(defn failed [db [_ {data :data}]]
  (-> db
      (assoc-in [page-key :error] data)
      (assoc-in [page-key :status] :failed)))

(comment 
  (second (re-find #"[^0-9]*-(\d+).*" "i-332")))

(defn page-subs [{st :status data :data} & _]
  {:grid (cond
           (= :progress st) {:message "Loading..."}
           (empty? data)    {:message "Nothing to show"}
           data             {:items (->> data
                                         (mapv (fn [{id :id :as r}]
                                                 {:id id
                                                  :display (:display r)
                                                  :img (str "https://picsum.photos/id/"
                                                            (second (re-find #"[^0-9]*-(\d+).*" id))
                                                            "/50/50")
                                                  :href (href "camps" id)})))})})


(defn do-search [{db :db} [_ q]]
  (println "search??" q)
  {:db (assoc-in db [page-key :status] :progress)
   :json/fetch {:uri "/camps"
                :params {:q q}
                :success {:event ::loaded}
                :error   {:event ::failed}}})


(rf/reg-sub ::page (fn [db & _] (get db page-key)))
(rf/reg-event-fx page-key index)
(rf/reg-sub page-key :<- [::page] page-subs)
(rf/reg-event-db ::loaded loaded)
(rf/reg-event-db ::failed failed)
(rf/reg-event-fx ::search do-search)

