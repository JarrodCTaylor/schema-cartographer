(ns client.routes.index.events
  (:require
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [client.config :as config]
    [client.events :as shared-events]
    [client.interceptors :refer [standard-interceptors]]
    [client.utils.helpers :refer [title->kebab ns-bucket]]
    [clojure.string :as str]))

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

(rf/reg-event-db
  ::create-empty-schema
  (fn [db [_ _]]
    (assoc-in db [:routes :index :schema] {:entities {} :idents {}})))

(rf/reg-event-fx
  ::settings
  [standard-interceptors]
  (fn [{:keys [db]} [_ setting desired-state]]
    {:db (assoc-in db [:routes :index :settings setting] desired-state)
     ::shared-events/save-in-local-storage {(keyword (str "cartographer-ls-" (name setting))) desired-state}}))

(rf/reg-event-fx
  ::unload-schema
  [standard-interceptors]
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:routes :index :schema] nil)
             (assoc-in [:routes :index :settings :modal-visible?] false)
             (assoc-in [:routes :index :aside-filter] "")
             (assoc-in [:routes :index :left-panel-active-tab] :ns)
             (assoc-in [:routes :index :currently-selected-ns] nil)
             (assoc-in [:routes :index :previously-selected-ns] []))}))

(rf/reg-fx
  ::download-schema-graph
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
      {::download-schema-graph base64-img})))

; region ---- Schema map conversion --------------------------------------------
(defn ident->tx [{:keys [namespace doc ns-idents]}]
  (into [{:db/ident namespace
          :db/doc doc}]
        (mapv (fn [{:keys [ident doc replaced-by deprecated?]}]
                (merge {:db/ident ident}
                       (when doc {:db/doc doc})
                       (when replaced-by {:db.schema/replaced-by (mapv (fn [ident] {:db/ident ident}) replaced-by)})
                       (when deprecated? {:db.schema/deprecated? true}))) ns-idents)))

(defn idents->datomic-tx [idents]
  (vec (mapcat seq (map ident->tx (vals idents)))))

(defn entity->tx [{:keys [namespace doc ns-entities attrs preds]}]
  (into []
        (sequence cat [[{:db/ident namespace
                         :db/doc doc}]
                       (when (or attrs preds)
                         [(merge {:db/ident (keyword (name namespace) "validate")
                                  :db.schema/validates-namespace {:db/ident namespace}}
                                 (when attrs {:db.entity/attrs attrs})
                                 (when preds {:db.entity/preds preds}))])
                       (mapv (fn [{:keys [ident doc value-type cardinality unique references-namespaces attr-preds tuple-attrs no-history? is-component?]}]
                               (merge {:db/ident ident}
                                      (when doc {:db/doc doc})
                                      (when value-type {:db/valueType {:db/ident value-type}})
                                      (when cardinality {:db/cardinality {:db/ident cardinality}})
                                      (when unique {:db/unique {:db/ident :db.unique/identity}})
                                      (when references-namespaces {:db.schema/references-namespaces (mapv (fn [ref] {:db/ident ref}) references-namespaces)})
                                      (when attr-preds {:db.attr/preds attr-preds})
                                      (when no-history? {:db/noHistory true})
                                      (when is-component? {:db/isComponent true})
                                      (when tuple-attrs {:db/tupleAttrs tuple-attrs}))) ns-entities)])))

(defn entities->datomic-tx [entities]
  (mapcat seq (map entity->tx (vals entities))))

(defn schema-map->datomic-txs [{:keys [idents entities]}]
  (let [ident-txs (idents->datomic-tx idents)
        entity-tsx (entities->datomic-tx entities)]
    (into ident-txs entity-tsx)))

(rf/reg-fx
  ::download-edn-file
  (fn [[schema-edn file-name]]
    (let [a (.createElement js/document "a")
          file (js/Blob. schema-edn {:type "text/edn"})]
      (do (.appendChild (.-body js/document) a)
          (set! (.-style a) "display: none")
          (set! (.-href a) (.createObjectURL js/URL file))
          (set! (.-target a) "_self")
          (set! (.-download a) file-name)
          (.click a)
          (.removeChild (.-body js/document) a)))))

