(ns client.events
  (:require
    [clojure.string :as string]
    [re-frame.core :as rf]
    [client.config :as config]
    [client.interceptors :refer [standard-interceptors]]
    [client.utils.helpers :as helpers]
    [client.db :as db]))


; region = Register CoFx =======================================================

; Usage: When registering a event (rf/inject-cofx ::uuid) as an interceptor
(rf/reg-cofx
  ::uuid
  (fn [coeffects _]
    (assoc coeffects :uuid (random-uuid))))
; endregion

; region = Register Effect Handlers ============================================

; Usage: In the returned map of a reg-event-fx {:save-in-local-storage {:key1 value1 :key2 value2}}
(rf/reg-fx
  ::save-in-local-storage
  (fn [map-to-store]
    (doseq [[k v] map-to-store]
      (.setItem js/localStorage (name k) v))))

; Usage: In the returned map of a reg-event-fx {::remove-from-local-storage [:user :username]}
(rf/reg-fx
  ::remove-from-local-storage
  (fn [keys]
    (doseq [k keys]
      (.removeItem js/localStorage (name k)))))

(rf/reg-fx
  ::set-color-scheme-class
  (fn [[color-scheme background-color]]
    (let [html (aget (js/document.getElementsByTagName "html") 0)]
      (set! (.-className html) color-scheme)
      (set! (.. html -style -backgroundColor) background-color))))
; endregion

; region = Register Events =====================================================

; region -- App Start Up -------------------------------------------------------
(defn get-from-ls
  "Attempt to retrieve ls-key from localStorage. If value is equal to 'null' return nil"
  [ls-key]
  (let [value (.getItem js/localStorage ls-key)]
    (if (= "null" value) nil value)))

; Description: Retrieve the existing localStorage information and inject it into the event.
(rf/reg-cofx
  ::local-store
  (fn [cofx _]
    (-> cofx
        (assoc :cartographer-ls-system (get-from-ls "cartographer-ls-system"))
        (assoc :cartographer-ls-region (get-from-ls "cartographer-ls-region"))
        (assoc :cartographer-ls-database-name (get-from-ls "cartographer-ls-database-name"))
        (assoc :cartographer-ls-color-scheme (or (get-from-ls "cartographer-ls-color-scheme") "dark"))
        (assoc :cartographer-ls-background-color (or (get-from-ls "cartographer-ls-background-color") "#282A36"))
        (assoc :cartographer-ls-collapse-details? (get-from-ls "cartographer-ls-collapse-details?"))
        (assoc :cartographer-ls-display-as-keywords? (get-from-ls "cartographer-ls-display-as-keywords?")))))


(rf/reg-event-fx
  ::initialize-db
  [(rf/inject-cofx ::local-store)
   standard-interceptors]
  (fn [{:keys [cartographer-ls-system cartographer-ls-region cartographer-ls-database-name
               cartographer-ls-color-scheme cartographer-ls-background-color
               cartographer-ls-display-as-keywords? cartographer-ls-collapse-details?]} _]
    {:db (-> db/default-db
             (assoc-in [:routes :index :load-schema-form :system :value] cartographer-ls-system)
             (assoc-in [:routes :index :load-schema-form :region :value] cartographer-ls-region)
             (assoc-in [:routes :index :load-schema-form :database-name :value] cartographer-ls-database-name)
             (assoc-in [:routes :index :settings :color-scheme] cartographer-ls-color-scheme)
             (assoc-in [:routes :index :settings :background-color] cartographer-ls-background-color)
             (assoc-in [:routes :index :settings :collapse-details?] (case cartographer-ls-collapse-details?
                                                                       "true" true
                                                                       "false" false
                                                                       nil true))
             (assoc-in [:routes :index :settings :display-as-keywords?] (case cartographer-ls-display-as-keywords?
                                                                          "true" true
                                                                          "false" false
                                                                          nil false)))}))
; endregion

; region -- Routing ------------------------------------------------------------
(defn update-events-with-actual-params
  "Events defined in the router to be dispatched when a route is navigated to
   use `:query-params` and `:path-params` as place holders for the actual values
   defined later."
  [events path query]
  (reduce (fn [res event]
            (conj res (mapv (fn [el]
                              (case el
                                :query-params query
                                :path-params path
                                el))
                            event)))
          []
          events))

(rf/reg-event-fx
  ::dispatch-route-events
  [standard-interceptors]
  (fn [_ [_ {:keys [path-params query-params] :as active-route}]]
    (let [route-initialization-events (get-in active-route [:data :dispatch-on-entry] [])]
      {:dispatch-n (update-events-with-actual-params route-initialization-events path-params query-params)})))

; Usage: (dispatch [::shared-events/set-active-route (rfront/match-by-path router/routes uri)])
; Description: Change the active reitit route for the app
(rf/reg-event-fx
  ::set-active-route
  [standard-interceptors]
  (fn [{:keys [db]} [_ {:keys [path-params query-params] :as route}]]
    {:db (-> db
             (assoc :active-route route)
             (assoc :path-params path-params)
             (assoc :query-params query-params))}))
; endregion

; region -- Alerts -------------------------------------------------------------

