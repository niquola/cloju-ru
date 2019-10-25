(ns app.camps.model
  (:require
   [re-frame.core :as rf]
   [zenform.core :as zf]
   [app.routes :refer [href]]))

(def show-key :camps/show)

(defn show
  [{db :db} [_ phase params]]
  (println "params" params)

  (cond
    (= :init phase)
    {:db (assoc db show-key {:status :progress})
     :json/fetch {:uri (str "/camps/" (:id params))
                  :params (:params params)
                  :success {:event ::loaded}
                  :error   {:event ::failed}}}

    (= :params phase)
    {:db db}

    (= :deinit phase)
    {:db db}))

(defn show-sub
  [{st :status data :data} & _]
  {:camp data})

(defn loaded [db [_ {data :data}]]
  (assoc db show-key {:status :done :data data}))


(defn failed [db [_ {data :data}]]
  (assoc db show-key {:status :error :error data}))


(rf/reg-sub ::show (fn [db & _] (get db show-key)))
(rf/reg-event-db ::loaded loaded)
(rf/reg-event-db ::failed failed)
(rf/reg-event-fx show-key show)
(rf/reg-sub show-key :<- [::show] show-sub)

(def new-key :camps/new)
(def form-path [new-key :form])

(def form-schema
  {:type :form
   :fields {:display  {:type :string
                       :validators {:required {}}}
            :summary  {:type :string
                       :validators {:required {}
                                    :min-length {:value 10}}}}})

(defn new
  [{db :db} [_ phase params]]

  (cond
    (= :init phase)
    {:db (assoc db new-key {:form (zf/form form-schema {})})}

    (= :params phase)
    {:db db}

    (= :deinit phase)
    {:db db}))

(defn new-sub
  [data & _]
  data)

(rf/reg-event-fx new-key new)
(rf/reg-sub ::new (fn [db & _] (get db new-key)))
(rf/reg-sub new-key
            :<- [::new]
            new-sub)
