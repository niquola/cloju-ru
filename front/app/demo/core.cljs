(ns app.demo.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [app.pages   :as pages]
            [app.demo.model :as model]))

(defn counter []
  (let [m @(rf/subscribe [:counter])]
    [:div
     [:h3 (:display m)
      [:button.btn.btn-success
       {:on-click #(rf/dispatch [:click])}
       "Click Me"]]]))

(defn index [params]
  (let [m @(rf/subscribe [:demo/index])]
    [:div#welcome.container
     [:div
      [:h3 "Hello"]
      [counter]]]))

(pages/reg-page :demo/index index)

(comment
  (js/alert "Hello")
  (.. js/window -location -href)

  )
