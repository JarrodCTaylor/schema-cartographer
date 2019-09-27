(ns client.subs
  (:require
    [re-frame.core :as rf]))


; region = Register Subscriptions =============================================
;; https://github.com/Day8/re-frame/blob/master/docs/Loading-Initial-Data.md#the-pattern
(rf/reg-sub
  ::initialized?
  (fn [db _]
    (and (not (empty? db))
         true)))

(rf/reg-sub
  ::active-route
  (fn [db _]
    (:active-route db)))

(rf/reg-sub
  ::alerts
  (fn [db _]
    (:alerts db)))

; region - Forms --------------------------------------------------------------
;; Project convention is for route forms to live in the db at [:routes <route-name> <form-name>]
;; The form is a map with the shape of:
;; {:field-name {:value ""
;;               :dirty? false
;;               :required? true
;;               :has-error? false
;;               :error-message "You must provide a value"}}
;; This allows the reuse of the following subs for project wide access to form values and errors.

(rf/reg-sub
  ::form
  (fn [db [_ route-name form-name]]
    (-> db :routes route-name form-name)))

(rf/reg-sub
  ::form-field
  (fn [[_ route-name form-name _]]
    [(rf/subscribe [::form route-name form-name])])
  (fn [[form] [_ _ _ field-name]]
    (field-name form)))
; endregion

(rf/reg-sub
  ::colors
  (fn [db _]
    (let [color-scheme (-> db :routes :index :settings :color-scheme)]
      (case color-scheme
        "dark" {:color-1  "#282a36"
                :color-2  "#44475a"
                :color-3  "#ff79c6"
                :color-4  "#f8f8f2"
                :color-5  "#f1fa8c"}
        "light" {:color-1  "#0A0909"
                 :color-2  "#CFC6BB"
                 :color-3  "#CC241D"
                 :color-4  "#E7DFD6"
                 :color-5  "#0A0909"}))))
; endregion
