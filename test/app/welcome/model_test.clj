(ns app.welcome.model-test
  (:require [app.welcome.model :as subj]
            [app.camps.model]
            [re-frame.core :as rf]
            [tsys]
            [matcho.core :as matcho]
            [clojure.test :refer :all]))

(defn not-empty? [x] (not (empty? x)))
(defn not-nil? [x] (not (nil? x)))

(deftest db-test

  ;; (tsys/restart-app)

  (tsys/ensure-app)

  (tsys/truncate :camps)

  (tsys/reset-app-db)
  (tsys/open "/")

  ;; (rf/dispatch [:welcome/index :init {}])

  (def page (rf/subscribe [:welcome/index]))

  (matcho/match
   @page
   {:grid {:message "Nothing to show"}}) 

  (tsys/create-resource :camps {:id "mt" :display "Monad transformers"})
  (tsys/create-resource :camps {:id "vjs" :display "Vui.js futurre"})

  (tsys/re-open "/")

  (matcho/match
   @page
   {:grid {:items [{:id "mt" :display string? :href "#/camps/mt"}
                   {:id "vjs" :display string? :href "#/camps/vjs"}]}})

  (rf/dispatch [::subj/search "vui"])

  (matcho/match
   @page
   {:grid {:items [{:id "vjs" :display string? :href "#/camps/vjs"}]}})

  (tsys/match
   {:uri "/camps"
    :params {:q "vu"}}
   {:status 200
    :body [{:id "vjs"}]})


  (tsys/re-open "/camps/vjs")

  (def show-page (rf/subscribe [:camps/show]))

  (matcho/match
   @show-page
   {:camp {:id "vjs"}})

  )