(rf/reg-event-fx
  ::export-schema-txs
  (fn [{:keys [db]} [_ _]]
    (let [annotation-idents [{:db/ident :db.schema/deprecated?
                              :db/valueType :db.type/boolean
                              :db/cardinality :db.cardinality/one
                              :db/doc "DOCUMENTATION ONLY. Boolean flag indicating the field has been deprecated."}
                             {:db/ident :db.schema/replaced-by
                              :db/valueType :db.type/ref
                              :db/cardinality :db.cardinality/many
                              :db/doc "DOCUMENTATION ONLY. Used to document when a deprecated field is replaced by another."}
                             {:db/ident :db.schema/references-namespaces
                              :db/valueType :db.type/ref
                              :db/cardinality :db.cardinality/many
                              :db/doc "DOCUMENTATION ONLY. Used to indicate which specific :db/idents are intended to be referenced by :db.type/ref"}
                             {:db/ident :db.schema/validates-namespace
                              :db/valueType :db.type/ref
                              :db/cardinality :db.cardinality/one
                              :db/doc "DOCUMENTATION ONLY. Used to indicate which specific :db/idents are intended to be validated by :db.type/ref"}]
          schema (-> db :routes :index :schema schema-map->datomic-txs)]
      {::download-edn-file [[(with-out-str (cljs.pprint/pprint (into annotation-idents schema)))] "schema-txs.edn"]})))

(rf/reg-event-fx
  ::export-schema-file
  (fn [{:keys [db]} [_ _]]
    (let [schema (-> db :routes :index :schema)]
      {::download-edn-file [[(with-out-str (cljs.pprint/pprint schema))] "schema.edn"]})))
; endregion

; region --- New Schema
(rf/reg-event-fx
  ::set-modal-visibility
  [standard-interceptors]
  (fn [{:keys [db]} [_ modal-name state]]
    {:db (assoc-in db [:routes :index :modal-visibility-state modal-name] state)}))

