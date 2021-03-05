(ns schema-cartographer.unit.explore-test
  (:require
    [datomic.client.api :as d]
    [clojure.test :refer [deftest testing is run-tests]]
    [schema-cartographer.unit.utils :refer [setup-speculative-db]]
    [unannotated-test-schema :refer [unannotated-ice-cream-shop-schema unannotated-transactions]]
    [schema-cartographer.queries :as query]
    [schema-cartographer.explorer :as sut]
    [clojure.java.io :as io]
    [clojure.edn :as edn]))

(def db-name "cartographer-unannotated-schema")

(deftest references-namespaces-response

  (testing "References cardinality one entity multiple namespaces"
    (let [attr-query-results [{:some/attr {:db/id 74766790709088
                                           :user/first-name "FName"
                                           :user/last-name "LName"}}
                              {:some/attr {:db/id 74766790709089
                                           :user/first-name "FName2"
                                           :user/last-name "LName2"}}
                              {:some/attr {:db/id 74766790709090
                                           :group/id #uuid "C62FBFCE-F889-48C8-BC9C-A9C111C717B8"
                                           :group/name "AGroup"}}]
          expected-response [:cartographer.entity/group :cartographer.entity/user]
          actual-response (sut/references-namespaces-cardinality-one attr-query-results :some/attr nil)]
      (is (= expected-response actual-response))))

  (testing "References cardinality many entity"
    (let [attr-query-results [{:db/id 74766790709088
                               :user/first-name "FName"
                               :user/last-name "LName"}
                              {:db/id 74766790709089
                               :user/first-name "FName2"
                               :user/last-name "LName2"}
                              {:db/id 74766790709090
                               :group/id #uuid "C62FBFCE-F889-48C8-BC9C-B741AcE99421"
                               :group/name "AGroup"}
                              {:db/id 74766790709091
                               :group/id #uuid "C62FBFCE-F889-48C8-BC9C-A9C111C717B8"
                               :group/name "BGroup"}]
          expected-response [:cartographer.entity/group :cartographer.entity/user]
          actual-response (sut/references-namespaces-cardinality-many attr-query-results nil)]
      (is (= expected-response actual-response))))

    (testing "References cardinality one enumeration"
      (let [attr-query-results [{:some/attr {:db/id 92358976733257 :db/ident :enum1/thing}}]
            expected-response [:cartographer.enumeration/enum1]
            actual-response (sut/references-namespaces-cardinality-one attr-query-results :some/attr nil)]
        (is (= expected-response actual-response))))

    (testing "References cardinality one enumeration referencing multiple enum namespaces"
      (let [attr-query-results [{:db/id 92358976733257
                                 :some/attr {:db/id 1234678 :db/ident :enum1/thing}}
                                {:db/id 92768392727859
                                 :some/attr {:db/id 8765435 :db/ident :enum2/other-thing}}]
            expected-response [:cartographer.enumeration/enum1 :cartographer.enumeration/enum2]
            actual-response (sut/references-enumeration-namespaces-cardinality-one attr-query-results :some/attr nil)]
        (is (= expected-response actual-response))))

  (testing "References cardinality many enumeration single namespace"
    (let [attr-query-results [{:db/id 92358976733257 :db/ident :enum1/thing}
                              {:db/id 92358976733272 :db/ident :enum1/thing}]
          expected-response [:cartographer.enumeration/enum1]
          actual-response (sut/references-enumeration-namespaces-cardinality-many attr-query-results nil)]
      (is (= expected-response actual-response))))

  (testing "References cardinality many enumeration multiple namespace"
    (let [attr-query-results [{:db/id 92358976733257 :db/ident :enum1/thing}
                              {:db/id 92358976733272 :db/ident :enum2/thing}]
          expected-response [:cartographer.enumeration/enum1 :cartographer.enumeration/enum2]
          actual-response (sut/references-enumeration-namespaces-cardinality-many attr-query-results nil)]
      (is (= expected-response actual-response)))))

  (deftest build-reference-by-vectors-response
    (testing "build-referenced-by-vectors returns expected response"
      (let [formatted-schema {:enumerations {}
                              :entities {:cartographer.entity/attr-1 {:namespace :cartographer.entity/attr-1
                                                                     :ns-attrs [{:references-namespaces [:cartographer.entity/attr-2]}]}
                                         :cartographer.entity/attr-2 {:namespace :cartographer.entity/attr-2
                                                                      :ns-attrs [{:references-namespaces []}]}
                                         :cartographer.entity/attr-3 {:namespace :cartographer.entity/attr-3
                                                                      :ns-attrs [{:references-namespaces [:cartographer.entity/attr-2 :cartographer.entity/attr-1]}]}
                                         :cartographer.entity/attr-4 {:namespace :cartographer.entity/attr-4
                                                                      :ns-attrs [{:references-namespaces [:cartographer.enumeration/enum-1 :cartographer.enumeration/enum-2]}]}}}
            expected-response [[:cartographer.entity/attr-1 :cartographer.entity/attr-2]
                               [:cartographer.entity/attr-3 :cartographer.entity/attr-2]
                               [:cartographer.entity/attr-3 :cartographer.entity/attr-1]
                               [:cartographer.entity/attr-4 :cartographer.enumeration/enum-1]
                               [:cartographer.entity/attr-4 :cartographer.enumeration/enum-2]]
            actual-response (sut/build-referenced-by-vectors formatted-schema)]
        (is (= expected-response actual-response)))))

  (deftest unannotated-schema-exploration

    (binding [*print-namespace-maps* false]
      (let [db (setup-speculative-db db-name unannotated-ice-cream-shop-schema unannotated-transactions)
            schema (query/unannotated-schema db)
            expected-response (-> (io/resource "expected-unannotated-schema-data.edn") slurp edn/read-string)
            actual-response (sut/explore db schema nil)]
        (is (= expected-response actual-response)))))