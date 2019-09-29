(ns client.routes.index.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :as rf]))

(defn qualified-kebab->title
  "(qualified-kebab->title 'things.value-set') ;=> 'Things Value Set'"
  [s]
  (as-> s _
        (str/split _ #"-|\.")
        (map str/capitalize _)
        (str/join " " _)))

(defn ns-bucket
  [the-ns]
  (let [ns (try (namespace the-ns)
                (catch js/Error _ nil))]
    (case ns
      "db.schema.ident.namespace" [:idents :ns-idents]
      "db.schema.entity.namespace" [:entities :ns-entities]
      nil)))

(rf/reg-sub
  ::schema-loaded?
  (fn [db _]
    (-> db :routes :index :schema empty? not)))

(rf/reg-sub
  ::read-only?
  (fn [db _]
    (-> db :routes :index :read-only?)))

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
  ::aside-selection
  (fn [db _]
    (-> db :routes :index :currently-selected-ns)))

(rf/reg-sub
  ::entity-selected?
  :<- [::aside-selection]
  (fn [selection]
    (when selection (= "db.schema.entity.namespace" (namespace selection)))))

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
       :sparkline-data (:sparkline-data analytics)
       :doc doc
       :attrs (->> attrs (map name) (map qualified-kebab->title))
       :attrs-kw (->> attrs (map str))
       :preds preds
       :referenced-by (set (map (fn [ns]
                                  {:ns ns
                                   :label (qualified-kebab->title (name ns))}) referenced-by))
       :by-way-of (map (fn [ns]
                         {:ns ns
                          :label (qualified-kebab->title (name ns))}) previously-selected-ns-keys)})))

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
                                                                                                  :kw refed-ns}) namespaces)))))
                                                   attrs)]
                                   (sort-by (juxt :deprecated? (complement :unique) :ident) idents)))))))

(rf/reg-sub
  ::aside-filter
  (fn [db _]
    (-> db :routes :index :aside-filter)))

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
            :iskey unique?
            :deprecated? deprecated?
            :figure (cond
                      unique? "Key2"
                      deprecated? "Skull"
                      (not attr-type) "Ident"
                      :else "Empty")}
           (when unique? {:color "Yellow"}))))

(defn node-data-array [display-as-keywords? {:keys [idents entities]}]
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

; region = Analytic Info =======================================================
(rf/reg-sub
  ::analytics-loading?
  (fn [db _]
    (-> db :routes :index :analytics :loading?)))

(rf/reg-sub
  ::analytic-info-visible?
  (fn [db _]
    (-> db :routes :index :analytics :visible?)))

(rf/reg-sub
  ::analytic-info
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          [ns-bucket _] (ns-bucket selected-ns)]
      (some-> ns-bucket schema selected-ns :analytics))))
; endregion
