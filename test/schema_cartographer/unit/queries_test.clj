(ns schema-cartographer.unit.queries-test
  (:require
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [datomic.client.api :as d]
    [clojure.test :refer [deftest testing is run-tests]]
    [complete-example-schema :refer [annotation-schema-tx ice-cream-shop-schema]]
    [schema-cartographer.unit.utils :refer [setup-speculative-db db-conn]]
    [schema-cartographer.queries :as sut]))

(def db-name "cartographer-local-test")

(deftest query-schema
  (let [spec-db (setup-speculative-db db-name annotation-schema-tx ice-cream-shop-schema)
        expected-response (->> (io/resource "raw-schema.edn")
                               slurp
                               edn/read-string
                               (map #(dissoc % :db.attr/preds))
                               (map #(dissoc % :db.entity/attrs))
                               (map #(dissoc % :db.entity/preds))
                               set)
        actual-response (->> (sut/annotated-schema spec-db)
                             (map #(dissoc % :db.attr/preds))
                             (map #(dissoc % :db.entity/attrs))
                             (map #(dissoc % :db.entity/preds))
                             set)]
    (is (= expected-response actual-response))))
