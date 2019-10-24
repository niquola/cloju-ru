(ns app.db.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [reagent.core :as r]
            [app.pages   :as pages]
            [app.helpers :as helpers]
            [app.db.model :as model]))

(def styles
  (styles/styles
   [:div#db
    [:.search {:width "100%"}]
    ]))

(defn search []
  (let [sub (rf/subscribe [:db/filter])
        on-search #(rf/dispatch [:db/filter-table (.. % -target -value)])]
    (fn []
      (let [m @sub]
        [:input.search {:placeholder "Search" :on-change on-search :value m}]))))

(defn index [params]
  (let [m (rf/subscribe [model/page-key])]
    (fn []
      (let [*m @m]
        [:div#db.centered-content
         styles
         [search]
         ;; [:input.search {:placeholder "Search" :on-change on-search :value (:search *m)}]
         (if (= (:status *m) :progress)
           [:center
            [:div.loader "Loading..."]]
           (let [table (:tables *m)]
             [:table.table
              [:thead
               (into [:tr]
                     (for [c (:columns table)]
                       [:th {:id (:id c)} (:title c)]))]
              [:tbody 
               (for [{id :id vals :vals} (:rows table)]
                 (into
                  [:tr {:key id}]
                  (for [v vals]
                    [:td {:key (:id v)}
                     (:value v)])))]]))]))))

(pages/reg-page model/page-key index)
