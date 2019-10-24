(ns app.<page>.core
  (:require [re-frame.core :as rf]
            [app.routes :refer [href]]
            [app.styles :as styles]
            [app.pages :as pages]
            [app.helpers :as helpers]
            [app.<page>.model :as model]))

(def styles
  (styles/styles
   [:div#<page>]))


(defn index [params]
  [:div#<page>.centered-content.crud
   styles
   [:h1 "<page>"]])

(pages/reg-page :<page>/index index)


