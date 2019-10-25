(ns app.welcome.core
  (:require [re-frame.core :as rf]
            [app.routes  :refer [href]]
            [app.styles  :as styles]
            [app.pages   :as pages]
            [app.welcome.model :as model]))

(def styles
  (styles/styles
   [:div#welcome
    [:.container {:width "900px" :margin "0 auto"}]
    [:.search {:display "block"
               :border "2px solid #777"
               :width "100%" :padding "5px"}]
    [:.items
     [:.item {:display "block"
              :padding "10px 0"
              :text-decoration "none"
              :color "#444"
              :border-bottom "1px solid #f1f1f1"}
      [:&:hover {:background-color "#f9f9f9"}]
      [:img.img {:margin-right "2em"
                 :height "50px"
                 :width "50px"
                 :background-color "#eee"
                 :border-radius "4px"}]]]]))

(defonce debounce-events (atom {}))

(defn debounce-dispatch [[nm & rest :as ev] ms]
  (when-let [h (get @debounce-events nm)]
    (js/clearTimeout h))
  (swap! debounce-events assoc nm (js/setTimeout (fn [] (rf/dispatch ev)) ms)))

(defn do-search [ev]
  (debounce-dispatch [::model/search (.. ev -target -value)] 500))

(defn index [params]
  (let [m @(rf/subscribe [:welcome/index])]
    [:div#welcome.container
     styles
     [:div
      [:input.search {:on-change do-search}]
      (when-let [msg (get-in m [:grid :message])]
        [:div msg])
      (when-let [items (get-in m [:grid :items])]
        (into
         [:div.items]
         (for [{id :id href :href disp :display :as item} items]
           [:a.item {:key id :href href}
            [:img.img {:src (:img item)}]
            [:span.display disp]])))]]))

(pages/reg-page :welcome/index index)

(comment
  (js/alert "Hello")
  (.. js/window -location -href)

  )