(rf/reg-event-fx
  ::add-new-ns
  (fn [{:keys [db]} [_ _]]
    (let [form (-> db :routes :index :new-ns-form)
          {:keys [type namespace doc]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          entity? (= "entity" (get type "value"))
          ns-str (if (str/starts-with? namespace ":") (str/replace-first namespace ":" "") (title->kebab namespace))
          ns-kw (keyword (str "db.schema." (if entity? "entity" "ident") ".namespace") ns-str)]
      {:db (assoc-in db [:routes
                         :index
                         :schema
                         (if entity? :entities :idents)
                         ns-kw] (merge {:namespace ns-kw
                                        :added-in-app? true}
                                       (if entity? {:ns-entities []} {:ns-idents []})
                                       (when doc {:doc doc})))
       :dispatch-n [[::set-modal-visibility :new-ns false]
                    [::shared-events/add-alert uuid "Namespace Successfully Added" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(rf/reg-event-fx
  ::edit-ns
  (fn [{:keys [db]} [_ _]]
    (let [form (-> db :routes :index :edit-ns-form)
          {:keys [doc entity-attrs entity-preds]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          entity? (= "db.schema.entity.namespace" (namespace selected-ns))]
      {:db (cond-> db
                   (not (str/blank? doc)) (assoc-in [:routes
                                                     :index
                                                     :schema
                                                     (if entity? :entities :idents)
                                                     selected-ns :doc] doc)
                   (not-empty entity-attrs) (update-in [:routes
                                                        :index
                                                        :schema
                                                        (if entity? :entities :idents)
                                                        selected-ns :attrs] into (mapv #(keyword (get % "value")) entity-attrs))
                   (not-empty entity-preds) (update-in [:routes
                                                        :index
                                                        :schema
                                                        (if entity? :entities :idents)
                                                        selected-ns :preds] into (mapv symbol (str/split entity-preds #"\s"))))
       :dispatch-n [[::set-modal-visibility :edit-ns false]
                    [::shared-events/add-alert uuid "Namespace Successfully Edited" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(rf/reg-event-fx
  ::update-referenced-by
  (fn [{:keys [db]} [_ selected-ns refed-namespace]]
    (let [[bucket _] (ns-bucket refed-namespace)]
      {:db (update-in db [:routes :index :schema bucket refed-namespace :referenced-by]
                      (fnil conj [])
                      selected-ns)})))

(rf/reg-event-fx
  ::add-new-ident
  [(rf/inject-cofx ::shared-events/uuid)]
  (fn [{:keys [db uuid]} [_ _]]
    (let [value #(get % "value")
          form (-> db :routes :index :new-ident-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          {:keys [ident doc deprecated replaced-by]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          ident (merge {:namespace selected-ns
                        :added-in-app? true
                        :ident (keyword (name selected-ns) (if (str/starts-with? ident ":") (str/replace-first ident ":" "") (title->kebab ident)))
                        :attribute? false}
                       (when doc {:doc doc})
                       (when replaced-by {:replaced-by (mapv #(keyword (value %)) replaced-by)})
                       (when (value deprecated) {:deprecated? true}))]
      {:db (update-in db [:routes :index :schema :idents selected-ns :ns-idents] conj ident)
       :dispatch-n [[::set-modal-visibility :new-ident false]
                    [::shared-events/add-alert uuid "Ident Successfully Added" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(rf/reg-event-fx
  ::add-new-entity-ident
  (fn [{:keys [db]} [_ _]]
    (let [value #(get % "value")
          form (-> db :routes :index :new-entity-ident-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          {:keys [ident doc cardinality value-type
                  tuple-attrs deprecated replaced-by unique
                  is-component no-history attr-preds ref-namespaces]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          ident (merge {:namespace selected-ns
                        :added-in-app? true
                        :ident (keyword (name selected-ns) (if (str/starts-with? ident ":") (str/replace-first ident ":" "") (title->kebab ident)))
                        :value-type (keyword "db.type" (value value-type))
                        :cardinality (keyword "db.cardinality" (value cardinality))
                        :attribute? true}
                       (when doc {:doc doc})
                       (when (value unique) {:unique :db.unique/identity})
                       (when (value is-component) {:is-component? true})
                       (when (value no-history) {:no-history? true})
                       (when (value deprecated) {:deprecated? true})
                       (when replaced-by {:replaced-by (mapv #(keyword (value %)) replaced-by)})
                       (when ref-namespaces {:references-namespaces (map #(keyword (value %)) ref-namespaces)})
                       (when tuple-attrs
                         (case (get value-type "value")
                           "composite-tuple" {:tuple-attrs (mapv #(keyword (value %)) tuple-attrs)}
                           "fixed-length-tuple" {:tuple-attrs (mapv #(keyword "db.type" (value %)) tuple-attrs)}
                           "variable-length-tuple" {:tuple-attrs (keyword "db.type" (value tuple-attrs))}))
                       (when attr-preds {:attr-preds (mapv symbol (str/split attr-preds #"\s"))}))]
      {:db (update-in db [:routes :index :schema :entities selected-ns :ns-entities] conj ident)
       :dispatch-n (into
                     [[::set-modal-visibility :new-entity-ident false]
                      [::shared-events/add-alert uuid "New Ident Successfully Added" "success"]]
                     (when ref-namespaces
                       (for [ref ref-namespaces]
                         [::update-referenced-by
                          selected-ns
                          (keyword (value ref))])))
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(rf/reg-event-fx
  ::set-edit-ident-values
  (fn [{:keys [db]} [_ _]]
    (let [form (-> db :routes :index :edit-ident-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          selected-ident (-> form :ident :value (get "value") keyword)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          selected-ident-map (->> db :routes :index :schema ns-bucket selected-ns ns-attrs (filter #(= selected-ident (:ident %))) first)
          {:keys [doc deprecated? references-namespaces value-type]} (select-keys selected-ident-map [:doc :deprecated? :references-namespaces :value-type])]
      {:db (cond-> db
                   (not (str/blank? doc)) (assoc-in [:routes :index :edit-ident-form :doc :value] doc)
                   deprecated? (assoc-in [:routes :index :edit-ident-form :deprecated :value] (clj->js {:value true :label "True"}))
                   (not deprecated?) (assoc-in [:routes :index :edit-ident-form :deprecated :value] (clj->js {:value false :label "False"})))})))

(rf/reg-event-fx
  ::edit-ident
  (fn [{:keys [db]} [_ _]]
    (let [form (-> db :routes :index :edit-ident-form)
          {:keys [ident doc deprecated ref-namespaces replaced-by]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          selected-ident-kw (keyword (get ident "value"))
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          ns-idents (-> db :routes :index :schema ns-bucket selected-ns ns-attrs)
          selected-ident (first (filter #(= selected-ident-kw (:ident %)) ns-idents))
          unchanged-idents (remove #(= selected-ident-kw (:ident %)) ns-idents)
          updated-ident (cond-> selected-ident
                                (not (str/blank? doc)) (assoc :doc doc)
                                ref-namespaces (update :references-namespaces into (map #(keyword (get % "value")) ref-namespaces))
                                deprecated (assoc :deprecated? (get (js->clj deprecated) "value"))
                                replaced-by (update :replaced-by (fn [existing new]
                                                                   (if existing
                                                                     (into existing new)
                                                                     new))
                                                    (mapv #(keyword (get % "value")) replaced-by)))]
      {:db (assoc-in db [:routes :index :schema ns-bucket selected-ns ns-attrs] (conj unchanged-idents updated-ident))
       :dispatch-n (into [[::set-modal-visibility :edit-ident false]
                          [::shared-events/add-alert uuid "Ident Successfully Edited" "success"]]
                         (when ref-namespaces
                           (for [ref ref-namespaces]
                             [::update-referenced-by
                              selected-ns
                              (keyword (get ref "value"))])))
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(defn remove-selected-namespaces [{:keys [idents entities] :as schema} selected-namespaces]
  (let [selections (->> selected-namespaces (map keyword) set)
        pruned-entities (reduce (fn [res [k v]] (if (contains? selections k) res (assoc res k v))) {} entities)
        pruned-idents (reduce (fn [res [k v]] (if (contains? selections k) res (assoc res k v))) {} idents)]
    (-> schema
        (assoc :entities pruned-entities)
        (assoc :idents pruned-idents))))

(defn remove-refs-to-ns [{:keys [entities]} selected-namespaces]
  (let [selections (->> selected-namespaces (map keyword) set)]
    (reduce
      (fn [res [ns ns-info]]
        (let [ns-entities (:ns-entities ns-info)
              updated-entities (mapv
                                 (fn [ident]
                                   (update ident :references-namespaces (fn [refed-namespaces]
                                                                          (remove #(contains? selections %) refed-namespaces))))
                                 ns-entities)]
          (assoc res ns (assoc ns-info :ns-entities updated-entities))))
      {}
      entities)))

(rf/reg-event-fx
  ::delete-ns
  [(rf/inject-cofx ::shared-events/uuid)]
  (fn [{:keys [db uuid]} [_ _]]
    (let [schema (-> db :routes :index :schema)
          form (-> db :routes :index :delete-ns-form)
          selected-namespaces (->> form :namespaces :value (map #(get % "value")))
          schema-without-namespaces (remove-selected-namespaces schema selected-namespaces)
          updated-entities (remove-refs-to-ns schema-without-namespaces selected-namespaces)]
      {:db (-> db
               (assoc-in [:routes :index :schema] schema-without-namespaces)
               (assoc-in [:routes :index :schema :entities] updated-entities))
       :dispatch-n [[::set-modal-visibility :delete-ns false]
                    [::shared-events/add-alert uuid "Namespace Successfully Deleted" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(defn remove-selected-idents [schema bucket ns-attrs selected-ns selected-ns-idents selected-idents]
  (let [selections (filter #(contains? selected-idents (:ident %)) selected-ns-idents)
        all-idents-ref-frequencies (->> selected-ns-idents
                                        (map :references-namespaces)
                                        (mapcat seq)
                                        frequencies)
        selected-ident-ref-frequencies (->> selections
                                            (filter #(contains? selected-idents (:ident %)))
                                            (filter #(not-empty (:references-namespaces %)))
                                            (map :references-namespaces)
                                            (mapcat seq)
                                            frequencies)
        remaining-idents-in-entity-ns (remove #(contains? selected-idents (:ident %)) selected-ns-idents)
        updated-schema (assoc-in schema [bucket selected-ns ns-attrs] remaining-idents-in-entity-ns)
        remove-refed-by-from (set (remove nil? (map (fn [[k frequency]]
                                                      (when (= (k all-idents-ref-frequencies) frequency) k)) selected-ident-ref-frequencies)))]
    {:schema-without-idents updated-schema :remove-refed-by remove-refed-by-from}))

(defn remove-unneeded-refed-by [{:keys [entities idents] :as schema} selected-ns remove-from]
  (let [update-bucket (fn [bucket] (reduce (fn [res [k v]]
                                             (if (contains? remove-from k)
                                               (assoc res k (update v :referenced-by (fn [refs] (remove #(= selected-ns %) refs))))
                                               (assoc res k v))) {} bucket))
        updated-idents (update-bucket idents)
        updated-entities (update-bucket entities)]
    (-> schema
        (assoc :idents updated-idents)
        (assoc :entities updated-entities))))

(rf/reg-event-fx
  ::delete-idents
  [(rf/inject-cofx ::shared-events/uuid)]
  (fn [{:keys [db uuid]} [_ _]]
    (let [schema (-> db :routes :index :schema)
          form (-> db :routes :index :delete-ident-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [bucket ns-attrs] (ns-bucket selected-ns)
          selected-ns-idents (-> schema bucket selected-ns ns-attrs)
          selected-idents (->> form :idents :value (map #(get % "value")) (map keyword) set)
          {:keys [schema-without-idents remove-refed-by]} (remove-selected-idents schema bucket ns-attrs selected-ns selected-ns-idents selected-idents)
          schema-without-unneeded-refs (remove-unneeded-refed-by schema-without-idents selected-ns remove-refed-by)]
      {:db (assoc-in db [:routes :index :schema] schema-without-unneeded-refs)
       :dispatch-n [[::set-modal-visibility :delete-ident false]
                    [::shared-events/add-alert uuid "Idents Successfully Deleted" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(defn find-refed-by [db selected-ns]
  (let [[ns-bucket _] (ns-bucket selected-ns)
        refed-by (-> db :routes :index :schema ns-bucket selected-ns :referenced-by)]
    (when refed-by {:ns selected-ns :refed-by refed-by})))

(rf/reg-event-fx
  ::update-existing-ref-notices
  (fn [{:keys [db]} [_ selected-namespaces]]
    (let [selected-namespaces-kw (map #(keyword (get % "value")) selected-namespaces)
          notices (map (partial find-refed-by db) selected-namespaces-kw)]
    {:db (assoc-in db [:routes :index :existing-ref-notices] notices)})))
