(ns cljs.components.alerts
  (:require
    [cljs.utils.helpers :refer [<sub >dis]]
    [cljs.events :as shared-events]
    [cljs.subs :as shared-subs]))

(defn alert-display []
  (let [alerts (<sub [::shared-subs/alerts])]
    [:div#alerts
     (for [alert alerts]
       [:div.notification {:key (:uuid alert)
                           :class (str "is-" (:type alert))}
        [:button.delete {:on-click #(>dis [::shared-events/remove-alert (:uuid alert)])}]
        (:message alert)])]))
