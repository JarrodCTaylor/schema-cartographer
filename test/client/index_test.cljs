(ns client.index-test
  (:require
    [day8.re-frame.test :as rf-test]
    [re-frame.core :as rf]
    [clojure.edn :as edn]
    [clojure.set :as set]
    [cljs.test :refer-macros [deftest testing is]]
    [client.config :as config]
    [client.utils.form-field-validators]
    [client.test-resources.ci-globals :refer [global-stubs]]
    [client.test-resources.schema-data :refer [schema]]
    [client.test-resources.datomic-txs :refer [txs]]
    [client.test-resources.verbose-is-equal :refer [is=]]
    [client.events :as shared-events]
    [client.routes.index.events :as sut-events]
    [client.routes.index.subs :as sut-subs]))

(when config/ci (global-stubs))

(deftest node-data-array-subscription
  (rf-test/run-test-sync
    (rf/dispatch [::shared-events/initialize-db])
    (let [first-expected-result [{:key "Employee"
                                  :items [{:attr "Name: Tuple"        :iskey true  :deprecated? nil :figure "Key2" :color "#464B52"}
                                          {:attr "First Name: String" :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Last Name: String"  :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}]}
                                 {:key "Store"
                                  :items [{:attr "Id: Uuid" :iskey true   :deprecated? nil :figure "Key2" :color "#464B52"}
                                          {:attr "Address: String" :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Capricious Accounting Department Id: String" :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Employees: Ref"                              :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}]}]
          second-expected-result [{:key "Cone Type: Enumerations"
                                   :items [{:attr "Cake"   :iskey false :deprecated? nil  :figure "Ident" :color "#464B52"}
                                           {:attr "Sugar"  :iskey false :deprecated? nil  :figure "Ident" :color "#464B52"}
                                           {:attr "Waffle" :iskey false :deprecated? nil  :figure "Ident" :color "#464B52"}
                                           {:attr "Gravy"  :iskey false :deprecated? true :figure "Skull" :color "#464B52"}]}
                                  {:key "Ice Cream Flavor: Enumerations"
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
                                   :items [{:attr "Id: Uuid"                                    :iskey true   :deprecated? nil  :figure "Key2" :color "#464B52"}
                                           {:attr "Address: String"                             :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}
                                           {:attr "Capricious Accounting Department Id: String" :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}
                                           {:attr "Employees: Ref"                              :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}]}]
          node-data-array (rf/subscribe [::sut-subs/node-data-array])]
      (rf/dispatch [::sut-events/load-schema schema])
      (is= nil @node-data-array)
      (rf/dispatch [::sut-events/select-ns :cartographer.entity/store])
      (is= first-expected-result @node-data-array)
      (rf/dispatch [::sut-events/select-ns :cartographer.entity/sale])
      (is= second-expected-result @node-data-array))))

(deftest creating-linked-data-array
  (rf-test/run-test-sync
    (rf/dispatch [::shared-events/initialize-db])
    (let [first-expected-result [{:from "Store" :to "Employee" :text "Employees" :toText "*"}]
          second-expected-result [{:from "Sale" :to "Ice Cream Flavor: Enumerations" :text "Flavor" :toText "1"}
                                  {:from "Sale" :to "Cone Type: Enumerations" :text "Cone" :toText "1"}
                                  {:from "Sale" :to "Licensed Retailer" :text "Location" :toText "1"}
                                  {:from "Sale" :to "Store" :text "Location" :toText "1"}]
          linked-data-array (rf/subscribe [::sut-subs/linked-data-array])]
      (rf/dispatch [::sut-events/load-schema schema])
      (is= [] @linked-data-array)
      (rf/dispatch [::sut-events/select-ns :cartographer.entity/store])
      (is= first-expected-result @linked-data-array)
      (rf/dispatch [::sut-events/select-ns :cartographer.entity/sale])
      (is= second-expected-result @linked-data-array))))

(deftest creating-new-attrs

  (testing "Basic entity attr value type string with doc string"
    (rf-test/run-test-sync
      (rf/dispatch [::shared-events/initialize-db])
      (let [schema-map (rf/subscribe [::sut-subs/raw-schema-data-map])
            expected-result {:namespace :cartographer.entity/store
                             :ident :store/simple-attr
                             :value-type :db.type/string
                             :cardinality :db.cardinality/one
                             :attribute? true
                             :added-in-app? true
                             :deprecated? false
                             :doc "Doc string for test purposes"}]
        (rf/dispatch [::sut-events/load-schema schema])
        (rf/dispatch [::sut-events/select-ns :cartographer.entity/store])
        (is= [] (->> @schema-map :entities :cartographer.entity/store :ns-attrs (filter #(= :store/simple-attr (:ident %)))))
        (rf/dispatch [::shared-events/reset-and-clear-form-field-values :index :new-entity-attr-form {:cardinality {"value" "one" "label" "One"}
                                                                                                      :deprecated {"value" false "label" "False"}
                                                                                                      :unique {"value" false "label" "False"}
                                                                                                      :is-component {"value" false "label" "False"}
                                                                                                      :no-history {"value" false "label" "False"}}])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-attr-form :attr "simple attr"])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-attr-form :doc "Doc string for test purposes"])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-attr-form :value-type {"value" "string" "label" "String"}])
        (rf/dispatch [::shared-events/submit-if-form-valid :index :new-entity-attr-form [::sut-events/add-new-entity-attr]])
        (is= expected-result (->> @schema-map :entities :cartographer.entity/store :ns-attrs (filter #(= :store/simple-attr (:ident %))) first)))))

  (testing "Entity attr value type ref updates appropriate ns"
    (rf-test/run-test-sync
      (rf/dispatch [::shared-events/initialize-db])
      (let [schema-map (rf/subscribe [::sut-subs/raw-schema-data-map])
            expected-result {:namespace :cartographer.entity/store
                             :ident :store/test-ref-attr
                             :value-type :db.type/ref
                             :references-namespaces [:cartographer.enumeration/ice-cream-flavor
                                                     :cartographer.enumeration/cone-type]
                             :cardinality :db.cardinality/one
                             :added-in-app? true
                             :deprecated? false
                             :attribute? true}]
        (rf/dispatch [::sut-events/load-schema schema])
        (rf/dispatch [::sut-events/select-ns :cartographer.entity/store])
        (is= [] (->> @schema-map :entities :cartographer.entity/store :ns-attrs (filter #(= :store/test-ref-ident (:ident %)))))
        (is= [:cartographer.entity/sale] (->> @schema-map :enumerations :cartographer.enumeration/ice-cream-flavor :referenced-by))
        (is= [:cartographer.entity/sale] (->> @schema-map :enumerations :cartographer.enumeration/cone-type :referenced-by))
        (rf/dispatch [::shared-events/reset-and-clear-form-field-values :index :new-entity-attr-form {:cardinality {"value" "one" "label" "One"}
                                                                                                      :deprecated {"value" false "label" "False"}
                                                                                                      :unique {"value" false "label" "False"}
                                                                                                      :is-component {"value" false "label" "False"}
                                                                                                      :no-history {"value" false "label" "False"}}])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-attr-form :attr ":test-ref-attr"])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-attr-form :value-type {"value" "ref" "label" "Reference"}])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-attr-form :ref-namespaces [{"value" "cartographer.enumeration/ice-cream-flavor"
                                                                                                          "label" ":cartographer.enumeration/ice-cream-flavor"}
                                                                                                         {"value" "cartographer.enumeration/cone-type"
                                                                                                          "label" ":cartographer.enumeration/cone-type"}]])
        (rf/dispatch [::shared-events/submit-if-form-valid :index :new-entity-attr-form [::sut-events/add-new-entity-attr]])
        (is= expected-result (->> @schema-map :entities :cartographer.entity/store :ns-attrs (filter #(= :store/test-ref-attr (:ident %))) first))
        (is= [:cartographer.entity/sale :cartographer.entity/store] (->> @schema-map :enumerations :cartographer.enumeration/ice-cream-flavor :referenced-by))
        (is= [:cartographer.entity/sale :cartographer.entity/store] (->> @schema-map :enumerations :cartographer.enumeration/cone-type :referenced-by))))))

(deftest schema-map->datomic-txs

  (testing "Enumerations"
    (let [expected-result [{:db/id ":cartographer.enumeration/cone-type"
                            :cartographer/enumeration :cone-type
                            :db/doc "Ice cream cone options currently available in store."}
                           {:db/id ":cone-type/sugar"
                            :db/ident :cone-type/sugar}
                           {:db/id ":cone-type/cake"
                            :db/ident :cone-type/cake}
                           {:db/id ":cone-type/gravy"
                            :cartographer/deprecated? true
                            :cartographer/replaced-by [{:db/ident :cone-type/cake}]
                            :db/ident :cone-type/gravy}
                           {:db/id ":cone-type/waffle"
                            :db/ident :cone-type/waffle}
                           {:db/id ":cartographer.enumeration/ice-cream-flavor"
                            :cartographer/enumeration :ice-cream-flavor
                            :db/doc "Ice cream flavor options currently available in store."}
                           {:db/id ":ice-cream-flavor/vanilla"
                            :db/ident :ice-cream-flavor/vanilla}
                           {:db/id ":ice-cream-flavor/strawberry"
                            :db/ident :ice-cream-flavor/strawberry}
                           {:db/id ":ice-cream-flavor/chocolate"
                            :db/ident :ice-cream-flavor/chocolate}]]
      (is= expected-result (sut-events/schema-map->datomic-txs (select-keys schema [:enumerations])))))

  (testing "Basic Entities"
    (let [data-map {:entities {:cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                          :doc "An entity representing a single ice cream cone sale"
                                                          :referenced-by nil
                                                          :ns-attrs [{:references-namespaces [:cartographer.entity/licensed-retailer :cartographer.entity/store]
                                                                      :ident :sale/location
                                                                      :attribute? true
                                                                      :cardinality :db.cardinality/one
                                                                      :doc "A reference to a store or licensed retailer entity"
                                                                      :namespace :cartographer.entity/sale
                                                                      :value-type :db.type/ref}
                                                                     {:unique :db.unique/identity
                                                                      :ident :sale/id
                                                                      :attribute? true
                                                                      :cardinality :db.cardinality/one
                                                                      :doc "Unique id assigned to each sale"
                                                                      :namespace :cartographer.entity/sale
                                                                      :value-type :db.type/uuid}
                                                                     {:references-namespaces [:cartographer.enumeration/ice-cream-flavor]
                                                                      :ident :sale/flavor
                                                                      :attribute? true
                                                                      :cardinality :db.cardinality/one
                                                                      :doc "A reference to ice-cream-flavor ident"
                                                                      :namespace :cartographer.entity/sale
                                                                      :value-type :db.type/ref}
                                                                     {:references-namespaces [:cartographer.enumeration/cone-type]
                                                                      :ident :sale/cone
                                                                      :attribute? true
                                                                      :cardinality :db.cardinality/one
                                                                      :doc "A reference to cone ident"
                                                                      :namespace :cartographer.entity/sale
                                                                      :value-type :db.type/ref}]}}}
          expected-result [{:db/id ":cartographer.entity/sale"
                            :cartographer/entity :sale
                            :db/doc "An entity representing a single ice cream cone sale"}
                           {:db/id ":sale/id"
                            :db/ident :sale/id
                            :db/valueType {:db/ident :db.type/uuid}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db/unique {:db/ident :db.unique/identity}
                            :db/doc "Unique id assigned to each sale"}
                           {:db/id ":sale/flavor"
                            :db/ident :sale/flavor
                            :db/valueType {:db/ident :db.type/ref}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :cartographer/references-namespaces [":cartographer.enumeration/ice-cream-flavor"]
                            :db/doc "A reference to ice-cream-flavor ident"}
                           {:db/id ":sale/cone"
                            :db/ident :sale/cone
                            :db/valueType {:db/ident :db.type/ref}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :cartographer/references-namespaces [":cartographer.enumeration/cone-type"]
                            :db/doc "A reference to cone ident"}
                           {:db/id ":sale/location"
                            :db/ident :sale/location
                            :db/valueType {:db/ident :db.type/ref}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :cartographer/references-namespaces [":cartographer.entity/licensed-retailer" ":cartographer.entity/store"]
                            :db/doc "A reference to a store or licensed retailer entity"}]
          actual-result (sut-events/schema-map->datomic-txs data-map)]
      (is= (set expected-result) (set actual-result))))

  (testing "Complex Entities: Validated With Tuples. Ensure tuples come last"
    (let [data-map {:entities {:cartographer.entity/employee {:namespace :cartographer.entity/employee
                                                              :doc "An entity representing an individual employee"
                                                              :referenced-by [:cartographer.entity/store
                                                                              :cartographer.entity/licensed-retailer]
                                                              :attrs [:employee/first-name :employee/last-name]
                                                              :preds ['i-am-not-real.entity-preds/valid-employee-name?]
                                                              :ns-attrs [{:unique :db.unique/identity
                                                                          :ident :employee/name
                                                                          :attribute? true
                                                                          :cardinality :db.cardinality/one
                                                                          :doc "Employee's must have a unique combination of first and last names."
                                                                          :namespace :cartographer.entity/employee
                                                                          :value-type :db.type/tuple
                                                                          :tuple-attrs [:employee/first-name :employee/last-name]}
                                                                         {:ident :employee/first-name
                                                                          :attribute? true
                                                                          :cardinality :db.cardinality/one
                                                                          :doc "Employee's first name"
                                                                          :namespace :cartographer.entity/employee
                                                                          :value-type :db.type/string}
                                                                         {:ident :employee/last-name
                                                                          :attribute? true
                                                                          :cardinality :db.cardinality/one
                                                                          :doc "Employee's last name"
                                                                          :namespace :cartographer.entity/employee
                                                                          :value-type :db.type/string
                                                                          :attr-preds ['i-am-not-real.attr-preds/valid-last-name?]}]}}}
          expected-result [{:db/id ":cartographer.entity/employee"
                            :cartographer/entity :employee
                            :db/doc "An entity representing an individual employee"}
                           {:db/id ":employee/first-name"
                            :db/ident :employee/first-name
                            :db/valueType {:db/ident :db.type/string}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db/doc "Employee's first name"}
                           {:db/id ":employee/last-name"
                            :db/ident :employee/last-name
                            :db/valueType {:db/ident :db.type/string}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db.attr/preds ['i-am-not-real.attr-preds/valid-last-name?]
                            :db/doc "Employee's last name"}
                           {:db/id ":employee/name"
                            :db/ident :employee/name
                            :db/valueType {:db/ident :db.type/tuple}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db/unique {:db/ident :db.unique/identity}
                            :db/tupleAttrs [:employee/first-name :employee/last-name]
                            :db/doc "Employee's must have a unique combination of first and last names."}
                           {:db/ident :employee/validate
                            :cartographer/validates-namespace {:db/ident :cartographer.entity/employee}
                            :db.entity/attrs [:employee/first-name :employee/last-name]
                            :db.entity/preds ['i-am-not-real.entity-preds/valid-employee-name?]}]
          actual-result (sut-events/schema-map->datomic-txs data-map)]
      (is= expected-result actual-result)))

  (testing "Complete Data Map To Txs"
    (let [expected-result txs
          actual-result (sut-events/schema-map->datomic-txs schema)]
      (is= (set expected-result) (set actual-result)))))

(deftest schema-updating-function

  (testing "Remove Selected NameSpaces Single"
    (let [schema {:enumerations {:cartographer.enumeration/cone-type {:namespace :cartographer.enumeration/cone-type
                                                                      :doc "Ice cream cone options currently available in store."
                                                                      :referenced-by [:cartographer.entity/sale]
                                                                      :ns-attrs []}
                                 :cartographer.enumeration/ice-cream-flavor {:namespace :cartographer.enumeration/ice-cream-flavor
                                                                             :doc "Ice cream flavor options currently available in store."
                                                                             :referenced-by [:cartographer.entity/sale]
                                                                             :ns-attrs []}}
                  :entities {:cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                        :doc "An entity representing a single ice cream cone sale"
                                                        :referenced-by nil
                                                        :ns-attrs []}
                             :cartographer.entity/store {:namespace :cartographer.entity/store
                                                         :doc "An entity representing an individual ice cream store"
                                                         :referenced-by [:cartographer.entity/sale]
                                                         :ns-attrs []}}}
          expected-result {:enumerations {:cartographer.enumeration/cone-type {:namespace :cartographer.enumeration/cone-type
                                                                               :doc "Ice cream cone options currently available in store."
                                                                               :referenced-by [:cartographer.entity/sale]
                                                                               :ns-attrs []}}
                           :entities {:cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                                 :doc "An entity representing a single ice cream cone sale"
                                                                 :referenced-by nil
                                                                 :ns-attrs []}
                                      :cartographer.entity/store {:namespace :cartographer.entity/store
                                                                  :doc "An entity representing an individual ice cream store"
                                                                  :referenced-by [:cartographer.entity/sale]
                                                                  :ns-attrs []}}}]
      (is= expected-result (sut-events/remove-selected-namespaces schema ["cartographer.enumeration/ice-cream-flavor"]))))

  (testing "Remove Selected NameSpaces Multi"
    (let [schema {:enumerations {:cartographer.enumeration/cone-type {:namespace :cartographer.enumeration/cone-type
                                                                      :doc "Ice cream cone options currently available in store."
                                                                      :referenced-by [:cartographer.entity/sale]
                                                                      :ns-attrs []}
                                 :cartographer.enumeration/ice-cream-flavor {:namespace :cartographer.enumeration/ice-cream-flavor
                                                                             :doc "Ice cream flavor options currently available in store."
                                                                             :referenced-by [:cartographer.entity/sale]
                                                                             :ns-attrs []}}
                  :entities {:cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                        :doc "An entity representing a single ice cream cone sale"
                                                        :referenced-by nil
                                                        :ns-attrs []}
                             :cartographer.entity/store {:namespace :cartographer.entity/store
                                                         :doc "An entity representing an individual ice cream store"
                                                         :referenced-by [:cartographer.entity/sale]
                                                         :ns-attrs []}}}
          expected-result {:enumerations {:cartographer.enumeration/cone-type {:namespace :cartographer.enumeration/cone-type
                                                                               :doc "Ice cream cone options currently available in store."
                                                                               :referenced-by [:cartographer.entity/sale]
                                                                               :ns-attrs []}}
                           :entities {:cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                                 :doc "An entity representing a single ice cream cone sale"
                                                                 :referenced-by nil
                                                                 :ns-attrs []}}}]
      (is= expected-result (sut-events/remove-selected-namespaces schema ["cartographer.enumeration/ice-cream-flavor" "cartographer.entity/store"]))))

  (testing "Remove References To NameSpace"
    (let [schema {:entities {:cartographer.entity/licensed-retailer {:namespace :cartographer.entity/licensed-retailer
                                                                     :ns-attrs [{:ident :licensed-retailer/employees
                                                                                 :references-namespaces [:cartographer.entity/employee]}
                                                                                {:ident :licensed-retailer/id
                                                                                 :references-namespaces []}
                                                                                {:ident :licensed-retailer/address
                                                                                 :references-namespaces []}]}
                             :cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                        :ns-attrs [{:ident :sale/flavor
                                                                    :references-namespaces [:cartographer.enumeration/ice-cream-flavor]}
                                                                   {:ident :sale/cone
                                                                    :references-namespaces [:cartographer.enumeration/cone-type]}
                                                                   {:ident :sale/id
                                                                    :references-namespaces []}
                                                                   {:ident :sale/location
                                                                    :references-namespaces [:cartographer.entity/licensed-retailer :cartographer.entity/store]}]}
                             :cartographer.entity/store {:namespace :cartographer.entity/store
                                                         :ns-attrs [{:ident :store/capricious-accounting-department-id
                                                                     :references-namespaces []}
                                                                    {:ident :store/address
                                                                     :references-namespaces []}
                                                                    {:ident :store/id
                                                                     :references-namespaces []}
                                                                    {:ident :store/employees
                                                                     :references-namespaces [:cartographer.entity/employee]}]}
                             :cartographer.entity/employee {:namespace :cartographer.entity/employee
                                                            :ns-attrs [{:ident :employee/name
                                                                        :references-namespaces []}
                                                                       {:ident :employee/last-name
                                                                        :references-namespaces []}
                                                                       {:ident :employee/first-name
                                                                        :references-namespaces []}]}}}
          expected-result {:cartographer.entity/licensed-retailer {:namespace :cartographer.entity/licensed-retailer
                                                                   :ns-attrs [{:ident :licensed-retailer/employees
                                                                               :references-namespaces [:cartographer.entity/employee]}
                                                                              {:ident :licensed-retailer/id
                                                                               :references-namespaces []}
                                                                              {:ident :licensed-retailer/address
                                                                               :references-namespaces []}]}
                           :cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                      :ns-attrs [{:ident :sale/flavor
                                                                  :references-namespaces [:cartographer.enumeration/ice-cream-flavor]}
                                                                 {:ident :sale/cone
                                                                  :references-namespaces [:cartographer.enumeration/cone-type]}
                                                                 {:ident :sale/id
                                                                  :references-namespaces []}
                                                                 {:ident :sale/location
                                                                  :references-namespaces [:cartographer.entity/licensed-retailer]}]}
                           :cartographer.entity/store {:namespace :cartographer.entity/store
                                                       :ns-attrs [{:ident :store/capricious-accounting-department-id
                                                                   :references-namespaces []}
                                                                  {:ident :store/address
                                                                   :references-namespaces []}
                                                                  {:ident :store/id
                                                                   :references-namespaces []}
                                                                  {:ident :store/employees
                                                                   :references-namespaces [:cartographer.entity/employee]}]}
                           :cartographer.entity/employee {:namespace :cartographer.entity/employee
                                                          :ns-attrs [{:ident :employee/name
                                                                      :references-namespaces []}
                                                                     {:ident :employee/last-name
                                                                      :references-namespaces []}
                                                                     {:ident :employee/first-name
                                                                      :references-namespaces []}]}}]
      (is= expected-result (sut-events/remove-refs-to-ns schema ["cartographer.entity/store"]))))

  (testing "Remove selected attrs"
    (let [schema {:entities {:cartographer.entity/licensed-retailer {:namespace :cartographer.entity/licensed-retailer
                                                                     :ns-attrs [{:ident :licensed-retailer/employees
                                                                                 :references-namespaces [:cartographer.entity/employee]}
                                                                                {:ident :licensed-retailer/id
                                                                                 :references-namespaces []}
                                                                                {:ident :licensed-retailer/address
                                                                                 :references-namespaces []}]}
                             :cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                        :ns-attrs [{:ident :sale/flavor
                                                                    :references-namespaces [:cartographer.enumeration/ice-cream-flavor]}
                                                                   {:ident :sale/cone
                                                                    :references-namespaces [:cartographer.enumeration/cone-type]}
                                                                   {:ident :sale/id
                                                                    :references-namespaces []}
                                                                   {:ident :sale/location
                                                                    :references-namespaces [:cartographer.entity/licensed-retailer :cartographer.entity/store]}]}
                             :cartographer.entity/store {:namespace :cartographer.entity/store
                                                         :ns-attrs [{:ident :store/capricious-accounting-department-id
                                                                     :references-namespaces []}
                                                                    {:ident :store/address
                                                                     :references-namespaces []}
                                                                    {:ident :store/id
                                                                     :references-namespaces []}
                                                                    {:ident :store/employees
                                                                     :references-namespaces [:cartographer.entity/employee]}]}
                             :cartographer.entity/employee {:namespace :cartographer.entity/employee
                                                            :ns-attrs [{:ident :employee/name
                                                                        :references-namespaces []}
                                                                       {:ident :employee/last-name
                                                                        :references-namespaces []}
                                                                       {:ident :employee/first-name
                                                                        :references-namespaces []}]}}}
          expected-result {:schema-without-attrs {:entities {:cartographer.entity/licensed-retailer {:namespace :cartographer.entity/licensed-retailer
                                                                                                     :ns-attrs [{:ident :licensed-retailer/employees
                                                                                                                 :references-namespaces [:cartographer.entity/employee]}
                                                                                                                {:ident :licensed-retailer/id
                                                                                                                 :references-namespaces []}
                                                                                                                {:ident :licensed-retailer/address
                                                                                                                 :references-namespaces []}]}
                                                             :cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                                                        :ns-attrs [{:ident :sale/id
                                                                                                    :references-namespaces []}
                                                                                                   {:ident :sale/location
                                                                                                    :references-namespaces [:cartographer.entity/licensed-retailer :cartographer.entity/store]}]}
                                                             :cartographer.entity/store {:namespace :cartographer.entity/store
                                                                                         :ns-attrs [{:ident :store/capricious-accounting-department-id
                                                                                                     :references-namespaces []}
                                                                                                    {:ident :store/address
                                                                                                     :references-namespaces []}
                                                                                                    {:ident :store/id
                                                                                                     :references-namespaces []}
                                                                                                    {:ident :store/employees
                                                                                                     :references-namespaces [:cartographer.entity/employee]}]}
                                                             :cartographer.entity/employee {:namespace :cartographer.entity/employee
                                                                                            :ns-attrs [{:ident :employee/name
                                                                                                        :references-namespaces []}
                                                                                                       {:ident :employee/last-name
                                                                                                        :references-namespaces []}
                                                                                                       {:ident :employee/first-name
                                                                                                        :references-namespaces []}]}}}
                           :remove-refed-by #{:cartographer.enumeration/ice-cream-flavor :cartographer.enumeration/cone-type}}
          selected-ns-attrs [{:ident :sale/flavor
                              :references-namespaces [:cartographer.enumeration/ice-cream-flavor]}
                             {:ident :sale/cone
                              :references-namespaces [:cartographer.enumeration/cone-type]}
                             {:ident :sale/id
                              :references-namespaces []}
                             {:ident :sale/location
                              :references-namespaces [:cartographer.entity/licensed-retailer :cartographer.entity/store]}]]
      (is= expected-result (sut-events/remove-selected-attrs schema :entities :ns-attrs :cartographer.entity/sale selected-ns-attrs #{:sale/cone :sale/flavor}))))

  (testing "Remove unneeded refed by"
    (let [schema {:enumerations {:cartographer.enumeration/cone-type {:namespace :cartographer.enumeration/cone-type
                                                                      :doc "Ice cream cone options currently available in store."
                                                                      :referenced-by [:cartographer.entity/sale]
                                                                      :ns-attrs []}
                                 :cartographer.enumeration/ice-cream-flavor {:namespace :cartographer.enumeration/ice-cream-flavor
                                                                             :doc "Ice cream flavor options currently available in store."
                                                                             :referenced-by [:cartographer.entity/sale]
                                                                             :ns-attrs []}}
                  :entities {:cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                        :doc "An entity representing a single ice cream cone sale"
                                                        :referenced-by nil
                                                        :ns-attrs []}
                             :cartographer.entity/store {:namespace :cartographer.entity/store
                                                         :doc "An entity representing an individual ice cream store"
                                                         :referenced-by [:cartographer.entity/sale]
                                                         :ns-attrs []}}}
          expected-result {:enumerations {:cartographer.enumeration/cone-type {:namespace :cartographer.enumeration/cone-type
                                                                               :doc "Ice cream cone options currently available in store."
                                                                               :referenced-by []
                                                                               :ns-attrs []}
                                          :cartographer.enumeration/ice-cream-flavor {:namespace :cartographer.enumeration/ice-cream-flavor
                                                                                      :doc "Ice cream flavor options currently available in store."
                                                                                      :referenced-by []
                                                                                      :ns-attrs []}}
                           :entities {:cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                                 :doc "An entity representing a single ice cream cone sale"
                                                                 :referenced-by nil
                                                                 :ns-attrs []}
                                      :cartographer.entity/store {:namespace :cartographer.entity/store
                                                                  :doc "An entity representing an individual ice cream store"
                                                                  :referenced-by [:cartographer.entity/sale]
                                                                  :ns-attrs []}}}]
      (is= expected-result (sut-events/remove-unneeded-refed-by schema :cartographer.entity/sale #{:cartographer.enumeration/ice-cream-flavor :cartographer.enumeration/cone-type})))))
