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
          enumeration-maps (map (fn [attr-ns]
                            {:label (-> attr-ns name qualified-kebab->title)
                             :ns attr-ns}) (-> schema :enumerations keys))]
      {:entities (sort-by :label (filter filter-fn entity-maps))
       :enumerations (sort-by :label (filter filter-fn enumeration-maps))})))

(rf/reg-sub
  ::selected-ns
  (fn [db _]
    (-> db :routes :index :currently-selected-ns)))

(rf/reg-sub
  ::entity-selected?
  :<- [::selected-ns]
  (fn [selection]
    (when selection (= "cartographer.entity" (namespace selection)))))

(rf/reg-sub
  ::attr-selected?
  :<- [::selected-ns]
  (fn [selection]
    (when selection (= "cartographer.enumeration" (namespace selection)))))

(rf/reg-sub
  ::aside-selection-summary-info
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          previously-selected-ns-keys (-> db :routes :index :previously-selected-ns)
          selected-ns (-> db :routes :index :currently-selected-ns)
          ns-bucket (ns-bucket selected-ns)
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
          ns-bucket (ns-bucket selected-ns)
          details (some->> ns-bucket schema selected-ns)]
      (update details :ns-attrs (fn [attrs]
                                  (let [attributes (map (fn [attr]
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
                                    (sort-by (juxt :deprecated? (complement :unique) :ident) attributes)))))))

(rf/reg-sub
  ::schema-added-in-app
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          ns-bucket (ns-bucket selected-ns)
          new-attrs (some->> ns-bucket schema selected-ns :ns-attrs (filter :added-in-app?))
          {:keys [enumerations entities]} (select-keys schema [:enumerations :entities])
          added-entities (filter :added-in-app? (vals entities))
          added-attrs (filter :added-in-app? (vals enumerations))]
      {:new-attr-namespaces added-attrs
       :new-entity-namespaces added-entities
       :new-attrs new-attrs})))

(rf/reg-sub
  ::schema-added-in-app?
  :<- [::schema-added-in-app]
  (fn [{:keys [new-attr-namespaces new-entity-namespaces new-attrs]}]
    {:ns-have-been-added? (or (not-empty new-attr-namespaces) (not-empty new-entity-namespaces))
     :attrs-have-been-added? (not-empty new-attrs)}))

