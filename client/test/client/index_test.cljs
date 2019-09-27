(ns client.index-test
  (:require
    [cljs.test :refer-macros [deftest testing is]]
    [client.test-resources.schema-data :refer [schema]]
    [client.test-resources.verbose-is-equal :refer [is=]]
    [day8.re-frame.test :as rf-test]
    [client.events :as shared-events]
    [client.routes.index.events :as sut-events]
    [client.routes.index.subs :as sut-subs]
    [re-frame.core :as rf]))

(deftest node-data-array-subscription
  (rf-test/run-test-sync
    (rf/dispatch [::shared-events/initialize-db])
    (let [first-expected-result [{:key "Employee"
                                  :items [{:attr "Name: Tuple" :iskey true  :deprecated? nil :figure "Key2" :color "Yellow"}
                                          {:attr "First Name: String" :iskey false :deprecated? nil :figure "Empty"}
                                          {:attr "Last Name: String" :iskey false :deprecated? nil :figure "Empty"}]}
                                 {:key "Store"
                                  :items [{:attr "Id: Uuid" :iskey true   :deprecated? nil  :figure "Key2" :color "Yellow"}
                                          {:attr "Address: String" :iskey false  :deprecated? nil  :figure "Empty"}
                                          {:attr "Capricious Accounting Id: String" :iskey false :deprecated? nil :figure "Empty"}
                                          {:attr "Employees: Ref" :iskey false :deprecated? nil :figure "Empty"}]}]
          second-expected-result [{:key "Cone Type: Idents"
                                   :items [{:attr "Cake" :iskey false :deprecated? nil  :figure "Ident"}
                                           {:attr "Sugar" :iskey false :deprecated? nil  :figure "Ident"}
                                           {:attr "Waffle" :iskey false :deprecated? nil  :figure "Ident"}
                                           {:attr "Gravy" :iskey false :deprecated? true :figure "Skull"}]}
                                  {:key "Ice Cream Flavor: Idents"
                                   :items [{:attr "Chocolate" :iskey false :deprecated? nil :figure "Ident"}
                                           {:attr "Strawberry" :iskey false :deprecated? nil :figure "Ident"}
                                           {:attr "Vanilla" :iskey false :deprecated? nil :figure "Ident"}]}
                                  {:key "Licensed Retailer"
                                   :items [{:attr "Id: Uuid" :iskey true  :deprecated? nil :figure "Key2" :color "Yellow"}
                                           {:attr "Address: String" :iskey false :deprecated? nil :figure "Empty"}
                                           {:attr "Employees: Ref" :iskey false :deprecated? nil :figure "Empty"}]}
                                  {:key "Sale"
                                   :items [{:attr "Id: Uuid" :iskey true  :deprecated? nil :figure "Key2" :color "Yellow"}
                                           {:attr "Cone: Ref" :iskey false :deprecated? nil :figure "Empty"}
                                           {:attr "Flavor: Ref" :iskey false :deprecated? nil :figure "Empty"}
                                           {:attr "Location: Ref" :iskey false :deprecated? nil :figure "Empty"}]}
                                  {:key "Store"
                                   :items [{:attr "Id: Uuid" :iskey true   :deprecated? nil  :figure "Key2" :color "Yellow"}
                                           {:attr "Address: String" :iskey false  :deprecated? nil  :figure "Empty"}
                                           {:attr "Capricious Accounting Id: String" :iskey false :deprecated? nil :figure "Empty"}
                                           {:attr "Employees: Ref" :iskey false :deprecated? nil :figure "Empty"}]}]
          node-data-array (rf/subscribe [::sut-subs/node-data-array])]
      (rf/dispatch [::sut-events/get-schema-success schema])
      (is= nil @node-data-array)
      (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/store])
      (is= first-expected-result @node-data-array)
      (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/sale])
      (is= second-expected-result @node-data-array))))

(deftest creating-linked-data-array
  (rf-test/run-test-sync
    (rf/dispatch [::shared-events/initialize-db])
    (let [first-expected-result [{:from "Store" :to "Employee" :text "Employees" :toText "*"}]
          second-expected-result [{:from "Sale" :to "Ice Cream Flavor: Idents" :text "Flavor" :toText "1"}
                                  {:from "Sale" :to "Cone Type: Idents" :text "Cone" :toText "1"}
                                  {:from "Sale" :to "Licensed Retailer" :text "Location" :toText "1"}
                                  {:from "Sale" :to "Store" :text "Location" :toText "1"}]
          linked-data-array (rf/subscribe [::sut-subs/linked-data-array])]
      (rf/dispatch [::sut-events/get-schema-success schema])
      (is= [] @linked-data-array)
      (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/store])
      (is= first-expected-result @linked-data-array)
      (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/sale])
      (is= second-expected-result @linked-data-array))))
