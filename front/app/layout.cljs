(ns app.layout
  (:require [re-frame.core :as rf]
            [app.styles]
            [app.routes :refer [href]]))

(rf/reg-sub
 ::navigation
 (fn [db _]
   {:title "INC"
    :menu [{:href (href "") :display "Home"}
           {:href (href "new") :display "New"}]}))

(def style (app.styles/styles
            [:nav
             {:padding "5px 0"
              :margin-bottom "10px"
              :border-bottom "1px solid #ddd"}
             [:.nav-item {:display "inline-block"
                          :padding "5px"}]]))

(defn menu [m]
  [:nav
   (for [i m]
     [:a.nav-item
      {:key (:href i)
       :href (:href i)}
      (:display i)])])

(defn layout []
  (let [model (rf/subscribe [::navigation])]
    (fn [cnt]
      [:div.container-fluid 
       style
       [menu (:menu @model)]
       cnt])))
