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
    (let [currently-selected (-> db :routes :index :currently-selected-ns)]
      {:db (cond-> db
                   true (assoc-in [:routes :index :currently-selected-ns] ns)
                   (not= ns currently-selected) (update-in [:routes :index :previously-selected-ns] conj currently-selected))})))
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
    (assoc-in db [:routes :index :schema] {:entities {} :enumerations {}})))

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
(defn ns->identifier [ns]
  (let [id-ns (namespace ns)
        [k n] (str/split id-ns #"\.")]
    (keyword k n)))

(defn attr->tx [{:keys [namespace doc ns-attrs]}]
  (into [(merge {:db/id (str namespace)
                 (ns->identifier namespace) (-> namespace name keyword)}
                (when doc {:db/doc doc}))]
        (mapv (fn [{:keys [ident doc replaced-by deprecated?]}]
                (merge {:db/id (str ident)
                        :db/ident ident}
                       (when doc {:db/doc doc})
                       (when replaced-by {:cartographer/replaced-by (mapv (fn [ident] {:db/ident ident}) replaced-by)})
                       (when deprecated? {:cartographer/deprecated? true}))) ns-attrs)))

(defn attrs->datomic-tx [attrs]
  (vec (mapcat seq (map attr->tx (vals attrs)))))

(defn entity->tx [{:keys [namespace doc ns-attrs attrs preds]}]
  (into []
        (sequence cat [[(merge {:db/id (str namespace)
                                (ns->identifier namespace) (-> namespace name keyword)}
                               (when doc {:db/doc doc}))]
                       (mapv (fn [{:keys [ident doc value-type cardinality unique references-namespaces attr-preds tuple-attrs no-history? is-component?]}]
                               (merge {:db/id (str ident)
                                       :db/ident ident}
                                      (when doc {:db/doc doc})
                                      (when value-type {:db/valueType {:db/ident value-type}})
                                      (when cardinality {:db/cardinality {:db/ident cardinality}})
                                      (when unique {:db/unique {:db/ident :db.unique/identity}})
                                      (when references-namespaces {:cartographer/references-namespaces (mapv str references-namespaces) #_ (mapv (fn [ref]
                                                                                                               (if (str/starts-with? (str ref) ":cartographer.entity")
                                                                                                                 {:cartographer/entity (-> ref name keyword)}
                                                                                                                 {:cartographer/enumeration (-> ref name keyword)})) references-namespaces)})
                                      (when attr-preds {:db.attr/preds attr-preds})
                                      (when no-history? {:db/noHistory true})
                                      (when is-component? {:db/isComponent true})
                                      (when tuple-attrs {:db/tupleAttrs tuple-attrs}))) ns-attrs)
                       (when (or attrs preds)
                         [(merge {:db/ident (keyword (name namespace) "validate")
                                  :cartographer/validates-namespace {:db/ident namespace}}
                                 (when attrs {:db.entity/attrs attrs})
                                 (when preds {:db.entity/preds preds}))])])))

