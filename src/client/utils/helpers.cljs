(ns client.utils.helpers
  (:require
    [clojure.string :as str]
    [re-frame.core :as rf]
    [reagent.core :as r]
    ["tippy.js" :default tippy]))

(defn remove-matching-uuid [elements uuid]
  (remove #(= uuid (:uuid %)) elements))

(def <sub (comp deref rf/subscribe))
(def >dis rf/dispatch)

(defn empty-form-field
  [{:keys [error-message required? validator-fn]}]
  {:value nil
   :dirty? false
   :validator-fn validator-fn
   :required? required?
   :has-error? false
   :error-message error-message})

(defn tip [el content placement]
  (r/create-class {:reagent-render (fn [] el)
                   :component-did-mount (fn [this]
                                          (tippy (r/dom-node this) (clj->js {:content content
                                                                             :placement placement})))}))

(defn title->kebab
  "(title->kebab 'User Document Id') ;=> 'user-document-id'"
  [s]
  (as-> s _
        (str/split _ #"\s")
        (map str/lower-case _)
        (str/join "-" _)))

(defn ns-bucket
  [the-ns]
  (let [ns (try (namespace the-ns)
                (catch js/Error _ nil))]
    (case ns
      "db.schema.ident.namespace" [:idents :ns-idents]
      "db.schema.entity.namespace" [:entities :ns-entities]
      nil)))