; Usage: ::add-alert and ::remove-alert are typically used at the same time.
;        In a reg-event-fx like so:
;        {:dispatch       [::add-alert (:uuid cofx) "Alert body here" "Alert Type ie: danger, info, etc"]
;         :dispatch-later [{:ms config/alert-timeout-ms
;                           :dispatch [::remove-alert (:uuid cofx)]}]}
; Description: Extends the top level `:alerts` vector with a alert map {:uuid #uuid :type "..." :message "..."}
(rf/reg-event-db
  ::add-alert
  [standard-interceptors]
  (fn [db [_ uuid message alert-type]]
    (update db :alerts merge {:uuid uuid :message message :type alert-type})))

; Description: Removes the alert with the matching uuid from the top-level
;              `:alerts` vector. See above for additional detail
(rf/reg-event-db
  ::remove-alert
  [standard-interceptors]
  (fn [db [_ uuid]]
    (update db :alerts helpers/remove-matching-uuid uuid)))

(rf/reg-event-fx
  ::dispatch-alert-danger
  [(rf/inject-cofx ::uuid) standard-interceptors]
  (fn [{:keys [uuid]} [_ message]]
    {:dispatch [::add-alert uuid message "danger"]
     :dispatch-later [{:ms config/alert-timeout-ms
                       :dispatch [::remove-alert uuid]}]}))
; endregion

; region -- Form Validation ----------------------------------------------------

; This method should be extended as needed for any custom field validation.
; Define as `(defmethod shared-events/form-field-error? :<FORM-NAME>-<FIELD-NAME> [m] (bool ...))` The
; function will receive a map with the following shape to use in determining if
; the field is valid: {:field-name ..
;                      :form ..
;                      :default-error-checker ..
;                      :value ..
;                      :dirty? ..
;                      :form-name ..}
; Any field can provide a keyword matching an existing error-checking method as a value to `:default-error-checker`
; and that will be used. If using a custom checker ensure that `:default-error-checker` is not defined
(defmulti form-field-error? (fn [{:keys [form-name field-name default-error-checker]}]
                              (cond
                                default-error-checker default-error-checker
                                :else (->> [form-name "-" field-name]
                                           (map name)
                                           (string/join "")
                                           keyword))))

(defmethod form-field-error? :non-empty-string [{:keys [value dirty?]}]
  (and dirty? (string/blank? value)))

(defmethod form-field-error? :password [{:keys [value dirty?]}]
  (and dirty?
       (or
         (string/blank? value)
         (<= (count value) 7))))

(defmethod form-field-error? :empty-or-number [{:keys [value dirty?]}]
  (and dirty?
       (and
         (not (string/blank? value))
         (not (re-matches #"\d\.?\d*" value)))))

; Usage: [::set-form-field-value :form-name :field-name "new value"]
; Description: Sets the value of the provided field in the form. The field is
;              considered dirty once this happens and it's error status will be updated.
(rf/reg-event-db
  ::set-form-field-value
  [standard-interceptors]
  (fn [db [_ route-name form-name field-name value]]
    (let [form-path [:routes route-name form-name]
          form (get-in db form-path)
          dirty? (-> form field-name :dirty?)
          default-error-checker (-> form field-name :default-error-checker)
          has-error? (form-field-error? {:default-error-checker default-error-checker
                                         :field-name field-name
                                         :form form
                                         :value value
                                         :dirty? dirty?
                                         :form-name form-name})]
      (-> db
          (assoc-in (into form-path [field-name :value]) value)
          (assoc-in (into form-path [field-name :has-error?]) has-error?)))))

; Usage: [::set-form-field-dirty :form-name :field-name]
; Description: Sets the value of :dirty? to true for the provided field in the form.
(rf/reg-event-db
  ::set-form-field-dirty
  [standard-interceptors]
  (fn [db [_ route-name form-name field-name]]
    (let [form-path [:routes route-name form-name]
          form (get-in db form-path)
          value (-> form field-name :value)
          default-error-checker (-> form field-name :default-error-checker)
          has-error? (form-field-error? {:default-error-checker default-error-checker
                                         :field-name field-name
                                         :form form
                                         :value value
                                         :dirty? true
                                         :form-name form-name})]
      (-> db
          (assoc-in (into form-path [field-name :dirty?]) true)
          (assoc-in (into form-path [field-name :has-error?]) has-error?)))))

(defn invalidator-reducer
  "Reducing function used to build a new form map when invalidating a form prior to submission.
  Marks fields as dirty and having an error if they begin as both required and clean"
  [res [k v]]
  (let [clean-and-required? (and (not (:dirty? v)) (:required? v))
        new-value (if clean-and-required?
                    (assoc v
                           :dirty? true
                           :has-error? (form-field-error? {:field-name k
                                                           :default-error-checker (:default-error-checker v)
                                                           :form (:form res)
                                                           :value (:value v)
                                                           :dirty? true
                                                           :form-name (:form-name res)}))
                    v)]
    (merge res {k new-value})))

; Usage: [::submit-if-form-valid :route-name :form-name [:event-to-dispatch]]
; Description: Checks to see if the named form is valid. A valid form is considered
;              to be one that does not have any known errors or required fields which are
;              not dirty. If form is in a valid state. Events are dispatched.
(rf/reg-event-fx
  ::submit-if-form-valid
  [standard-interceptors]
  (fn [{:keys [db]} [_ route-name form-name & events]]
    (let [form-path [:routes route-name form-name]
          form (get-in db form-path)
          invalidated-form (-> (reduce invalidator-reducer {:form-name form-name :form form} form)
                               (dissoc :form-name :form))
          required-fields (filter :required? (vals invalidated-form))
          form-valid? (every? #(and (:dirty? %) (not (:has-error? %))) required-fields)]
      (merge {:db (assoc-in db form-path invalidated-form)}
             (when form-valid? {:dispatch-n (into [] events)})))))
; endregion

; region -- Color Schemes ------------------------------------------------------
(rf/reg-event-fx
  ::set-color-scheme
  (fn [_ [_ color-scheme background-color]]
    {::set-color-scheme-class [(name color-scheme) background-color]}))
; endregion
; endregion

