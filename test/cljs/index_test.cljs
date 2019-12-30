(ns cljs.index-test
  (:require
    [day8.re-frame.test :as rf-test]
    [re-frame.core :as rf]
    [cljs.test :refer-macros [deftest testing is]]
    [cljs.test-resources.schema-data :refer [schema]]
    [cljs.test-resources.verbose-is-equal :refer [is=]]
    [cljs.events :as shared-events]
    [cljs.routes.index.events :as sut-events]
    [cljs.routes.index.subs :as sut-subs]))

(deftest node-data-array-subscription
  (rf-test/run-test-sync
    (rf/dispatch [::shared-events/initialize-db])
    (let [first-expected-result [{:key "Employee"
                                  :items [{:attr "Name: Tuple"        :iskey true  :deprecated? nil :figure "Key2" :color "#464B52"}
                                          {:attr "First Name: String" :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Last Name: String"  :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}]}
                                 {:key "Store"
                                  :items [{:attr "Id: Uuid"                         :iskey true   :deprecated? nil :figure "Key2" :color "#464B52"}
                                          {:attr "Address: String"                  :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Capricious Accounting Id: String" :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Employees: Ref"                   :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}]}]
          second-expected-result [{:key "Cone Type: Idents"
                                   :items [{:attr "Cake"   :iskey false :deprecated? nil  :figure "Ident" :color "#464B52"}
                                           {:attr "Sugar"  :iskey false :deprecated? nil  :figure "Ident" :color "#464B52"}
                                           {:attr "Waffle" :iskey false :deprecated? nil  :figure "Ident" :color "#464B52"}
                                           {:attr "Gravy"  :iskey false :deprecated? true :figure "Skull" :color "#464B52"}]}
                                  {:key "Ice Cream Flavor: Idents"
                                   :items [{:attr "Chocolate"  :iskey false :deprecated? nil :figure "Ident" :color "#464B52"}
                                           {:attr "Strawberry" :iskey false :deprecated? nil :figure "Ident" :color "#464B52"}
                                           {:attr "Vanilla"    :iskey false :deprecated? nil :figure "Ident" :color "#464B52"}]}
                                  {:key "Licensed Retailer"
                                   :items [{:attr "Id: Uuid"        :iskey true  :deprecated? nil :figure "Key2" :color "#464B52"}
                                           {:attr "Address: String" :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}
                                           {:attr "Employees: Ref"  :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}]}
                                  {:key "Sale"
                                   :items [{:attr "Id: Uuid"      :iskey true  :deprecated? nil :figure "Key2" :color "#464B52"}
                                           {:attr "Cone: Ref"     :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}
                                           {:attr "Flavor: Ref"   :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}
                                           {:attr "Location: Ref" :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}]}
                                  {:key "Store"
                                   :items [{:attr "Id: Uuid"                         :iskey true   :deprecated? nil  :figure "Key2" :color "#464B52"}
                                           {:attr "Address: String"                  :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}
                                           {:attr "Capricious Accounting Id: String" :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}
                                           {:attr "Employees: Ref"                   :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}]}]
          node-data-array (rf/subscribe [::sut-subs/node-data-array])]
      (rf/dispatch [::sut-events/load-schema schema])
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
      (rf/dispatch [::sut-events/load-schema schema])
      (is= [] @linked-data-array)
      (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/store])
      (is= first-expected-result @linked-data-array)
      (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/sale])
      (is= second-expected-result @linked-data-array))))
