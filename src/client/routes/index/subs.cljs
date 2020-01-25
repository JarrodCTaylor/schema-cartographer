(ns client.routes.index.subs
  (:require
    [clojure.string :as str]
    [clojure.set :as set]
    [re-frame.core :as rf]
    [client.utils.helpers :refer [ns-bucket]]))

(defn qualified-kebab->title
  "(qualified-kebab->title 'things.value-set') ;=> 'Things Value Set'"
  [s]
  (as-> s _
        (str/split _ #"-|\.")
        (map str/capitalize _)
        (str/join " " _)))

(rf/reg-sub
  ::schema-loaded?
  (fn [db _]
    (-> db :routes :index :schema empty? not)))

(rf/reg-sub
  ::raw-schema-data-map
  (fn [db _]
    (-> db :routes :index :schema)))

(rf/reg-sub
  ::nav-schema
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          aside-filter (-> db :routes :index :aside-filter)
          filter-fn #(re-matches (re-pattern (str "(?i).*" aside-filter ".*")) (:label %))
          entity-maps (map (fn [entity-ns]
                             {:label (-> entity-ns name qualified-kebab->title)
                              :ns entity-ns}) (-> schema :entities keys))
          ident-maps (map (fn [ident-ns]
                            {:label (-> ident-ns name qualified-kebab->title)
                             :ns ident-ns}) (-> schema :idents keys))]
      {:entities (sort-by :label (filter filter-fn entity-maps))
       :idents (sort-by :label (filter filter-fn ident-maps))})))

(rf/reg-sub
  ::selected-ns
  (fn [db _]
    (-> db :routes :index :currently-selected-ns)))

(rf/reg-sub
  ::entity-selected?
  :<- [::selected-ns]
  (fn [selection]
    (when selection (= "db.schema.entity.namespace" (namespace selection)))))

(rf/reg-sub
  ::ident-selected?
  :<- [::selected-ns]
  (fn [selection]
    (when selection (= "db.schema.ident.namespace" (namespace selection)))))

(rf/reg-sub
  ::aside-selection-summary-info
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          previously-selected-ns-keys (-> db :routes :index :previously-selected-ns)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket _] (ns-bucket selected-ns)
          {:keys [doc referenced-by analytics attrs preds]} (some-> ns-bucket schema selected-ns)]
      {:label (some-> selected-ns name qualified-kebab->title)
       :entity-count (:entity-count analytics)
       :oldest (:oldest analytics)
       :newest (:newest analytics)
       :doc doc
       :attrs (->> attrs (map name) (map qualified-kebab->title))
       :attrs-kw (->> attrs (map str))
       :preds preds
       :referenced-by (set (map (fn [ns]
                                  {:ns ns
                                   :label (qualified-kebab->title (name ns))}) referenced-by))
       :by-way-of (map (fn [ns]
                         {:ns ns
                          :label (qualified-kebab->title (name ns))})
                       (when selected-ns (conj previously-selected-ns-keys selected-ns)))})))

(rf/reg-sub
  ::aside-selection-details
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          details (some->> ns-bucket schema selected-ns)]
      (update details ns-attrs (fn [attrs]
                                 (let [idents (map (fn [attr]
                                                     (-> attr
                                                         (update :ident #(qualified-kebab->title (name %)))
                                                         (assoc :ident-kw (-> attr :ident str))
                                                         (update :references-namespaces (fn [namespaces]
                                                                                          (map (fn [refed-ns]
                                                                                                 {:label (qualified-kebab->title (name refed-ns))
                                                                                                  :kw refed-ns}) namespaces)))
                                                         (update :replaced-by (fn [namespaces]
                                                                                (map (fn [replaced-by-ns]
                                                                                       {:label (qualified-kebab->title (name replaced-by-ns))
                                                                                        :kw replaced-by-ns}) namespaces)))))
                                                   attrs)]
                                   (sort-by (juxt :deprecated? (complement :unique) :ident) idents)))))))

(rf/reg-sub
  ::schema-added-in-app
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          new-idents (some->> ns-bucket schema selected-ns ns-attrs (filter :added-in-app?))
          {:keys [idents entities]} (select-keys schema [:idents :entities])
          added-entities (filter :added-in-app? (vals entities))
          added-idents (filter :added-in-app? (vals idents))]
      {:new-ident-namespaces added-idents
       :new-entity-namespaces added-entities
       :new-idents new-idents})))

(rf/reg-sub
  ::schema-added-in-app?
  :<- [::schema-added-in-app]
  (fn [{:keys [new-ident-namespaces new-entity-namespaces new-idents]}]
    {:ns-have-been-added? (or (not-empty new-ident-namespaces) (not-empty new-entity-namespaces))
     :idents-have-been-added? (not-empty new-idents)}))