(rf/reg-sub
  ::added-ns
  :<- [::schema-added-in-app]
  (fn [{:keys [new-attr-namespaces new-entity-namespaces]}]
    (mapv (fn [{:keys [namespace]}] {:value (str/replace-first (str namespace) #"^:" "") :label (str namespace)}) (into new-attr-namespaces new-entity-namespaces))))

(rf/reg-sub
  ::added-enumerations
  :<- [::schema-added-in-app]
  (fn [{:keys [new-attrs]}]
    (mapv (fn [{:keys [ident]}] {:value (str/replace-first (str ident) #"^:" "") :label (str ident)}) new-attrs)))

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

(defn node-data-array [display-as-keywords? {:keys [enumerations entities]}]
  (let [make-node (fn [schema-type]
                    (fn [[k v]]
                      {:key (str (qualified-kebab->title (name k))
                                 (when (= schema-type :enumerations) ": Enumerations"))
                       :items (sort-by
                                (juxt :deprecated? (complement :iskey) :attr)
                                (map (partial attr->node display-as-keywords?) (:ns-attrs v)))}))
        attr-node-data (map (make-node :enumerations) enumerations)
        entity-node-data (map (make-node :entities) entities)]
    (sort-by :key (into attr-node-data entity-node-data))))

(rf/reg-sub
  ::node-data-array
  (fn [db _]
    (let [selected-ns-key (-> db :routes :index :currently-selected-ns)
          previously-selected-ns-keys (-> db :routes :index :previously-selected-ns)
          ns-bucket (ns-bucket selected-ns-key)
          schema (-> db :routes :index :schema)
          refed-ns (some->> ns-bucket schema selected-ns-key :ns-attrs (mapcat :references-namespaces))
          filtered-entities (select-keys (:entities schema) (sequence cat [previously-selected-ns-keys
                                                                           [selected-ns-key]
                                                                           refed-ns]))
          filtered-attrs (if (= :entities ns-bucket)
                            (select-keys (:enumerations schema) refed-ns)
                            (select-keys (:enumerations schema) [selected-ns-key]))
          display-as-keywords? (-> db :routes :index :settings :display-as-keywords?)]
      (when selected-ns-key
        (node-data-array display-as-keywords? {:enumerations filtered-attrs :entities filtered-entities})))))
; endregion

; region ---- Linked Data Arrays -----------------------------------------------
(defn entity->data-array [{:keys [namespace referenced-ns ident cardinality]}]
  (let [attr-type (when (str/starts-with? (str referenced-ns) ":cartographer.enumeration") ": Enumerations")]
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
        relevant-namespaces (into [selected-ns-key] (->> entities selected-ns-key :ns-attrs (mapcat :references-namespaces)))
        relevant-entities (->>
                            (select-keys entities relevant-namespaces)
                            vals
                            (filter #(= (:namespace %) selected-ns-key))
                            first
                            :ns-attrs)
        entities-with-references (filter #(-> % :references-namespaces not-empty) relevant-entities)
        single-ref-entities (mapcat separate-entity-refs entities-with-references)]
    (map entity->data-array single-ref-entities)))

(rf/reg-sub
  ::linked-data-array
  (fn [db _]
    (let [schema (-> db :routes :index :schema)
          selected-ns-key (-> db :routes :index :currently-selected-ns)
          previously-selected-ns-keys (-> db :routes :index :previously-selected-ns)]
      (if (and selected-ns-key (= "cartographer.entity" (namespace selected-ns-key)))
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
  ::ns-attr-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          ns-bucket (ns-bucket selected-ns)
          attrs (when selected-ns (->> schema ns-bucket selected-ns :ns-attrs (map :ident) sort))]
      (mapv (fn [attr] {:value (str/replace-first (str attr) #"^:" "") :label (str attr)}) attrs))))

(rf/reg-sub
  ::edit-attr-replaced-by-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          form (-> db :routes :index :edit-attr-form)
          selected-attr (-> form :attr :value (get "value") keyword)
          selected-ns (-> db :routes :index :currently-selected-ns)
          ns-bucket (ns-bucket selected-ns)
          attrs (when selected-ns (->> schema ns-bucket selected-ns :ns-attrs (map :ident) sort))
          selected-attr-map (->> db :routes :index :schema ns-bucket selected-ns :ns-attrs (filter #(= selected-attr (:ident %))) first)
          {:keys [replaced-by]} (select-keys selected-attr-map [:doc :deprecated? :replaced-by])]
      {:replaced-by-options (mapv (fn [attr] {:value (str/replace-first (str attr) #"^:" "") :label (str attr)}) (set/difference (set  attrs) (set replaced-by)))
       :existing-replaced-by (map str replaced-by)})))

(rf/reg-sub
  ::ns-attr-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          selected-ns (-> db :routes :index :currently-selected-ns)
          ns-bucket (ns-bucket selected-ns)
          existing-attrs (when selected-ns (->> schema ns-bucket selected-ns :attrs set))
          attrs (when selected-ns (->> schema ns-bucket selected-ns :ns-attrs (map :ident) sort))
          unused-attrs (remove #(contains? existing-attrs %) attrs)]
      (map (fn [attr] {:value (str/replace-first (str attr) #"^:" "")
                        :label (str attr)}) unused-attrs))))

(rf/reg-sub
  ::ns-options
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          entity-namespaces (-> schema :entities keys sort)
          attr-namespaces (-> schema :enumerations keys sort)]
      (map (fn [ns-kw] {:value (str/replace-first (str ns-kw) #"^:" "") :label (str ns-kw)}) (into entity-namespaces attr-namespaces)))))

(rf/reg-sub
  ::attr-already-deprecated?
  (fn [db [_ _]]
    (let [schema (-> db :routes :index :schema)
          form (-> db :routes :index :edit-attr-form)
          selected-attr-kw (-> form :attr :value (get "value") keyword)
          selected-ns (-> db :routes :index :currently-selected-ns)
          ns-bucket (ns-bucket selected-ns)
          attr (when selected-ns (->> schema ns-bucket selected-ns :ns-attrs (filter #(= selected-attr-kw (:ident %))) first))]
      (:deprecated? attr))))

(rf/reg-sub
  ::edit-attr-selected?
  (fn [db [_ _]]
    (let [form (-> db :routes :index :edit-attr-form)]
      (-> form :attr :value nil? not))))

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
  ::selected-attr-to-edit-ref-info
  (fn [db [_ _]]
    (let [form (-> db :routes :index :edit-attr-form)
          selected-ns (-> db :routes :index :currently-selected-ns)
          selected-attr (-> form :attr :value (get "value") keyword)
          ns-bucket (ns-bucket selected-ns)
          selected-attr-map (->> db :routes :index :schema ns-bucket selected-ns :ns-attrs (filter #(= selected-attr (:ident %))) first)]
      {:is-ref-type? (= :db.type/ref (:value-type selected-attr-map))
       :existing-references-namespaces (:references-namespaces selected-attr-map)})))

(rf/reg-sub
  ::edit-attr-ref-options
  :<- [::ns-options]
  :<- [::selected-attr-to-edit-ref-info]
  (fn [[ns-options {:keys [existing-references-namespaces]}]]
    (remove #(contains? (->> existing-references-namespaces (map str) set) (:label %)) ns-options)))
;endregion
