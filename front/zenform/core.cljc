(ns zenform.core
  (:require [zenform.validators :as validators]
            [clojure.string :as str]
            [re-frame.core :as rf]))

(defn *form [sch path val]
  (cond
    (= :form (:type sch))
    (assoc (dissoc sch :fields) :value
           (reduce (fn [acc [k *sch]]
                     (let [v (get val k)]
                       (assoc acc k (*form *sch (conj path k) v))))
                   {} (:fields sch)))

    (= :collection (:type sch))
    (assoc sch :value
           (->> (map-indexed (fn [i *val]
                               [i (*form
                                   (dissoc (:item sch) :value)
                                   (conj path i)
                                   *val)]) val)
                (into {})))

    (:type sch)
    (assoc sch :value val)

    :else val))

(defn form
  "create form model from schema and defaults"
  [schema & [value]]
  (*form schema [] (or value {})))

(defn get-node-path  [pth]
  (reduce (fn [acc x] (conj acc :value x)) [] pth))

(defn get-value-path  [pth]
  (conj (get-node-path pth) :value))

(defn *get-value [form]
  (cond
    (and (:value form) (= :collection (:type form)))
    (let [res (->> (mapv *get-value (mapv second (sort-by first (:value form))))
                   (filterv #(not (nil? %))))]
      (when-not (empty? res) res))


    (and  (map? form) (map? (:value form)) (= :form (:type form)))
    (let [res (reduce (fn [acc [k node]]
                        (let [v (*get-value node)]
                          (if-not (nil? v)
                            (assoc acc k v)
                            acc))) {} (:value form))]
      (when-not (empty? res) res))


    :else (:value form)))


(defn get-value
  "Get value for specific path; if path not passed returns form value"
  ([form path]
   (*get-value (get-in form (get-node-path path))))
  ([form]
   (*get-value form)))


(defn validate-node [node v]
  (reduce (fn [errs [k cfg]]
            (if-let [msg (validators/validate (update cfg :type (fn [x] (or x k))) v)]
              (assoc errs k msg)
              errs)) nil (:validators node)))


(defn fire-on-change [form-path form &[path]]
  (when-let [node (get-in form (or path []))]
    (when-let [change (:on-change node)]
      (let [ppth (vec (remove #(= :value %) path))]
        (doall
         (for [[k args] change]
           (rf/dispatch [k (:value node) form-path ppth args])))))
    (if (map? node)
      (reduce-kv (fn [path k v]
                   (fire-on-change form-path form (conj path k))
                   path)
                 (if (or (= :collection (:type node)) (= :form (:type node)))
                   (conj (or path []) :value))
                 (if (or (= :collection (:type node)) (= :form (:type node)))
                   (:value node))))))

(rf/reg-event-fx
 :zf/fire-on-change
 (fn [{db :db} [_ form-path]]
   (fire-on-change form-path (get-in db form-path))
   {}))

(defn *on-value-set [node form-path path]
  (let [v (*get-value node)
        errs (validate-node node v)]
    (doall
     (for [[k args] (:on-change node)]
       (rf/dispatch [k v form-path path args])))
    (cond-> (dissoc node :errors)
      errs (assoc :errors errs))))

(defn *on-value-set-loop [form form-path path]
  (loop [form form path path]
    (if (nil? path)
      (*on-value-set form form-path path)
      (recur (update-in form (get-node-path path) #(*on-value-set % form-path path))
             (butlast path)))))

(defn *set-value [form form-path path value & [type]]
  (let [value (if (and (string? value) (str/blank? value)) nil value)
        form (assoc-in form (if (= type :collection)
                              (get-node-path path)
                              (get-value-path path)) value)
        ] form))

(defn set-value
  "Put value for specific path; run validations"
  [form form-path path value & [type]]
  (let [value (if (and (string? value) (str/blank? value)) nil value)
        form (assoc-in form (if (= type :collection)
                              (get-node-path path)
                              (get-value-path path)) value)]
    (*on-value-set-loop (*set-value form form-path path value type) form-path path )))


(defn raw-value
  "Return raw form value"
  [v]
  (clojure.walk/prewalk
   (fn [x]
     (if (and (map? x) (:value x))
       (:value x)
       x)) v))

;; this fn will fuck your brain
;; it evals all validators and collect errors
;; at the same time collect value
;; intended to be used at submit
(defn *eval-form [{tp :type v :value :as node}]
  (if (or (= tp :collection) (= tp :form))
    (let [{v :value :as res}
          (reduce (fn [res [idx n]]
                    (let [{v :value err :errors ch-node :form} (*eval-form n)
                          res (if (empty? err) res
                                  (update res :errors (fn [es]
                                                        (reduce (fn [es [err-k err-v]]
                                                                  (assoc es (into [idx] err-k) err-v))
                                                                es err))))
                          res (-> res (assoc-in [:form :value idx] ch-node))]
                      (cond-> res
                        (and (not (nil? v)) (= tp :collection)) (update :value conj v)
                        (and (not (nil? v)) (= tp :form))       (assoc-in [:value idx] v)))
                    ) {:value   (if (= tp :form) {} []) :errors  {} :form node} v)
          errs (validate-node node v)]
      (cond-> (update res :value (fn [x] (when-not (empty? x) x)))
        errs (assoc-in [:errors []] errs)
        errs (assoc-in [:form :errors] errs)))

    (let [errs (validate-node node v)
          node (cond-> (assoc node :touched true) errs (assoc :errors errs))]
      (cond-> {:value v :form node}
        errs (assoc :errors {[] errs})))))

(defn eval-form [form]
  (*eval-form form))

(rf/reg-event-db
 :zf/init
 (fn [db [_ form-path schema value]]
   (assoc-in db form-path (form schema value))))

(rf/reg-event-db
 :zf/set-value
 (fn [db [_ form-path path v]]
   (update-in db form-path (fn [form] (set-value form form-path path v)))))

(rf/reg-sub
 :zf/collection-indexes
 (fn [db [_ form-path path]]
   (-> db
       (get-in form-path)
       (get-in (get-node-path path))
       :value keys)))

(rf/reg-sub
 :zf/node
 (fn [db [_ form-path path]]
   (-> db
       (get-in form-path)
       (get-in (get-node-path path)))))

(rf/reg-sub
 :zf/form
 (fn [db [_ form-path]]
   (get-in db form-path)))

(rf/reg-sub
 :zf/get-value
 (fn [db [_ form-path & [path]]]
   (let [form (get-in db form-path)]
     (if path
       (get-value form path)
       (get-value form)))))

(defn add-collection-item [form form-path path v]
  (let [node (get-in form (get-node-path path))]
    (if (= :collection (:type node))
      (let [coll (:value node)
            idx (if (empty? coll)
                  0
                  (inc (first (apply max-key key coll))))
            sch (:item node)
            v (*form sch [] (or v {}))]
        (*on-value-set-loop (*set-value form form-path (conj path idx) v (:type node)) (get-in form (get-node-path path)) path))
      form)))

(defn remove-collection-item [form path idx]
  (let [node-path (get-node-path path)]
    (if (= :collection (get-in form (conj node-path :type)))
      (*on-value-set-loop (update-in form (conj node-path :value) dissoc idx) node-path path)
      form)))

(defn set-collection [form path v]
  (let [node (get-in form (get-node-path path))]
    (if (= :collection (:type node))
      (set-value form path (*form node [] (or v {})) (:type node))
      form)))

(rf/reg-event-db
 :zf/add-collection-item
 (fn [db [_ form-path path v]]
   (update-in db form-path (fn [form] (add-collection-item form form-path path v)))))

(rf/reg-event-db
 :zf/remove-collection-item
 (fn [db [_ form-path path idx]]
   (update-in db form-path (fn [form] (remove-collection-item form path idx)))))

(rf/reg-event-db
 :zf/set-collection
 (fn [db [_ form-path path v]]
   (update-in db form-path (fn [form] (set-collection form path v)))))

(rf/reg-sub
 :zf/collection
 (fn [[_ form-path path] _]
   (rf/subscribe [:zf/node form-path path]))
 (fn [node _]
   (:value node)))

(defn get-full-path
  [form-path path]
  (into form-path (get-node-path path)))

(rf/reg-event-fx
 :zf/update-node-schema
 (fn [{db :db} [_ form-path path value]]
   {:db (update-in db
                   (get-full-path form-path path)
                   #(merge % value))}))

