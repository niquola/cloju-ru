(ns app.camps.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [app.pages   :as pages]
            [app.camps.model :as model]
            [clojure.string :as str]))

(def styles
  (styles/styles
   [:div#welcome
    [:.container {:width "900px" :margin "0 auto"}]]))

(defn show [params]
  (let [{camp :camp :as m} @(rf/subscribe [model/show-key])]
    [:div#welcome.container
     styles
     (when camp
       [:div 
        [:h1 [:a {:href "#/" } "<"] " " (:display camp)]])]))


(defn errors [form-path path]
  (let [node (rf/subscribe [:zf/node form-path path])]
    (fn [& _]
      (let [errs (:errors @node)]
        [:div.invalid-feedback {:style {:display "block" :visibility "visible"}} (str/join ", " (vals errs))]))))

(defn text-input [form-path path & [attrs]]
  (let [node (rf/subscribe [:zf/node form-path path])
        attrs (assoc attrs :on-change #(rf/dispatch [:zf/set-value form-path path (.. % -target -value)]))]
    (fn [& _]
      (let [*node @node
            v (:value *node)
            errs (:errors *node)]
        [:input.form-control
         (-> attrs
             (assoc :value v)
             (update :class (fn [class] (str class (when errs " is-invalid") ))))]))))

(defn textarea-input [form-path path & [attrs]]
  (let [node (rf/subscribe [:zf/node form-path path])
        attrs (assoc attrs :on-change #(rf/dispatch [:zf/set-value form-path path (.. % -target -value)]))]
    (fn [& _]
      (let [*node @node
            v (:value *node)
            errs (:errors *node)]
        [:textarea.form-control
         (-> attrs
             (assoc :value v)
             (update :class (fn [class] (str class (when errs " is-invalid") ))))]))))


(defn debug-form []
  (let [m @(rf/subscribe [:zf/get-value model/form-path])]
    [:pre (pr-str m)]))

(defn new [params]
  (let [m @(rf/subscribe [model/new-key])]
    [:div#welcome.container
     styles
     [:div
      [:h1 "New Camp"]
      [:hr]
      [:div.form-row
       [:lable "Display"]
       [text-input model/form-path [:display]]
       [errors model/form-path [:display]]]
      [:div.form-row
       [:lable "Summary"]
       [textarea-input model/form-path [:summary]]
       [errors model/form-path [:summary]]]
      [:hr]
      [:div.form-row
       [:button.btn.btn-success {:on-click #(rf/dispatch [:camps/create])}
        "Save"]]]
     #_[debug-form]]))

(pages/reg-page model/show-key show)
(pages/reg-page :camps/new new)

