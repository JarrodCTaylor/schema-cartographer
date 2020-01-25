(ns client.components.validated-input
  (:require
    ["react-select" :default react-select]
    [client.utils.helpers :refer [<sub >dis tip]]
    [client.events :as shared-events]
    [client.subs :as shared-subs]))

(defn text [{:keys [label route form field tool-tip color-scheme additional-dom]}]
  (let [{:keys [has-error? error-message value]} (<sub [::shared-subs/form-field route form field])]
    [:div.field
     [:label.label label (when tool-tip
                           [tip
                            [:img {:src (if (= "dark" color-scheme) "img/info-dark.svg" "img/info-light.svg")
                                   :style {:height "15px" :top "2px" :position "relative" :left "5px" :margin-top "-5px"}}]
                            tool-tip "top"])]
     (when additional-dom
       additional-dom)
     [:input.input {:type "text"
                    :class (when has-error? "input-error")
                    :value value
                    :placeholder label
                    :on-change #(>dis [::shared-events/set-form-field-value route form field (-> % .-target .-value)])
                    :on-blur #(>dis [::shared-events/set-form-field-dirty route form field])}]
     (when has-error? [:span.error-message error-message])]))

(defn select-box [{:keys [options is-multi label route form field color-scheme tool-tip on-change additional-dom disabled? is-clearable?]}]
  (let [{:keys [has-error? error-message value]} (<sub [::shared-subs/form-field route form field])
        color-1 (if (= "dark" color-scheme) "#BBC0C7" "#464B52")
        color-2 (if (= "dark" color-scheme) "#464B52" "#EDF2F7")
        color-3 (if (= "dark" color-scheme) "#535861" "#FFFFFF")]
    [:div.field
     [:label.label label (when tool-tip
                           [tip
                            [:img {:src (if (= "dark" color-scheme) "img/info-dark.svg" "img/info-light.svg")
                                   :style {:height "15px" :top "2px" :position "relative" :left "5px" :margin-top "-5px"}}]
                            tool-tip "top"])]
     (when additional-dom
       additional-dom)
     [:> react-select {:class-name "react-select-container"
                       :class-name-prefix "react-select-style-prefix"
                       :value value
                       :is-multi is-multi
                       :options (clj->js options)
                       :on-change (or on-change #(>dis [::shared-events/set-form-field-value route form field (js->clj %)]))
                       :is-disabled disabled?
                       :is-clearable is-clearable?
                       :on-blur #(>dis [::shared-events/set-form-field-dirty route form field])
                       :styles {:option (fn [provided state]
                                          (let [state-map (js->clj state)
                                                provided-map (js->clj provided)
                                                new-map (merge
                                                          provided-map
                                                          {"color" (cond
                                                                     (get state-map "isSelected") color-2
                                                                     (get state-map "isFocused") color-1
                                                                     :else color-1)}
                                                          {"backgroundColor" (cond
                                                                               (get state-map "isSelected") color-1
                                                                               (get state-map "isFocused") color-2
                                                                               :else "transparent")
                                                           "cursor" (if (get state-map "isSelected") "default" "pointer")
                                                           ":active" {"backgroundColor" color-2}})]
                                            (clj->js new-map)))
                                :menu (fn [provided state]
                                        (let [provided-map (js->clj provided)]
                                          (clj->js (merge provided-map
                                                          {:backgroundColor color-3
                                                           :borderRadius 2
                                                           :left "10px"}))))}}]
     (when has-error? [:span.error-message error-message])]))
