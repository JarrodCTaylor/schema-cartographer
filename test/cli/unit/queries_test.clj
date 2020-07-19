(ns cli.unit.queries-test
  (:require
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [datomic.client.api :as d]
    [clojure.test :refer [deftest testing is run-tests]]
    [complete-example-schema :refer [annotation-schema-tx ice-cream-shop-schema]]
    [cli.queries :as sut]))

(def db-name "cartographer-local-test")

(def client (d/client {:system "cartographer-test"
                       :server-type :dev-local}))

(defn ensure-db []
  (when-not (some #{db-name} (d/list-databases client {}))
    (d/create-database client {:db-name db-name})))

(defn db-conn []
  (ensure-db)
  (d/connect client {:db-name db-name}))

(defn setup []
  (let [conn (db-conn)]
    (d/transact conn {:tx-data annotation-schema-tx})
    (d/transact conn {:tx-data ice-cream-shop-schema})))

(deftest test-placeholder
  (setup)
  (let [expected-response (->> (io/resource "raw-schema.edn")
                               slurp
                               edn/read-string
                               (map #(dissoc % :db.attr/preds))
                               (map #(dissoc % :db.entity/attrs))
                               (map #(dissoc % :db.entity/preds))
                               set)
        actual-response (->> (sut/schema (d/db (db-conn)))
                             (map #(dissoc % :db.attr/preds))
                             (map #(dissoc % :db.entity/attrs))
                             (map #(dissoc % :db.entity/preds))
                             set)]
    (is (= expected-response actual-response))))
