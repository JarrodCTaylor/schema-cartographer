(ns server.unit.route-functions.schema.get-schema-test
  (:require
    [server.route-functions.schema.get-schema :as sut]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.test :refer [deftest testing is]]))

(deftest test-ns-referenced-by
  (let [raw-schema (-> (io/resource "raw-schema.edn") slurp edn/read-string)
        formatted-schema (map sut/format-schema-attr raw-schema)
        expected-response {:db.schema.entity.namespace/store [:db.schema.entity.namespace/sale]
                           :db.schema.entity.namespace/licensed-retailer [:db.schema.entity.namespace/sale]
                           :db.schema.entity.namespace/employee [:db.schema.entity.namespace/store
                                                                 :db.schema.entity.namespace/licensed-retailer]
                           :db.schema.ident.namespace/cone-type [:db.schema.entity.namespace/sale]
                           :db.schema.ident.namespace/ice-cream-flavor [:db.schema.entity.namespace/sale]}
        actual-response (sut/ns-referenced-by formatted-schema)]
    (is (= expected-response actual-response))))

(deftest test-schema-data-is-properly-formatted
  (binding [*print-namespace-maps* false]
  (let [raw-schema (-> (io/resource "raw-schema.edn") slurp edn/read-string)
        expected-schema-data (-> (io/resource "expected-schema-data.edn") slurp edn/read-string)
        actual-response (sut/schema-data raw-schema)]
    (is (= expected-schema-data actual-response)))))
