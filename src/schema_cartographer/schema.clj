(ns schema-cartographer.schema)

(defn attr-namespace [value-type {:keys [ident enumeration entity]}]
  (cond
    enumeration (keyword "cartographer.enumeration" (name enumeration))
    entity (keyword "cartographer.entity" (name entity))
    :else (keyword (if value-type ;; enums are not given db.valueType
                     "cartographer.entity"
                     "cartographer.enumeration")
                   (namespace ident))))

(defn refed-namespace [{:cartographer/keys [enumeration entity]}]
  (if enumeration
    (keyword "cartographer.enumeration" (name enumeration))
    (keyword "cartographer.entity" (name entity))))

(defn format-schema-attr
  [{:db/keys [ident doc valueType cardinality unique isComponent tupleAttrs noHistory]
    :db.attr/keys [preds]
    :cartographer/keys [also-see replaced-by references-namespaces deprecated? enumeration entity]}]
  (merge {:ident (or ident enumeration entity)
          :attribute? (boolean valueType)
          :deprecated? (boolean deprecated?)}
         (when doc {:doc doc})
         (when cardinality {:cardinality (:db/ident cardinality)})
         (when valueType {:value-type (:db/ident valueType)})
         (when-let [namespace (attr-namespace valueType {:ident ident :enumeration enumeration :entity entity})] {:namespace namespace})
         (when unique {:unique (:db/ident unique)})
         (when-let [replaced-with (or replaced-by also-see)] {:replaced-by (mapv :db/ident replaced-with)})
         (when (boolean valueType) {:references-namespaces (mapv refed-namespace references-namespaces)})
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
        attrs-key (if (= "cartographer.entity" attr-type) :ns-attrs :ns-attrs)
        e-attrs-ns (some->> entity-attrs :db/ident namespace (keyword "cartographer.entity"))
        e-preds-ns (some->> entity-preds :db/ident namespace (keyword "cartographer.entity"))]
    (merge {:namespace ns
            :doc (->> (filter #(= (name ns) (name (:ident %))) attrs) first :doc)
            :referenced-by (ns refed-by-map)
            attrs-key (remove #(= (name ns) (name (:ident %))) attrs)}
           (when (= e-attrs-ns ns) {:attrs (:db.entity/attrs entity-attrs)})
           (when (= e-preds-ns ns) {:preds (:db.entity/preds entity-preds)}))))

(defn data-map [raw-schema]
  (let [formatted-schema (map format-schema-attr (remove :cartographer/validates-namespace raw-schema))
        referenced-by-map (ns-referenced-by formatted-schema)
        entity-attrs (first (filter :db.entity/preds raw-schema))
        entity-preds (first (filter :db.entity/attrs raw-schema))
        grouped-by-namespace (group-by :namespace formatted-schema)]
    (reduce (fn [res ns-attrs]
              (let [attr-ns (-> ns-attrs first :namespace)]
                (if (= "cartographer.enumeration" (namespace attr-ns))
                  (assoc-in res [:enumerations attr-ns] (build-schema-data-for-attr ns-attrs "cartographer.enumeration" referenced-by-map entity-attrs entity-preds))
                  (assoc-in res [:entities attr-ns] (build-schema-data-for-attr ns-attrs "cartographer.entity" referenced-by-map entity-attrs entity-preds)))))
            {:enumerations {} :entities {}}
            (vals grouped-by-namespace))))

