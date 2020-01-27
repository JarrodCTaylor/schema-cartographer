(ns cli.schema)

(defn ident-namespace [value-type ident]
  (cond
    (= "db.schema.ident.namespace" (namespace ident)) ident
    (= "db.schema.entity.namespace" (namespace ident)) ident
    :else (keyword (if value-type ;; idents are not given db.valueType
                     "db.schema.entity.namespace"
                     "db.schema.ident.namespace")
                   (namespace ident))))

(defn format-schema-attr
  [{:db/keys [ident doc valueType cardinality unique isComponent tupleAttrs noHistory]
    :db.attr/keys [preds]
    :db.schema/keys [also-see replaced-by references-namespaces deprecated?]}]
  (merge {:ident ident
          :attribute? (boolean valueType)
          :deprecated? (boolean deprecated?)}
         (when doc {:doc doc})
         (when cardinality {:cardinality (:db/ident cardinality)})
         (when valueType {:value-type (:db/ident valueType)})
         (when-let [namespace (ident-namespace valueType ident)] {:namespace namespace})
         (when unique {:unique (:db/ident unique)})
         (when-let [replaced-with (or replaced-by also-see)] {:replaced-by (map :db/ident replaced-with)})
         (when (boolean valueType) {:references-namespaces (map :db/ident references-namespaces)})
         (when isComponent {:is-component? isComponent})
         (when noHistory {:no-history? noHistory})
         (when tupleAttrs {:tuple-attrs tupleAttrs})
         (when preds {:attr-preds preds})))

(defn ns-referenced-by
  "Returns a map in the shape of {referenced-namespace [referenced-by-ns referenced-by-ns]}"
  [formatted-schema]
  (let [attrs-with-refs (filter #(not-empty (:references-namespaces %)) formatted-schema)
        reference-maps (reduce #(into %1 (for [ns (:references-namespaces %2)]
                                           {:refed-from (:namespace %2) :ref-to ns}))
                               []
                               attrs-with-refs)]
    (reduce (fn [res {:keys [refed-from ref-to]}]
              (update res ref-to #(conj % refed-from)))
            {}
            reference-maps)))

(defn build-schema-data-for-attr [attrs attr-type refed-by-map entity-attrs entity-preds]
  (let [ns (-> attrs first :namespace)
        attrs-key (if (= "db.schema.entity.namespace" attr-type) :ns-entities :ns-idents)
        e-attrs (-> entity-attrs :db.schema/validates-namespace :db/ident)
        e-preds (-> entity-preds :db.schema/validates-namespace :db/ident)]
    (merge {:namespace ns
            :doc (->> (filter #(= attr-type (namespace (:ident %))) attrs) first :doc)
            :referenced-by (ns refed-by-map)
            attrs-key (remove #(= attr-type (namespace (:ident %))) attrs)}
           (when (= e-attrs ns) {:attrs (:db.entity/attrs entity-attrs)})
           (when (= e-preds ns) {:preds (:db.entity/preds entity-preds)}))))

(defn data-map [raw-schema]
  (let [formatted-schema (map format-schema-attr (remove :db.schema/validates-namespace raw-schema))
        referenced-by-map (ns-referenced-by formatted-schema)
        entity-attrs (first (filter :db.entity/preds raw-schema))
        entity-preds (first (filter :db.entity/attrs raw-schema))
        grouped-by-namespace (group-by :namespace formatted-schema)]
    (reduce (fn [res ns-attrs]
              (let [attr-ns (-> ns-attrs first :namespace)]
                (if (= "db.schema.ident.namespace" (namespace attr-ns))
                  (assoc-in res [:idents attr-ns] (build-schema-data-for-attr ns-attrs "db.schema.ident.namespace" referenced-by-map entity-attrs entity-preds))
                  (assoc-in res [:entities attr-ns] (build-schema-data-for-attr ns-attrs "db.schema.entity.namespace" referenced-by-map entity-attrs entity-preds)))))
            {:idents {} :entities {}}
            (vals grouped-by-namespace))))