(rf/reg-sub
  ::added-ns
  :<- [::schema-added-in-app]
  (fn [{:keys [new-ident-namespaces new-entity-namespaces]}]
    (mapv (fn [{:keys [namespace]}] {:value (str/replace-first (str namespace) #"^:" "") :label (str namespace)}) (into new-ident-namespaces new-entity-namespaces))))

(rf/reg-sub
  ::added-idents
  :<- [::schema-added-in-app]
  (fn [{:keys [new-idents]}]
    (mapv (fn [{:keys [ident]}] {:value (str/replace-first (str ident) #"^:" "") :label (str ident)}) new-idents)))

(rf/reg-sub
  ::aside-filter
  (fn [db _]
    (-> db :routes :index :aside-filter)))

(rf/reg-sub
  ::left-panel-active-tab
  (fn [db _]
    (-> db :routes :index :left-panel-active-tab)))

(rf/reg-sub
  ::graph-command-handler
  (fn [db _]
    (-> db :routes :index :diagram :command-handler)))

(rf/reg-sub
  ::diagram
  (fn [db _]
    (-> db :routes :index :diagram)))

; region ---- Node Data Arrays -------------------------------------------------
(defn attr->node [display-as-keywords? {:keys [ident value-type unique deprecated?]}]
  (let [attr-type (when value-type (str ": " (str/capitalize (name value-type))))
        unique? (= :db.unique/identity unique)
        attr (if display-as-keywords?
               (str ident)
               (-> ident name qualified-kebab->title))]
    (merge {:attr (str attr attr-type)
            :color "#464B52"
            :iskey unique?
            :deprecated? deprecated?
            :figure (cond
                      unique? "Key2"
                      deprecated? "Skull"
                      (not attr-type) "Ident"
                      :else "Empty")})))

(defn node-data-array [display-as-keywords? {:keys [idents entities] :as x}]
  (let [make-node (fn [schema-type]
                    (fn [[k v]]
                      {:key (str (qualified-kebab->title (name k))
                                 (when (= schema-type :ns-idents) ": Idents"))
                       :items (sort-by
                                (juxt :deprecated? (complement :iskey) :attr)
                                (map (partial attr->node display-as-keywords?) (schema-type v)))}))
        ident-node-data (map (make-node :ns-idents) idents)
        entity-node-data (map (make-node :ns-entities) entities)]
    (sort-by :key (into ident-node-data entity-node-data))))

(rf/reg-sub
  ::node-data-array
  (fn [db _]
    (let [selected-ns-key (-> db :routes :index :currently-selected-ns)
          previously-selected-ns-keys (-> db :routes :index :previously-selected-ns)
          [ns-bucket _] (ns-bucket selected-ns-key)
          schema (-> db :routes :index :schema)
          filtered-entities (select-keys (:entities schema) (sequence cat [previously-selected-ns-keys
                                                                           [selected-ns-key]
                                                                           (some->> ns-bucket schema selected-ns-key :ns-entities (mapcat :references-namespaces))]))
          filtered-idents (if (= :entities ns-bucket)
                            (select-keys (:idents schema) (some->> ns-bucket schema selected-ns-key :ns-entities (mapcat :references-namespaces)))
                            (select-keys (:idents schema) [selected-ns-key]))
          display-as-keywords? (-> db :routes :index :settings :display-as-keywords?)]
      (when selected-ns-key
        (node-data-array display-as-keywords? {:idents filtered-idents :entities filtered-entities})))))
; endregion

; region ---- Linked Data Arrays -----------------------------------------------
(defn entity->data-array [{:keys [namespace referenced-ns ident cardinality]}]
  (let [attr-type (when (str/starts-with? (str referenced-ns) ":db.schema.ident.namespace") ": Idents")]
    {:from (-> namespace name qualified-kebab->title)
     :to (str (-> referenced-ns name qualified-kebab->title) attr-type)
     :text (-> ident name qualified-kebab->title)
     :toText (if (= cardinality :db.cardinality/one) "1" "*")}))

(defn separate-entity-refs [{:keys [namespace ident cardinality references-namespaces]}]
  (map (fn [ns]
         {:namespace namespace :ident ident :cardinality cardinality :referenced-ns ns})
       references-namespaces))

(defn linked-data-array [schema selected-ns-key]
  (let [entities (:entities schema)
        relevant-namespaces (into [selected-ns-key] (->> entities selected-ns-key :ns-entities (mapcat :references-namespaces)))
        relevant-entities (->>
                            (select-keys entities relevant-namespaces)
                            vals
                            (filter #(= (:namespace %) selected-ns-key))
                            first
                            :ns-entities)
        entities-with-references (filter #(-> % :references-namespaces not-empty) relevant-entities)
        single-ref-entities (mapcat separate-entity-refs entities-with-references)]
    (map entity->data-array single-ref-entities)))

(rf/reg-sub
  ::linked-data-array
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          selected-ns-key (-> db :routes :index :currently-selected-ns)
          previously-selected-ns-keys (-> db :routes :index :previously-selected-ns)]
      (if (and selected-ns-key (= "db.schema.entity.namespace" (namespace selected-ns-key)))
        (mapcat #(linked-data-array schema %) (into previously-selected-ns-keys [selected-ns-key]))
        (mapcat #(linked-data-array schema %) previously-selected-ns-keys)))))
; endregion

(rf/reg-sub
  ::load-schema-active-tab
  (fn [db _]
    (-> db :routes :index :load-schema-tabs :active-tab)))

(rf/reg-sub
  ::settings
  (fn [db _]
    (-> db :routes :index :settings)))

; region --- New Schema
(rf/reg-sub
  ::modal-visible?
  (fn [db [_ modal-name]]
    (-> db :routes :index :modal-visibility-state modal-name)))

(rf/reg-sub
  ::ns-ident-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          idents (when selected-ns (->> schema ns-bucket selected-ns ns-attrs (map :ident) sort))]
      (mapv (fn [ident] {:value (str/replace-first (str ident) #"^:" "") :label (str ident)}) idents))))

(rf/reg-sub
  ::edit-ident-replaced-by-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          form (-> db :routes :index :edit-ident-form)
          selected-ident (-> form :ident :value (get "value") keyword)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          idents (when selected-ns (->> schema ns-bucket selected-ns ns-attrs (map :ident) sort))
          selected-ident-map (->> db :routes :index :schema ns-bucket selected-ns ns-attrs (filter #(= selected-ident (:ident %))) first)
          {:keys [replaced-by]} (select-keys selected-ident-map [:doc :deprecated? :replaced-by])]
      {:replaced-by-options (mapv (fn [ident] {:value (str/replace-first (str ident) #"^:" "") :label (str ident)}) (set/difference (set idents) (set replaced-by)))
       :existing-replaced-by (map str replaced-by)})))

(rf/reg-sub
  ::ns-attr-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          existing-attrs (when selected-ns (->> schema ns-bucket selected-ns :attrs set))
          idents (when selected-ns (->> schema ns-bucket selected-ns ns-attrs (map :ident) sort))
          unused-idents (remove #(contains? existing-attrs %) idents)]
      (map (fn [ident] {:value (str/replace-first (str ident) #"^:" "")
                        :label (str ident)}) unused-idents))))

(rf/reg-sub
  ::ns-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          entity-namespaces (-> schema :entities keys sort)
          ident-namespaces (-> schema :idents keys sort)]
      (map (fn [ns-kw] {:value (str/replace-first (str ns-kw) #"^:" "") :label (str ns-kw)}) (into entity-namespaces ident-namespaces)))))

(rf/reg-sub
  ::ident-already-deprecated?
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          form (-> db :routes :index :edit-ident-form)
          selected-ident-kw (-> form :ident :value (get "value") keyword)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          ident (when selected-ns (->> schema ns-bucket selected-ns ns-attrs (filter #(= selected-ident-kw (:ident %))) first))]
      (:deprecated? ident))))

(rf/reg-sub
  ::edit-ident-selected?
  (fn [db [_ _]]
    (let [form (-> db :routes :index :edit-ident-form)]
      (-> form :ident :value nil? not))))

(rf/reg-sub
  ::existing-ref-notices
  (fn [db [_ _]]
    (->> db
         :routes
         :index
         :existing-ref-notices
         (remove nil?)
         (map #(update % :ns (fn [kw]
                               (-> kw
                                   name
                                   qualified-kebab->title)))))))

(rf/reg-sub
  ::selected-ident-to-edit-ref-info
  (fn [db [_ _]]
    (let [form (-> db :routes :index :edit-ident-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          selected-ident (-> form :ident :value (get "value") keyword)
          [ns-bucket ns-attrs] (ns-bucket selected-ns)
          selected-ident-map (->> db :routes :index :schema ns-bucket selected-ns ns-attrs (filter #(= selected-ident (:ident %))) first)]
      {:is-ref-type? (= :db.type/ref (:value-type selected-ident-map))
       :existing-references-namespaces (:references-namespaces selected-ident-map)})))

(rf/reg-sub
  ::edit-ident-ref-options
  :<- [::ns-options]
  :<- [::selected-ident-to-edit-ref-info]
  (fn [[ns-options {:keys [existing-references-namespaces]}]]
    (remove #(contains? (->> existing-references-namespaces (map str) set) (:label %)) ns-options)))
;endregion
