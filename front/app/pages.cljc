(ns app.pages)

(defonce pages (atom {}))


(defn reg-page [key page]
  (swap! pages assoc key page))

