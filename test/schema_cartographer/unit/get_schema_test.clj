(ns schema-cartographer.unit.get-schema-test
  (:require
    [schema-cartographer.schema :as sut]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.test :refer [deftest testing is]]))

(deftest test-ns-referenced-by
  (binding [*print-namespace-maps* false]
    (let [raw-schema (-> (io/resource "raw-schema.edn") slurp edn/read-string)
          formatted-schema (map sut/format-schema-attr raw-schema)
          expected-response {:cartographer.entity/store [:cartographer.entity/sale :cartographer.entity/store]
                             :cartographer.entity/licensed-retailer [:cartographer.entity/sale :cartographer.entity/store]
                             :cartographer.entity/employee [:cartographer.entity/licensed-retailer
                                                            :cartographer.entity/store]
                             :cartographer.enumeration/cone-type [:cartographer.entity/sale]
                             :cartographer.enumeration/ice-cream-flavor [:cartographer.entity/sale]}
          actual-response (sut/ns-referenced-by formatted-schema)]
      (is (= expected-response actual-response)))))

(deftest test-schema-data-is-properly-formatted
  (binding [*print-namespace-maps* false]
    (let [raw-schema (-> (io/resource "raw-schema.edn") slurp edn/read-string)
          expected-schema-data (-> (io/resource "expected-schema-data.edn") slurp edn/read-string)
          actual-response (sut/data-map raw-schema)]
      (is (= expected-schema-data actual-response)))))
