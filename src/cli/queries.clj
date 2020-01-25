(ns cli.queries
  (:require
    [datomic.client.api :as d]))

(defn schema [db]
  (->> (d/q '[:find (pull ?element [:db/ident
                                    {:db/valueType [:db/ident]}
                                    {:db/cardinality [:db/ident]}
                                    :db.attr/preds
                                    :db/unique
                                    :db/isComponent
                                    :db/noHistory
                                    :db/tupleAttrs
                                    :db.entity/attrs
                                    :db.entity/preds
                                    {:db.schema/replaced-by [:db/ident]}
                                    {:db.schema/also-see [:db/ident]}
                                    :db.schema/references-namespaces
                                    :db.schema/validates-namespace
                                    :db.schema/deprecated?
                                    :db/doc])
              :where [?e :db/ident ?element]]
            db)
       (map first)
       (filter #(re-matches #"^(db.schema|(?!db))(?!fressian).+" (-> % :db/ident namespace)))))

(defn attr-tx-instances [db a]
  (->> (d/q '[:find ?txInstant
              :in $ ?a
              :where [_ ?a _ ?tx]
                     [?tx :db/txInstant ?txInstant]] db a)
       (map first)
       sort))
