(ns cli.queries
  (:require
    [datomic.client.api :as d]))

(defn schema [db]
  (let [everything (map first (d/q '[:find (pull ?e [:db/ident
                                                     {:db/valueType [:db/ident]}
                                                     {:db/cardinality [:db/ident]}
                                                     :db.attr/preds
                                                     :db/unique
                                                     :db/isComponent
                                                     :db/noHistory
                                                     :db/tupleAttrs
                                                     :db.entity/attrs
                                                     :db.entity/preds
                                                     :cartographer/enumeration
                                                     :cartographer/entity
                                                     {:cartographer/replaced-by [:db/ident]}
                                                     {:cartographer/references-namespaces [:cartographer/entity :cartographer/enumeration]}
                                                     {:cartographer/validates-namespace [:cartographer/entity]}
                                                     :cartographer/deprecated?
                                                     :db/doc])
                                     :where (or [?e :db/ident]
                                                [?e :cartographer/entity]
                                                [?e :cartographer/enumeration])]
                                   db))
        schema-attrs (->> everything
                           (filter :db/ident)
                           (filter (fn [{:db/keys [ident]}]
                                     (re-matches #"^(?!cartographer)(?!db)(?!fressian).+" (or (namespace ident) "")))))
        meta-schema-schema (filter #(-> % :db/ident not) everything)]
    (into meta-schema-schema schema-attrs)))

(defn attr-tx-instances [db a]
  (->> (d/q '[:find ?txInstant
              :in $ ?a
              :where [_ ?a _ ?tx]
                     [?tx :db/txInstant ?txInstant]] db a)
       (map first)
       sort))
