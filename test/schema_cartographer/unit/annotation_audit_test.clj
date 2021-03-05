(ns schema-cartographer.unit.annotation-audit-test
  (:require
    [schema-cartographer.annotation-audit :as sut]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.test :refer [deftest testing is]]))

(def schema-with-full-annotations
  (-> (io/resource "raw-schema.edn") slurp edn/read-string))

(def schema-with-unannotated-ns
  (let [raw-schema (-> (io/resource "raw-schema.edn") slurp edn/read-string)
        unannotated-schema (-> (io/resource "unannotated-ns.edn") slurp edn/read-string)]
    (into raw-schema unannotated-schema)))

(def schema-with-missing-ns-ref
  (let [raw-schema (-> (io/resource "raw-schema.edn") slurp edn/read-string)
        missing-ns-ref-schema (-> (io/resource "missing-ns-ref.edn") slurp edn/read-string)]
    (into raw-schema missing-ns-ref-schema)))

(def schema-with-missing-ns-ref-and-unannotated-ns
  (let [raw-schema (-> (io/resource "raw-schema.edn") slurp edn/read-string)
        unannotated-schema (-> (io/resource "unannotated-ns.edn") slurp edn/read-string)
        missing-ns-ref-schema (-> (io/resource "missing-ns-ref.edn") slurp edn/read-string)]
    (sequence cat [raw-schema missing-ns-ref-schema unannotated-schema])))

(deftest test-audit-schema-on-fully-annotated-schema
  (let [expected-response {:unannotated-attrs {} :missing-ns-refs []}
        actual-response (sut/audit-schema schema-with-full-annotations)]
    (is (= expected-response actual-response))))

(deftest test-audit-schema-can-identified-unannotated-ns
  (let [expected-response {:unannotated-attrs {"zorg" [{:ident :zorg/first-name, :kw "first-name", :ns "zorg"}
                                                       {:ident :zorg/last-name, :kw "last-name", :ns "zorg"}]}
                           :missing-ns-refs []}
        actual-response (sut/audit-schema schema-with-unannotated-ns)]
    (is (= expected-response actual-response))))

(deftest test-audit-schema-can-identified-missing-ns-refs
  (let [expected-response {:unannotated-attrs {}
                           :missing-ns-refs [:employee/pieces-of-flair]}
        actual-response (sut/audit-schema schema-with-missing-ns-ref)]
    (is (= expected-response actual-response))))

(deftest test-audit-schema-can-identified-missing-ns-refs-and-unannotated-ns
  (let [expected-response {:unannotated-attrs {"zorg" [{:ident :zorg/first-name, :kw "first-name", :ns "zorg"}
                                                       {:ident :zorg/last-name, :kw "last-name", :ns "zorg"}]}
                           :missing-ns-refs [:employee/pieces-of-flair]}
        actual-response (sut/audit-schema schema-with-missing-ns-ref-and-unannotated-ns)]
    (is (= expected-response actual-response))))
