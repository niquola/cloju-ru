(ns app.welcome.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [app.pages   :as pages]
            [app.welcome.model :as model]))

(def styles
  (styles/styles
   [:div#welcome]))

(defn index [params]
  (let [m @(rf/subscribe [:welcome/index])]
    [:div#welcome.centered-content
     styles
     [:div [:h1 "Hello"]
      [:pre (pr-str m)]]]))

(pages/reg-page :welcome/index index)

(comment
  (js/alert "Hello")
  (.. js/window -location -href)

  )
