(ns zenform.validators
  (:require [clojure.string :as str]))


(defmulti validate (fn [cfg value] (:type cfg)))

(defmethod validate
  :min-length
  [{limit :value msg :message} v]
  (when (string? v)
    (when (< (count v) limit)
      (or msg (str "Shouldn't be shorter then " limit)))))

(defmethod validate
  :min-items
  [{limit :value msg :message} v]
  (when (coll? (or v []))
    (when (< (count v) limit)
      (or msg (str "Shouldn't be shorter then " limit)))))

(defmethod validate
  :number
  [{msg :message} v]
  (when-not
      (and (not (if (string? v)
                  (str/blank? v)
                  (empty? v)))
           (or (>= v 0) (<= v 0)))
    (or msg "Should be a number")))

(defmethod validate
  :required
  [{msg :message} v]
  (when (or (nil? v)
            (and (string? v) (str/blank? v))
            (and (map? v) (empty? v))
            (and (sequential? v) (empty? v)))
    (or msg "Should not be blank")))

(def email-regex #".+?\@.+?\..+")

(defmethod validate
  :email
  [{msg :message} v]
  (when v
    (when-not (re-matches email-regex v)
      (or msg "Should be valid email"))))

(defmethod validate
  :pattern
  [{rx :regex msg :message} v]
  (when v
    (when-not (re-matches rx v)
      (or msg (str "Should match " rx)))))

