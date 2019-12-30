(ns cljs.routes.index.events
  (:require
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [cljs.events :as shared-events]
    [cljs.interceptors :refer [standard-interceptors]]))

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
     ::set-transition-height []}))

(rf/reg-event-fx
  ::select-breadcrumb-ns
  [standard-interceptors]
  (fn [{:keys [db]} [_ ns]]
    {:db (-> db
             (assoc-in [:routes :index :currently-selected-ns] ns)
             (update-in [:routes :index :previously-selected-ns] (fn [selected-ns] (into [] (take-while #(not= ns %) selected-ns)))))}))

(rf/reg-event-fx
  ::select-ns-and-push-previous
  [standard-interceptors]
  (fn [{:keys [db]} [_ ns]]
    (let [previously-selected (-> db :routes :index :currently-selected-ns)]
      {:db (-> db
               (assoc-in [:routes :index :currently-selected-ns] ns)
               (update-in [:routes :index :previously-selected-ns] conj previously-selected))})))
; endregion

(rf/reg-event-db
  ::update-aside-filter
  [standard-interceptors]
  (fn [db [_ filter-str]]
    (assoc-in db [:routes :index :aside-filter] filter-str)))

(rf/reg-event-db
  ::set-left-panel-active-tab
  (fn [db [_ tab]]
    (assoc-in db [:routes :index :left-panel-active-tab] tab)))

(rf/reg-event-db
  ::load-schema
  [standard-interceptors]
  (fn [db [_ schema]]
    (assoc-in db [:routes :index :schema] schema)))

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

