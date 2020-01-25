(ns client.components.alerts
  (:require
    [client.utils.helpers :refer [<sub >dis]]
    [client.events :as shared-events]
    [client.subs :as shared-subs]))

(defn alert-display []
  (let [alerts (<sub [::shared-subs/alerts])]
    [:div#alerts
     (for [alert alerts]
       [:div.notification {:key (:uuid alert)
                           :class (str "is-" (:type alert))
                           :on-click #(>dis [::shared-events/remove-alert (:uuid alert)])}
        (:message alert)])]))
