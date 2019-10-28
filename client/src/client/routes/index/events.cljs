(ns client.routes.index.events
  (:require
    ["@fnando/sparkline/dist/sparkline.commonjs2" :refer (sparkline)]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [client.config :refer [api-url]]
    [client.events :as shared-events]
    [client.interceptors :refer [standard-interceptors]]))

(rf/reg-fx
  ::set-transition-height
  (fn []
    (let [elms-entering (js/document.getElementsByClassName "collapsible")]
      (doseq [n (range (.-length elms-entering))]
        (set! (.-maxHeight (.-style (aget elms-entering n))) (str (.-scrollHeight (aget elms-entering n)) "px"))))))

(rf/reg-event-fx
  ::set-transition-max-height
  [standard-interceptors]
  (fn [_ _]
    {::set-transition-height []}))

; region = Namespace Selection =================================================
(rf/reg-event-fx
  ::select-ns
  [standard-interceptors]
  (fn [{:keys [db]} [_ ns]]
    {:db (-> db
             (assoc-in [:routes :index :currently-selected-ns] ns)
             (assoc-in [:routes :index :previously-selected-ns] []))
     :dispatch [::hide-analytic-info]
     ::set-transition-height []}))

(rf/reg-event-fx
  ::select-breadcrumb-ns
  [standard-interceptors]
  (fn [{:keys [db]} [_ ns]]
    {:db (-> db
             (assoc-in [:routes :index :currently-selected-ns] ns)
             (update-in [:routes :index :previously-selected-ns] (fn [selected-ns] (into [] (take-while #(not= ns %) selected-ns)))))
     :dispatch [::hide-analytic-info]}))

(rf/reg-event-fx
  ::select-ns-and-push-previous
  [standard-interceptors]
  (fn [{:keys [db]} [_ ns]]
    (let [previously-selected (-> db :routes :index :currently-selected-ns)]
      {:db (-> db
               (assoc-in [:routes :index :currently-selected-ns] ns)
               (update-in [:routes :index :previously-selected-ns] conj previously-selected))
       :dispatch [::hide-analytic-info]})))
; endregion

(rf/reg-event-db
  ::update-aside-filter
  [standard-interceptors]
  (fn [db [_ filter-str]]
    (assoc-in db [:routes :index :aside-filter] filter-str)))

; region = Get/Load Schema =====================================================

(rf/reg-event-fx
  ::get-schema-success
  [standard-interceptors]
  (fn [{:keys [db]} [_ response]]
    {:db (assoc-in db [:routes :index :schema] (:schema response))}))

(rf/reg-event-fx
  ::get-schema
  [standard-interceptors]
  (fn [{:keys [db]} _]
    (let [{:keys [system region database-name]} (-> db :routes :index :load-schema-form)]
      {::shared-events/save-in-local-storage {:cartographer-ls-system (:value system)
                                              :cartographer-ls-region (:value region)
                                              :cartographer-ls-database-name (:value database-name)}
       :http-xhrio {:method :get
                    :uri (str api-url "schema")
                    :headers {"system"  (:value system)
                              "region"  (:value region)
                              "db-name" (:value database-name)}
                    :response-format (ajax/transit-response-format)
                    :on-success [::get-schema-success]
                    :on-failure [::shared-events/dispatch-alert-danger "Failure retrieving schema"]}})))

(rf/reg-event-db
  ::load-schema
  [standard-interceptors]
  (fn [db [_ schema]]
    (assoc-in db [:routes :index :schema] schema)))

(rf/reg-event-db
  ::switch-load-schema-tab
  [standard-interceptors]
  (fn [db [_ tab]]
    (assoc-in db [:routes :index :load-schema-tabs :active-tab] tab)))

(rf/reg-event-db
  ::read-only
  [standard-interceptors]
  (fn [db [_ state]]
    (assoc-in db [:routes :index :read-only?] state)))
; endregion

(rf/reg-event-fx
  ::settings
  [standard-interceptors]
  (fn [{:keys [db]} [_ setting desired-state]]
    {:db (assoc-in db [:routes :index :settings setting] desired-state)
     ::shared-events/save-in-local-storage {(keyword (str "cartographer-ls-" (name setting))) desired-state}}))

(rf/reg-event-db
  ::unload-schema
  [standard-interceptors]
  (fn [db _]
    (-> db
        (assoc-in [:routes :index :schema] nil)
        (assoc-in [:routes :index :settings :modal-visible?] false))))

; region = Analytic Info =======================================================
(rf/reg-event-db
  ::hide-analytic-info
  [standard-interceptors]
  (fn [db _]
    (assoc-in db [:routes :index :analytics :visible?] false)))

(rf/reg-event-db
  ::toggle-analytic-info-visible
  [standard-interceptors]
  (fn [db _]
    (update-in db [:routes :index :analytics :visible?] not)))

(def the-options
  (clj->js {:onmousemove (fn [event datapoint]
                           (let [svg (.querySelector js/document ".sparkline")
                                 tooltip (.-nextElementSibling svg)
                                 date (.-date datapoint)
                                 value (.-value datapoint)]
                             (set! (.-hidden tooltip) false)
                             (set! (.-textContent tooltip) (str date ": " value))
                             (set! js/tooltip.style.top (str (.-offsetY event) "px"))
                             (set! js/tooltip.style.left (str (+ 20 (.-offsetX event)) "px"))))
            :onmouseout (fn []
                          (let [svg (.querySelector js/document ".sparkline")
                                tooltip (.-nextElementSibling svg)]
                            (set! (.-hidden tooltip) true)))}))

(rf/reg-fx
  ::init-sparkline-fx
  (fn [^js data]
    (let [svg (.querySelector js/document ".sparkline")
          data (if (= 1 (count data)) ;; At least two data points must be present for the graph to render
                 (into data data)
                 data)]
      (sparkline svg (clj->js data) the-options))))

(rf/reg-event-fx
  ::init-sparkline
  [standard-interceptors]
  (fn [_ [_ data]]
    {::init-sparkline-fx data}))

(rf/reg-event-fx
  ::get-analytics-success
  [standard-interceptors]
  (fn [{:keys [db]} [_ response]]
    (let [selected-ns (-> db :routes :index :currently-selected-ns)]
      {:db (-> db
               (assoc-in [:routes :index :schema :entities selected-ns :analytics] response)
               (assoc-in [:routes :index :analytics :loading?] false))})))

(rf/reg-event-fx
  ::get-analytics-failure
  [standard-interceptors]
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:routes :index :analytics :loading?] false)
     :dispatch [::shared-events/dispatch-alert-danger "Failure retrieving analytic info"]}))

(rf/reg-event-fx
  ::get-analytic-info
  [standard-interceptors]
  (fn [{:keys [db]} _]
    (let [{:keys [system region database-name]} (-> db :routes :index :load-schema-form)
          entities (-> db :routes :index :schema :entities)
          selected-ns (-> db :routes :index :currently-selected-ns)
          unique-ident (->> entities selected-ns :ns-entities (filter :unique) (remove :deprecated?) first :ident str)
          random-ident (->> entities selected-ns :ns-entities (remove :deprecated?) first :ident str)]
      {:db (assoc-in db [:routes :index :analytics :loading?] true)
       :http-xhrio {:method :get
                    :uri (str api-url "analytics")
                    :headers {"system" (:value system)
                              "region" (:value region)
                              "db-name" (:value database-name)}
                    :params {:ident (if (str/blank? unique-ident)
                                      random-ident unique-ident)}
                    :response-format (ajax/transit-response-format)
                    :on-success [::get-analytics-success]
                    :on-failure [::get-analytics-failure]}})))
; endregion

(rf/reg-fx
  ::download-doc
  (fn [base64-img]
    (let [a (.createElement js/document "a")]
      (do (.appendChild (.-body js/document) a)
          (set! (.-style a) "display: none")
          (set! (.-href a) base64-img)
          (set! (.-target a) "_self")
          (set! (.-download a) "schema-diagram.png")
          (.click a)
          (.removeChild (.-body js/document) a)))))

(rf/reg-event-fx
  ::save-graph-to-file
  (fn [{:keys [db]} _]
    (let [^js diagram (-> db :routes :index :diagram :diagram)
          color-scheme (-> db :routes :index :settings :color-scheme)
          bg-color (-> db :routes :index :graph-colors (get color-scheme) :color-2)
          base64-img (.makeImageData diagram (clj->js {:scale 1 :background bg-color :type "image/png"}))]
      {::download-doc base64-img})))

