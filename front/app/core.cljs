(ns app.core
  (:require [clojure.string :as str]
            [reagent.core :as reagent]
            [re-frame.core :as rf]

            [zframes.routing]
            [zframes.redirect]
            [zframes.xhr]
            [zframes.debounce]

            [app.routes]
            [app.layout]

            [app.pages]
            [app.welcome.core]
            [garden.core :as garden]))

(defn style [css]
  [:style (garden/css css)])

(def styles
  (style [[:.grey {:background-color "#f1f1f1"}]]))

(defn not-found-page [page]
  [:div "Not found" page])

(defn current-page []
  (let [route @(rf/subscribe [:route-map/current-route])
        params (:params route)

        page   (get @app.pages/pages (:match route))]
    [:div
     [:style styles]
     [app.layout/layout 
      (if page
        [page params]
        [not-found-page (pr-str route)])]]))

(rf/reg-event-fx
 ::initialize
 [(rf/inject-cofx :window-location)]
 (fn [{location :location db :db :as cofx} _]
   {:dispatch [:route-map/init app.routes/routes]}))

(defn mount-root []
  (rf/dispatch [::initialize])
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
