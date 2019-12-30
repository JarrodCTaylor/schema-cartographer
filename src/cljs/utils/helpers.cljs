(ns cljs.utils.helpers
  (:require [re-frame.core :as rf]))

(defn remove-matching-uuid [elements uuid]
  (remove #(= uuid (:uuid %)) elements))

(def <sub (comp deref rf/subscribe))
(def >dis rf/dispatch)

(defn empty-form-field
  [{:keys [error-message required? default-error-checker]
    :or {default-error-checker :non-empty-string}}]
  {:value nil
   :dirty? false
   :default-error-checker default-error-checker
   :required? required?
   :has-error? false
   :error-message error-message})