(defn sort-entity-attrs
  "When creating tx data ensure tuples come last"
  [entities]
  (sort-by (juxt
             (fn [{:keys [value-type]}]
               (= :db.type/tuple value-type))
             #(-> % :ident str)) entities))

(defn entities->datomic-tx [entities]
  (let []
    (mapcat seq (map
                  (fn [entity-group]
                    (entity->tx (update entity-group :ns-attrs sort-entity-attrs)))
                  (vals entities)))))

(defn schema-map->datomic-txs [{:keys [enumerations entities]}]
  (let [attr-txs (attrs->datomic-tx enumerations)
        entity-tsx (entities->datomic-tx entities)]
    (into attr-txs entity-tsx)))

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
    (let [annotation-attrs [{:db/ident :cartographer/entity
                             :db/valueType :db.type/keyword
                             :db/unique :db.unique/identity
                             :db/cardinality :db.cardinality/one
                             :db/doc "Creating an entity with this attr will cause its value to be considered an entity-grouping namespace in the application."}
                            {:db/ident :cartographer/enumeration
                             :db/valueType :db.type/keyword
                             :db/unique :db.unique/identity
                             :db/cardinality :db.cardinality/one
                             :db/doc "Creating an entity with this attr will cause its value to be considered an enumeration-grouping namespace in the application."}
                            {:db/ident :cartographer/deprecated?
                             :db/valueType :db.type/boolean
                             :db/cardinality :db.cardinality/one
                             :db/doc "Boolean flag indicating the field has been deprecated."}
                            {:db/ident :cartographer/replaced-by
                             :db/valueType :db.type/ref
                             :db/cardinality :db.cardinality/many
                             :db/doc "Used to document when a deprecated field is replaced by other."}
                            {:db/ident :cartographer/references-namespaces
                             :db/valueType :db.type/ref
                             :db/cardinality :db.cardinality/many
                             :db/doc "Used to indicate which specific :cartographer/entity or :cartographer/enumeration are intended to be referenced by :db.type/ref"}
                            {:db/ident :cartographer/validates-namespace
                             :db/valueType :db.type/ref
                             :db/cardinality :db.cardinality/one
                             :db/doc "Used to indicate which specific :cartographer/entity is intended to be validated by :db.type/ref"}]
          schema (-> db :routes :index :schema schema-map->datomic-txs)]
      {::download-edn-file [[(with-out-str (cljs.pprint/pprint {:annotation-attrs annotation-attrs
                                                                :db-schema-attrs schema}))] "schema-txs.edn"]})))

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
          ns-kw (keyword (str "cartographer." (if entity? "entity" "enumeration")) ns-str)]
      {:db (assoc-in db [:routes
                         :index
                         :schema
                         (if entity? :entities :enumerations)
                         ns-kw] (merge {:namespace ns-kw
                                        :added-in-app? true}
                                       (if entity? {:ns-attrs []} {:ns-attrs []})
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
          entity? (= "cartographer.entity" (namespace selected-ns))]
      {:db (cond-> db
                   (not (str/blank? doc)) (assoc-in [:routes
                                                     :index
                                                     :schema
                                                     (if entity? :entities :enumerations)
                                                     selected-ns :doc] doc)
                   (not-empty entity-attrs) (update-in [:routes
                                                        :index
                                                        :schema
                                                        (if entity? :entities :enumerations)
                                                        selected-ns :attrs] into (mapv #(keyword (get % "value")) entity-attrs))
                   (not-empty entity-preds) (update-in [:routes
                                                        :index
                                                        :schema
                                                        (if entity? :entities :enumerations)
                                                        selected-ns :preds] into (mapv symbol (str/split entity-preds #"\s"))))
       :dispatch-n [[::set-modal-visibility :edit-ns false]
                    [::shared-events/add-alert uuid "Namespace Successfully Edited" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(rf/reg-event-fx
  ::update-referenced-by
  (fn [{:keys [db]} [_ selected-ns refed-namespace]]
    (let [bucket (ns-bucket refed-namespace)]
      {:db (update-in db [:routes :index :schema bucket refed-namespace :referenced-by]
                      (fnil conj [])
                      selected-ns)})))

(rf/reg-event-fx
  ::add-new-attr
  [(rf/inject-cofx ::shared-events/uuid)]
  (fn [{:keys [db uuid]} [_ _]]
    (let [value #(get % "value")
          form (-> db :routes :index :new-attr-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          {:keys [attr doc deprecated replaced-by]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          attr (merge {:namespace selected-ns
                       :added-in-app? true
                       :ident (keyword (name selected-ns) (if (str/starts-with? attr ":") (str/replace-first attr ":" "") (title->kebab attr)))
                       :attribute? false
                       :deprecated? (if (value deprecated) true false)}
                      (when doc {:doc doc})
                      (when replaced-by {:replaced-by (mapv #(keyword (value %)) replaced-by)}))]
      {:db (update-in db [:routes :index :schema :enumerations selected-ns :ns-attrs] conj attr)
       :dispatch-n [[::set-modal-visibility :new-attr false]
                    [::shared-events/add-alert uuid "Attr Successfully Added" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(rf/reg-event-fx
  ::add-new-entity-attr
  (fn [{:keys [db]} [_ _]]
    (let [value #(get % "value")
          form (-> db :routes :index :new-entity-attr-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          {:keys [attr doc cardinality value-type
                  tuple-attrs deprecated replaced-by unique
                  is-component no-history attr-preds ref-namespaces]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          attr (merge {:namespace selected-ns
                       :added-in-app? true
                       :ident (keyword (name selected-ns) (if (str/starts-with? attr ":") (str/replace-first attr ":" "") (title->kebab attr)))
                       :value-type (keyword "db.type" (value value-type))
                       :cardinality (keyword "db.cardinality" (value cardinality))
                       :attribute? true
                       :deprecated? (if (value deprecated) true false)}
                      (when doc {:doc doc})
                      (when (value unique) {:unique :db.unique/identity})
                      (when (value is-component) {:is-component? true})
                      (when (value no-history) {:no-history? true})
                      (when replaced-by {:replaced-by (mapv #(keyword (value %)) replaced-by)})
                      (when ref-namespaces {:references-namespaces (map #(keyword (value %)) ref-namespaces)})
                      (when tuple-attrs
                        (case (get value-type "value")
                          "tuple" {:tuple-attrs (mapv #(keyword (value %)) tuple-attrs)}
                          "fixed-length-tuple" {:tuple-attrs (mapv #(keyword "db.type" (value %)) tuple-attrs)}
                          "variable-length-tuple" {:tuple-attrs (keyword "db.type" (value tuple-attrs))}))
                      (when attr-preds {:attr-preds (mapv symbol (str/split attr-preds #"\s"))}))]
      {:db (update-in db [:routes :index :schema :entities selected-ns :ns-attrs] conj attr)
       :dispatch-n (into
                     [[::set-modal-visibility :new-entity-attr false]
                      [::shared-events/add-alert uuid "New Attr Successfully Added" "success"]]
                     (when ref-namespaces
                       (for [ref ref-namespaces]
                         [::update-referenced-by
                          selected-ns
                          (keyword (value ref))])))
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(rf/reg-event-fx
  ::set-edit-attr-values
  (fn [{:keys [db]} [_ _]]
    (let [form (-> db :routes :index :edit-attr-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          selected-attr (-> form :attr :value (get "value") keyword)
          ns-bucket (ns-bucket selected-ns)
          selected-attr-map (->> db :routes :index :schema ns-bucket selected-ns :ns-attrs (filter #(= selected-attr (:ident %))) first)
          {:keys [doc deprecated? references-namespaces value-type]} (select-keys selected-attr-map [:doc :deprecated? :references-namespaces :value-type])]
      {:db (cond-> db
                   (not (str/blank? doc)) (assoc-in [:routes :index :edit-attr-form :doc :value] doc)
                   deprecated? (assoc-in [:routes :index :edit-attr-form :deprecated :value] (clj->js {:value true :label "True"}))
                   (not deprecated?) (assoc-in [:routes :index :edit-attr-form :deprecated :value] (clj->js {:value false :label "False"})))})))

(rf/reg-event-fx
  ::edit-attr
  (fn [{:keys [db]} [_ _]]
    (let [form (-> db :routes :index :edit-attr-form)
          {:keys [attr doc deprecated ref-namespaces replaced-by]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
          selected-attr-kw (keyword (get attr "value"))
          selected-ns (-> db :routes :index :currently-selected-ns)
          ns-bucket (ns-bucket selected-ns)
          ns-attrs (-> db :routes :index :schema ns-bucket selected-ns :ns-attrs)
          selected-attr (first (filter #(= selected-attr-kw (:ident %)) ns-attrs))
          unchanged-attrs (remove #(= selected-attr-kw (:ident %)) ns-attrs)
          updated-attr (cond-> selected-attr
                               (not (str/blank? doc)) (assoc :doc doc)
                               ref-namespaces (update :references-namespaces into (map #(keyword (get % "value")) ref-namespaces))
                               deprecated (assoc :deprecated? (get (js->clj deprecated) "value"))
                               replaced-by (update :replaced-by (fn [existing new]
                                                                  (if existing
                                                                    (into existing new)
                                                                    new))
                                                   (mapv #(keyword (get % "value")) replaced-by)))]
      {:db (assoc-in db [:routes :index :schema ns-bucket selected-ns :ns-attrs] (conj unchanged-attrs updated-attr))
       :dispatch-n (into [[::set-modal-visibility :edit-attr false]
                          [::shared-events/add-alert uuid "Attr Successfully Edited" "success"]]
                         (when ref-namespaces
                           (for [ref ref-namespaces]
                             [::update-referenced-by
                              selected-ns
                              (keyword (get ref "value"))])))
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(defn remove-selected-namespaces [{:keys [enumerations entities] :as schema} selected-namespaces]
  (let [selections (->> selected-namespaces (map keyword) set)
        pruned-entities (reduce (fn [res [k v]] (if (contains? selections k) res (assoc res k v))) {} entities)
        pruned-attrs (reduce (fn [res [k v]] (if (contains? selections k) res (assoc res k v))) {} enumerations)]
    (-> schema
        (assoc :entities pruned-entities)
        (assoc :enumerations pruned-attrs))))

(defn remove-refs-to-ns [{:keys [entities]} selected-namespaces]
  (let [selections (->> selected-namespaces (map keyword) set)]
    (reduce
      (fn [res [ns ns-info]]
        (let [ns-attrs (:ns-attrs ns-info)
              updated-entities (mapv
                                 (fn [attr]
                                   (update attr :references-namespaces (fn [refed-namespaces]
                                                                         (remove #(contains? selections %) refed-namespaces))))
                                 ns-attrs)]
          (assoc res ns (assoc ns-info :ns-attrs updated-entities))))
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

(defn remove-selected-attrs [schema bucket ns-attrs selected-ns selected-ns-attrs selected-attrs]
  (let [selections (filter #(contains? selected-attrs (:ident %)) selected-ns-attrs)
        all-attrs-ref-frequencies (->> selected-ns-attrs
                                        (map :references-namespaces)
                                        (mapcat seq)
                                        frequencies)
        selected-attrs-ref-frequencies (->> selections
                                            (filter #(contains? selected-attrs (:ident %)))
                                            (filter #(not-empty (:references-namespaces %)))
                                            (map :references-namespaces)
                                            (mapcat seq)
                                            frequencies)
        remaining-attrs-in-entity-ns (remove #(contains? selected-attrs (:ident %)) selected-ns-attrs)
        updated-schema (assoc-in schema [bucket selected-ns ns-attrs] remaining-attrs-in-entity-ns)
        remove-refed-by-from (set (remove nil? (map (fn [[k frequency]]
                                                      (when (= (k all-attrs-ref-frequencies) frequency) k)) selected-attrs-ref-frequencies)))]
    {:schema-without-attrs updated-schema :remove-refed-by remove-refed-by-from}))

(defn remove-unneeded-refed-by [{:keys [entities enumerations] :as schema} selected-ns remove-from]
  (let [update-bucket (fn [bucket] (reduce (fn [res [k v]]
                                             (if (contains? remove-from k)
                                               (assoc res k (update v :referenced-by (fn [refs] (remove #(= selected-ns %) refs))))
                                               (assoc res k v))) {} bucket))
        updated-attrs (update-bucket enumerations)
        updated-entities (update-bucket entities)]
    (-> schema
        (assoc :enumerations updated-attrs)
        (assoc :entities updated-entities))))

(rf/reg-event-fx
  ::delete-attrs
  [(rf/inject-cofx ::shared-events/uuid)]
  (fn [{:keys [db uuid]} [_ _]]
    (let [schema (-> db :routes :index :schema)
          form (-> db :routes :index :delete-attr-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          bucket (ns-bucket selected-ns)
          selected-ns-attrs (-> schema bucket selected-ns :ns-attrs)
          selected-attrs (->> form :enumerations :value (map #(get % "value")) (map keyword) set)
          {:keys [schema-without-attrs remove-refed-by]} (remove-selected-attrs schema bucket :ns-attrs selected-ns selected-ns-attrs selected-attrs)
          schema-without-unneeded-refs (remove-unneeded-refed-by schema-without-attrs selected-ns remove-refed-by)]
      {:db (assoc-in db [:routes :index :schema] schema-without-unneeded-refs)
       :dispatch-n [[::set-modal-visibility :delete-attr false]
                    [::shared-events/add-alert uuid "Attrs Successfully Deleted" "success"]]
       :dispatch-later [{:ms config/alert-timeout-ms
                         :dispatch [::shared-events/remove-alert uuid]}]})))

(defn find-refed-by [db selected-ns]
  (let [ns-bucket (ns-bucket selected-ns)
        refed-by (-> db :routes :index :schema ns-bucket selected-ns :referenced-by)]
    (when refed-by {:ns selected-ns :refed-by refed-by})))

(rf/reg-event-fx
  ::update-existing-ref-notices
  (fn [{:keys [db]} [_ selected-namespaces]]
    (let [selected-namespaces-kw (map #(keyword (get % "value")) selected-namespaces)
          notices (map (partial find-refed-by db) selected-namespaces-kw)]
    {:db (assoc-in db [:routes :index :existing-ref-notices] notices)})))
