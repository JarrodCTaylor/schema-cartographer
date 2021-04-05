(ns schema-cartographer.explorer
  (:require
    [clojure.set :as set]
    [datomic.client.api :as d]
    [schema-cartographer.utils :refer [with-retry]]))

(defn query-unindexed-attr [db attr ref-search-limit]
  (map first (d/q {:query '[:find (pull ?x a)
                             :in $ ?attr a
                             :where [?x ?attr]]
                    :offset 1
                    :limit (or ref-search-limit 1000000)
                    :args [db attr [{attr ['*]}]]})))

(defn query-attr [db attr cardinality ref-search-limit]
  (let [card-many? (= :db.cardinality/many (:db/ident cardinality))
        pull-map {:index (if card-many? :aevt :avet)
                  :selector (if card-many? '[*] [{attr '[*]}])
                  :start [attr]}
        _ (println " - Analyzing" attr "references")
        return-value (try (with-retry #(d/index-pull db pull-map))
                          (catch Exception e e))]
    (cond
      (= :db.error/attribute-not-indexed ( -> return-value ex-data :db/error)) (query-unindexed-attr db attr ref-search-limit)
      (-> return-value ex-data :db/error) (throw return-value)
      :else return-value)))

(defn log-analyzed-count [{:keys [counter response]}]
  (println (str "  - " (format "%,d" counter) " relationships analyzed\n"))
  response)

(defn reduce-enumeration-references-namespaces [ref-search-limit idents]
  (reduce (fn [{:keys [counter response]} kws]
            (when (= 0 (mod counter 100000))
              (println " --" (format "%,d" counter) "processed"))
            {:counter (inc counter)
             :response (set/union response #{(or (and kws (namespace kws)) "unnamespaced")})})
          {:counter 0 :response #{}}
          (if ref-search-limit
            (take ref-search-limit idents)
            idents)))

(defn reduce-references-namespaces [ref-search-limit keys]
  (reduce (fn [{:keys [counter response]} kws]
            (when (= 0 (mod counter 100000))
              (println " --" (format "%,d" counter) "processed"))
            {:counter (inc counter)
             :response (set/union response (disj (set (map #(or (and kws (namespace %)) "unnamespaced") kws)) "db"))})
          {:counter 0 :response #{}}
          (if ref-search-limit
            (take ref-search-limit keys)
            keys)))

(defn references-enumeration-namespaces-cardinality-one [attr-query-results attr ref-search-limit]
  (with-retry #(->> attr-query-results
                    (map attr)
                    (map :db/ident)
                    (reduce-enumeration-references-namespaces ref-search-limit)
                    (log-analyzed-count)
                    (map (fn [ns] (keyword "cartographer.enumeration" ns))))))

(defn references-namespaces-cardinality-one [attr-query-results attr ref-search-limit]
  (if attr-query-results
    (let [references-enumeration? (every? #(= "db" %) (->> attr-query-results first attr keys (map (fn [k]
                                                                                                     (or (namespace k) "unnamespaced")))))]
      (if references-enumeration?
        (references-enumeration-namespaces-cardinality-one attr-query-results attr ref-search-limit)
        (with-retry #(->> attr-query-results
                          (map attr)
                          (map keys)
                          (reduce-references-namespaces ref-search-limit)
                          (log-analyzed-count)
                          (map (fn [ns] (keyword "cartographer.entity" ns)))))))
    []))

(defn references-enumeration-namespaces-cardinality-many [attr-query-results ref-search-limit]
  (with-retry #(->> attr-query-results
                    (map :db/ident)
                    (reduce-enumeration-references-namespaces ref-search-limit)
                    (log-analyzed-count)
                    (map (fn [ns] (keyword "cartographer.enumeration" ns))))))

(defn references-namespaces-cardinality-many [attr-query-results ref-search-limit]
  (if attr-query-results
    (let [references-enumeration? (every? #(= "db" %) (->> attr-query-results first keys (map (fn [k]
                                                                                                (or (namespace k) "unnamespaced")))))]
      (if references-enumeration?
        (references-enumeration-namespaces-cardinality-many attr-query-results ref-search-limit)
        (with-retry #(->> attr-query-results
                          (map keys)
                          (reduce-references-namespaces ref-search-limit)
                          (log-analyzed-count)
                          (map (fn [ns] (keyword "cartographer.entity" ns)))))))
    []))

(defn format-attribute [db {:db/keys [ident valueType cardinality unique noHistory isComponent tupleAttrs] :as attr} ref-search-limit]
  (let [attr-query-results (when (= :db.type/ref (:db/ident valueType)) (query-attr db ident cardinality ref-search-limit))
        cardinality-many? (= :db.cardinality/many (:db/ident cardinality))
        formatted-attr (cond-> {:namespace (keyword "cartographer.entity" (or (namespace ident) "unnamespaced"))
                                :ident ident
                                :doc "Automatically generated by Explorer"
                                :value-type (:db/ident valueType)
                                :cardinality (:db/ident cardinality)
                                :deprecated? false
                                :attribute? true
                                :references-namespaces (if cardinality-many?
                                                         (references-namespaces-cardinality-many attr-query-results ref-search-limit)
                                                         (references-namespaces-cardinality-one attr-query-results ident ref-search-limit))}
                               noHistory (assoc :no-history? true)
                               tupleAttrs (assoc :tuple-attrs tupleAttrs)
                               (:db.attr/preds attr) (assoc :attr-preds (:db.attr/preds attr))
                               isComponent (assoc :is-component? true)
                               unique (assoc :unique :db.unique/identity))]
    formatted-attr))

(defn assoc-ns-attrs [db res entity attr ref-search-limit]
  (update-in res [:entities entity :ns-attrs] conj (format-attribute db attr ref-search-limit)))

(defn add-namespace [db res entity attr ref-search-limit]
  (assoc-in res [:entities entity] {:namespace entity
                                    :doc "Automatically generated by Explorer"
                                    :referenced-by ()
                                    :ns-attrs [(format-attribute db attr ref-search-limit)]}))

(defn process-entity [db res {:db/keys [ident] :as attr} ref-search-limit]
  (let [entity (keyword "cartographer.entity" (or (namespace ident) "unnamespaced"))]
    (if (-> res :entities entity)
      (assoc-ns-attrs db res entity attr ref-search-limit)
      (add-namespace db res entity attr ref-search-limit))))

(defn format-enumeration [{:db/keys [ident]}]
    {:namespace (keyword "cartographer.enumeration" (or (namespace ident) "unnamespaced"))
     :ident ident
     :attribute? false
     :deprecated? false})

(defn assoc-enumeration-ns-attrs [res entity attr]
  (update-in res [:enumerations entity :ns-attrs] conj (format-enumeration attr)))

(defn add-enumeration-namespace [res entity attr]
  (assoc-in res [:enumerations entity] {:namespace entity
                                        :doc "Automatically generated by Explorer"
                                        :referenced-by ()
                                        :ns-attrs [(format-enumeration attr)]}))

(defn process-enumeration
  [res {:db/keys [ident] :as attr}]
  (let [enum (keyword "cartographer.enumeration" (or (namespace ident) "unnamespaced"))]
    (if (-> res :enumerations enum)
      (assoc-enumeration-ns-attrs res enum attr)
      (add-enumeration-namespace res enum attr))))

(defn assoc-entity-predicate [res entity {:db.entity/keys [attrs preds]}]
  (update-in res [:entities entity] merge {:attrs attrs
                                           :preds preds}))

(defn process-entity-predicate
  [res attr]
  (let [entity (keyword "cartographer.entity" (or (namespace (:db/ident attr)) "unnamespaced"))]
    (if (-> res :entities entity)
      (assoc-entity-predicate res entity attr)
      res)))

(defn format-raw-schema-query
  "Given the output of cli.queries/unannotated-schema and a db. Build end map of schema for use in UI.
   The output of this function lacks `:referenced-by` vectors on the namespaces"
  [db schema ref-search-limit]
  (reduce
    (fn [res {:db/keys [valueType] :as attr}]
      (cond
        (:db.entity/preds attr) (process-entity-predicate res attr)
        valueType (process-entity db res attr ref-search-limit)
        :else (process-enumeration res attr)))
    {:enumerations {}
     :entities {}}
    schema))

(defn build-referenced-by-vectors [formatted-schema]
  (mapcat seq (reduce (fn [res entity]
                        (let [references (filter not-empty (map (fn [attr] (:references-namespaces attr)) (:ns-attrs entity)))
                              referenced-by (:namespace entity)
                              ref-maps (mapcat (fn [refs]
                                                 (map (fn [ref]
                                                        [referenced-by ref]) refs))
                                               references)]
                          (if (not-empty references)
                            (conj res ref-maps)
                            res))) [] (vals (:entities formatted-schema)))))

(defn explore [db schema ref-search-limit]
  (let [formatted-schema (format-raw-schema-query db schema ref-search-limit)
        referenced-by-vectors (build-referenced-by-vectors formatted-schema)]
    (reduce (fn [res [referenced-by ref]]
              (let [entity? (= "cartographer.entity" (or (namespace ref) "unnamespaced"))]
                (if entity?
                  (update-in res [:entities ref :referenced-by] conj referenced-by)
                  (update-in res [:enumerations ref :referenced-by] conj referenced-by))))
            formatted-schema referenced-by-vectors)))