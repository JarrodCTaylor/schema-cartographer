(ns client.index-test
  (:require
    [day8.re-frame.test :as rf-test]
    [re-frame.core :as rf]
    [clojure.edn :as edn]
    [clojure.set :as set]
    [cljs.test :refer-macros [deftest testing is]]
    [client.utils.form-field-validators]
    [client.test-resources.schema-data :refer [schema]]
    [client.test-resources.datomic-txs :refer [txs]]
    [client.test-resources.verbose-is-equal :refer [is=]]
    [client.events :as shared-events]
    [client.routes.index.events :as sut-events]
    [client.routes.index.subs :as sut-subs]))

(deftest node-data-array-subscription
  (rf-test/run-test-sync
    (rf/dispatch [::shared-events/initialize-db])
    (let [first-expected-result [{:key "Employee"
                                  :items [{:attr "Name: Tuple"        :iskey true  :deprecated? nil :figure "Key2" :color "#464B52"}
                                          {:attr "First Name: String" :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Last Name: String"  :iskey false :deprecated? nil :figure "Empty" :color "#464B52"}]}
                                 {:key "Store"
                                  :items [{:attr "Id: Uuid"                                    :iskey true   :deprecated? nil :figure "Key2" :color "#464B52"}
                                          {:attr "Address: String"                             :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Capricious Accounting Department Id: String" :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}
                                          {:attr "Employees: Ref"                              :iskey false  :deprecated? nil :figure "Empty" :color "#464B52"}]}]
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
                                   :items [{:attr "Id: Uuid"                                    :iskey true   :deprecated? nil  :figure "Key2" :color "#464B52"}
                                           {:attr "Address: String"                             :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}
                                           {:attr "Capricious Accounting Department Id: String" :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}
                                           {:attr "Employees: Ref"                              :iskey false  :deprecated? nil  :figure "Empty" :color "#464B52"}]}]
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

(deftest creating-new-idents

  (testing "Basic entity ident value type string with doc string"
    (rf-test/run-test-sync
      (rf/dispatch [::shared-events/initialize-db])
      (let [schema-map (rf/subscribe [::sut-subs/raw-schema-data-map])
            expected-result {:namespace :db.schema.entity.namespace/store
                             :ident :store/simple-ident
                             :value-type :db.type/string
                             :cardinality :db.cardinality/one
                             :attribute? true
                             :added-in-app? true
                             :doc "Doc string for test purposes"}]
            (rf/dispatch [::sut-events/load-schema schema])
            (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/store])
            (is= [] (->> @schema-map :entities :db.schema.entity.namespace/store :ns-entities (filter #(= :store/simple-ident (:ident %)))))
            (rf/dispatch [::shared-events/reset-and-clear-form-field-values :index :new-entity-ident-form {:cardinality {"value" "one" "label" "One"}
                                                                                                    :deprecated {"value" false "label" "False"}
                                                                                                    :unique {"value" false "label" "False"}
                                                                                                    :is-component {"value" false "label" "False"}
                                                                                                    :no-history {"value" false "label" "False"}}])
            (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-ident-form :ident "simple Ident"])
            (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-ident-form :doc "Doc string for test purposes"])
            (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-ident-form :value-type {"value" "string" "label" "String"}])
            (rf/dispatch [::shared-events/submit-if-form-valid :index :new-entity-ident-form [::sut-events/add-new-entity-ident]])
            (is= expected-result (->> @schema-map :entities :db.schema.entity.namespace/store :ns-entities (filter #(= :store/simple-ident (:ident %))) first)))))

  (testing "Entity ident value type ref updates appropriate ns"
    (rf-test/run-test-sync
      (rf/dispatch [::shared-events/initialize-db])
      (let [schema-map (rf/subscribe [::sut-subs/raw-schema-data-map])
            expected-result {:namespace :db.schema.entity.namespace/store
                             :ident :store/test-ref-ident
                             :value-type :db.type/ref
                             :references-namespaces [:db.schema.ident.namespace/ice-cream-flavor
                                                     :db.schema.ident.namespace/cone-type]
                             :cardinality :db.cardinality/one
                             :added-in-app? true
                             :attribute? true}]
        (rf/dispatch [::sut-events/load-schema schema])
        (rf/dispatch [::sut-events/select-ns :db.schema.entity.namespace/store])
        (is= [] (->> @schema-map :entities :db.schema.entity.namespace/store :ns-entities (filter #(= :store/test-ref-ident (:ident %)))))
        (is= [:db.schema.entity.namespace/sale] (->> @schema-map :idents :db.schema.ident.namespace/ice-cream-flavor :referenced-by))
        (is= [:db.schema.entity.namespace/sale] (->> @schema-map :idents :db.schema.ident.namespace/cone-type :referenced-by))
        (rf/dispatch [::shared-events/reset-and-clear-form-field-values :index :new-entity-ident-form {:cardinality {"value" "one" "label" "One"}
                                                                                                :deprecated {"value" false "label" "False"}
                                                                                                :unique {"value" false "label" "False"}
                                                                                                :is-component {"value" false "label" "False"}
                                                                                                :no-history {"value" false "label" "False"}}])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-ident-form :ident ":test-ref-ident"])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-ident-form :value-type {"value" "ref" "label" "Reference"}])
        (rf/dispatch [::shared-events/set-form-field-value :index :new-entity-ident-form :ref-namespaces [{"value" "db.schema.ident.namespace/ice-cream-flavor"
                                                                                                    "label" ":db.schema.ident.namespace/ice-cream-flavor"}
                                                                                                   {"value" "db.schema.ident.namespace/cone-type"
                                                                                                    "label" ":db.schema.ident.namespace/cone-type"}]])
        (rf/dispatch [::shared-events/submit-if-form-valid :index :new-entity-ident-form [::sut-events/add-new-entity-ident]])
        (is= expected-result (->> @schema-map :entities :db.schema.entity.namespace/store :ns-entities (filter #(= :store/test-ref-ident (:ident %))) first))
        (is= [:db.schema.entity.namespace/sale :db.schema.entity.namespace/store] (->> @schema-map :idents :db.schema.ident.namespace/ice-cream-flavor :referenced-by))
        (is= [:db.schema.entity.namespace/sale :db.schema.entity.namespace/store] (->> @schema-map :idents :db.schema.ident.namespace/cone-type :referenced-by))))))

(deftest schema-map->datomic-txs

  (testing "Idents"
    (let [expected-result [{:db/ident :db.schema.ident.namespace/cone-type
                            :db/doc "Ice cream cone options currently available in store."}
                           {:db/ident :cone-type/sugar}
                           {:db/ident :cone-type/cake}
                           {:db.schema/deprecated? true
                            :db.schema/replaced-by [{:db/ident :cone-type/cake}]
                            :db/ident :cone-type/gravy}
                           {:db/ident :cone-type/waffle}
                           {:db/ident :db.schema.ident.namespace/ice-cream-flavor
                            :db/doc "Ice cream flavor options currently available in store."}
                           {:db/ident :ice-cream-flavor/vanilla}
                           {:db/ident :ice-cream-flavor/strawberry}
                           {:db/ident :ice-cream-flavor/chocolate}]]
      (is= expected-result (sut-events/schema-map->datomic-txs (select-keys schema [:idents])))))

  (testing "Basic Entities"
    (let [data-map {:entities {:db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                                 :doc "An entity representing a single ice cream cone sale"
                                                                 :referenced-by nil
                                                                 :ns-entities [{:references-namespaces [:db.schema.entity.namespace/licensed-retailer :db.schema.entity.namespace/store]
                                                                                :ident :sale/location
                                                                                :attribute? true
                                                                                :cardinality :db.cardinality/one
                                                                                :doc "A reference to a store or licensed retailer entity"
                                                                                :namespace :db.schema.entity.namespace/sale
                                                                                :value-type :db.type/ref}
                                                                               {:unique :db.unique/identity
                                                                                :ident :sale/id
                                                                                :attribute? true
                                                                                :cardinality :db.cardinality/one
                                                                                :doc "Unique id assigned to each sale"
                                                                                :namespace :db.schema.entity.namespace/sale
                                                                                :value-type :db.type/uuid}
                                                                               {:references-namespaces [:db.schema.ident.namespace/ice-cream-flavor]
                                                                                :ident :sale/flavor
                                                                                :attribute? true
                                                                                :cardinality :db.cardinality/one
                                                                                :doc "A reference to ice-cream-flavor ident"
                                                                                :namespace :db.schema.entity.namespace/sale
                                                                                :value-type :db.type/ref}
                                                                               {:references-namespaces [:db.schema.ident.namespace/cone-type]
                                                                                :ident :sale/cone
                                                                                :attribute? true
                                                                                :cardinality :db.cardinality/one
                                                                                :doc "A reference to cone ident"
                                                                                :namespace :db.schema.entity.namespace/sale
                                                                                :value-type :db.type/ref}]}}}
          expected-result [{:db/ident :db.schema.entity.namespace/sale
                            :db/doc "An entity representing a single ice cream cone sale"}
                           {:db/ident :sale/id
                            :db/valueType {:db/ident :db.type/uuid}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db/unique {:db/ident :db.unique/identity}
                            :db/doc "Unique id assigned to each sale"}
                           {:db/ident :sale/flavor
                            :db/valueType {:db/ident :db.type/ref}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db.schema/references-namespaces [{:db/ident :db.schema.ident.namespace/ice-cream-flavor}]
                            :db/doc "A reference to ice-cream-flavor ident"}
                           {:db/ident :sale/cone
                            :db/valueType {:db/ident :db.type/ref}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db.schema/references-namespaces [{:db/ident :db.schema.ident.namespace/cone-type}]
                            :db/doc "A reference to cone ident"}
                           {:db/ident :sale/location
                            :db/valueType {:db/ident :db.type/ref}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db.schema/references-namespaces [{:db/ident :db.schema.entity.namespace/licensed-retailer}
                                                              {:db/ident :db.schema.entity.namespace/store}]
                            :db/doc "A reference to a store or licensed retailer entity"}]
          actual-result (sut-events/schema-map->datomic-txs data-map)]
      (is= (set expected-result) (set actual-result))))

  (testing "Complex Entities: Validated With Tuples"
    (let [data-map {:entities {:db.schema.entity.namespace/employee {:namespace :db.schema.entity.namespace/employee
                                                                     :doc "An entity representing an individual employee"
                                                                     :referenced-by [:db.schema.entity.namespace/store
                                                                                     :db.schema.entity.namespace/licensed-retailer]
                                                                     :attrs [:employee/first-name :employee/last-name]
                                                                     :preds ['i-am-not-real.entity-preds/valid-employee-name?]
                                                                     :ns-entities [{:ident :employee/first-name
                                                                                    :attribute? true
                                                                                    :cardinality :db.cardinality/one
                                                                                    :doc "Employee's first name"
                                                                                    :namespace :db.schema.entity.namespace/employee
                                                                                    :value-type :db.type/string}
                                                                                   {:ident :employee/last-name
                                                                                    :attribute? true
                                                                                    :cardinality :db.cardinality/one
                                                                                    :doc "Employee's last name"
                                                                                    :namespace :db.schema.entity.namespace/employee
                                                                                    :value-type :db.type/string
                                                                                    :attr-preds ['i-am-not-real.attr-preds/user-name? 'i-am-not-real.attr-preds/valid-last-name?]}
                                                                                   {:unique :db.unique/identity
                                                                                    :ident :employee/name
                                                                                    :attribute? true
                                                                                    :cardinality :db.cardinality/one
                                                                                    :doc "Employee's must have a unique combination of first and last names."
                                                                                    :namespace :db.schema.entity.namespace/employee
                                                                                    :value-type :db.type/tuple
                                                                                    :tuple-attrs [:employee/first-name :employee/last-name]}]}}}
          expected-result [{:db/ident :db.schema.entity.namespace/employee
                            :db/doc "An entity representing an individual employee"}
                           {:db/ident :employee/first-name
                            :db/valueType {:db/ident :db.type/string}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db/doc "Employee's first name"}
                           {:db/ident :employee/last-name
                            :db/valueType {:db/ident :db.type/string}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db.attr/preds ['i-am-not-real.attr-preds/user-name? 'i-am-not-real.attr-preds/valid-last-name?]
                            :db/doc "Employee's last name"}
                           {:db/ident :employee/name
                            :db/valueType {:db/ident :db.type/tuple}
                            :db/cardinality {:db/ident :db.cardinality/one}
                            :db/unique {:db/ident :db.unique/identity}
                            :db/tupleAttrs [:employee/first-name :employee/last-name]
                            :db/doc "Employee's must have a unique combination of first and last names."}
                           {:db/ident :employee/validate
                            :db.schema/validates-namespace {:db/ident :db.schema.entity.namespace/employee}
                            :db.entity/attrs [:employee/first-name :employee/last-name]
                            :db.entity/preds ['i-am-not-real.entity-preds/valid-employee-name?]}]
          actual-result (sut-events/schema-map->datomic-txs data-map)]
      (is= (set expected-result) (set actual-result))))

  (testing "Complete Data Map To Txs"
    (let [expected-result txs
          actual-result (sut-events/schema-map->datomic-txs schema)]
      (is= (set expected-result) (set actual-result)))))

(deftest schema-updating-function

  (testing "Remove Selected NameSpaces Single"
    (let [schema {:idents {:db.schema.ident.namespace/cone-type {:namespace :db.schema.ident.namespace/cone-type
                                                                 :doc "Ice cream cone options currently available in store."
                                                                 :referenced-by [:db.schema.entity.namespace/sale]
                                                                 :ns-idents []}
                           :db.schema.ident.namespace/ice-cream-flavor {:namespace :db.schema.ident.namespace/ice-cream-flavor
                                                                        :doc "Ice cream flavor options currently available in store."
                                                                        :referenced-by [:db.schema.entity.namespace/sale]
                                                                        :ns-idents []}}
                  :entities {:db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                               :doc "An entity representing a single ice cream cone sale"
                                                               :referenced-by nil
                                                               :ns-entities []}
                             :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                :doc "An entity representing an individual ice cream store"
                                                                :referenced-by [:db.schema.entity.namespace/sale]
                                                                :ns-entities []}}}
          expected-result {:idents {:db.schema.ident.namespace/cone-type {:namespace :db.schema.ident.namespace/cone-type
                                                                          :doc "Ice cream cone options currently available in store."
                                                                          :referenced-by [:db.schema.entity.namespace/sale]
                                                                          :ns-idents []}}
                           :entities {:db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                                        :doc "An entity representing a single ice cream cone sale"
                                                                        :referenced-by nil
                                                                        :ns-entities []}
                                      :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                         :doc "An entity representing an individual ice cream store"
                                                                         :referenced-by [:db.schema.entity.namespace/sale]
                                                                         :ns-entities []}}}]
      (is= expected-result (sut-events/remove-selected-namespaces schema ["db.schema.ident.namespace/ice-cream-flavor"]))))

  (testing "Remove Selected NameSpaces Multi"
    (let [schema {:idents {:db.schema.ident.namespace/cone-type {:namespace :db.schema.ident.namespace/cone-type
                                                                 :doc "Ice cream cone options currently available in store."
                                                                 :referenced-by [:db.schema.entity.namespace/sale]
                                                                 :ns-idents []}
                           :db.schema.ident.namespace/ice-cream-flavor {:namespace :db.schema.ident.namespace/ice-cream-flavor
                                                                        :doc "Ice cream flavor options currently available in store."
                                                                        :referenced-by [:db.schema.entity.namespace/sale]
                                                                        :ns-idents []}}
                  :entities {:db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                               :doc "An entity representing a single ice cream cone sale"
                                                               :referenced-by nil
                                                               :ns-entities []}
                             :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                :doc "An entity representing an individual ice cream store"
                                                                :referenced-by [:db.schema.entity.namespace/sale]
                                                                :ns-entities []}}}
          expected-result {:idents {:db.schema.ident.namespace/cone-type {:namespace :db.schema.ident.namespace/cone-type
                                                                          :doc "Ice cream cone options currently available in store."
                                                                          :referenced-by [:db.schema.entity.namespace/sale]
                                                                          :ns-idents []}}
                           :entities {:db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                                        :doc "An entity representing a single ice cream cone sale"
                                                                        :referenced-by nil
                                                                        :ns-entities []}}}]
      (is= expected-result (sut-events/remove-selected-namespaces schema ["db.schema.ident.namespace/ice-cream-flavor" "db.schema.entity.namespace/store"]))))

  (testing "Remove References To NameSpace"
    (let [schema {:entities {:db.schema.entity.namespace/licensed-retailer {:namespace :db.schema.entity.namespace/licensed-retailer
                                                                            :ns-entities [{:ident :licensed-retailer/employees
                                                                                           :references-namespaces [:db.schema.entity.namespace/employee]}
                                                                                          {:ident :licensed-retailer/id
                                                                                           :references-namespaces []}
                                                                                          {:ident :licensed-retailer/address
                                                                                           :references-namespaces []}]}
                             :db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                               :ns-entities [{:ident :sale/flavor
                                                                              :references-namespaces [:db.schema.ident.namespace/ice-cream-flavor]}
                                                                             {:ident :sale/cone
                                                                              :references-namespaces [:db.schema.ident.namespace/cone-type]}
                                                                             {:ident :sale/id
                                                                              :references-namespaces []}
                                                                             {:ident :sale/location
                                                                              :references-namespaces [:db.schema.entity.namespace/licensed-retailer :db.schema.entity.namespace/store]}]}
                             :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                :ns-entities [{:ident :store/capricious-accounting-department-id
                                                                               :references-namespaces []}
                                                                              {:ident :store/address
                                                                               :references-namespaces []}
                                                                              {:ident :store/id
                                                                               :references-namespaces []}
                                                                              {:ident :store/employees
                                                                               :references-namespaces [:db.schema.entity.namespace/employee]}]}
                             :db.schema.entity.namespace/employee {:namespace :db.schema.entity.namespace/employee
                                                                   :ns-entities [{:ident :employee/name
                                                                                  :references-namespaces []}
                                                                                 {:ident :employee/last-name
                                                                                  :references-namespaces []}
                                                                                 {:ident :employee/first-name
                                                                                  :references-namespaces []}]}}}
          expected-result {:db.schema.entity.namespace/licensed-retailer {:namespace :db.schema.entity.namespace/licensed-retailer
                                                                          :ns-entities [{:ident :licensed-retailer/employees
                                                                                         :references-namespaces [:db.schema.entity.namespace/employee]}
                                                                                        {:ident :licensed-retailer/id
                                                                                         :references-namespaces []}
                                                                                        {:ident :licensed-retailer/address
                                                                                         :references-namespaces []}]}
                           :db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                             :ns-entities [{:ident :sale/flavor
                                                                            :references-namespaces [:db.schema.ident.namespace/ice-cream-flavor]}
                                                                           {:ident :sale/cone
                                                                            :references-namespaces [:db.schema.ident.namespace/cone-type]}
                                                                           {:ident :sale/id
                                                                            :references-namespaces []}
                                                                           {:ident :sale/location
                                                                            :references-namespaces [:db.schema.entity.namespace/licensed-retailer]}]}
                           :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                              :ns-entities [{:ident :store/capricious-accounting-department-id
                                                                             :references-namespaces []}
                                                                            {:ident :store/address
                                                                             :references-namespaces []}
                                                                            {:ident :store/id
                                                                             :references-namespaces []}
                                                                            {:ident :store/employees
                                                                             :references-namespaces [:db.schema.entity.namespace/employee]}]}
                           :db.schema.entity.namespace/employee {:namespace :db.schema.entity.namespace/employee
                                                                 :ns-entities [{:ident :employee/name
                                                                                :references-namespaces []}
                                                                               {:ident :employee/last-name
                                                                                :references-namespaces []}
                                                                               {:ident :employee/first-name
                                                                                :references-namespaces []}]}}]
      (is= expected-result (sut-events/remove-refs-to-ns schema ["db.schema.entity.namespace/store"]))))

  (testing "Remove selected idents"
    (let [schema {:entities {:db.schema.entity.namespace/licensed-retailer {:namespace :db.schema.entity.namespace/licensed-retailer
                                                                            :ns-entities [{:ident :licensed-retailer/employees
                                                                                           :references-namespaces [:db.schema.entity.namespace/employee]}
                                                                                          {:ident :licensed-retailer/id
                                                                                           :references-namespaces []}
                                                                                          {:ident :licensed-retailer/address
                                                                                           :references-namespaces []}]}
                             :db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                               :ns-entities [{:ident :sale/flavor
                                                                              :references-namespaces [:db.schema.ident.namespace/ice-cream-flavor]}
                                                                             {:ident :sale/cone
                                                                              :references-namespaces [:db.schema.ident.namespace/cone-type]}
                                                                             {:ident :sale/id
                                                                              :references-namespaces []}
                                                                             {:ident :sale/location
                                                                              :references-namespaces [:db.schema.entity.namespace/licensed-retailer :db.schema.entity.namespace/store]}]}
                             :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                :ns-entities [{:ident :store/capricious-accounting-department-id
                                                                               :references-namespaces []}
                                                                              {:ident :store/address
                                                                               :references-namespaces []}
                                                                              {:ident :store/id
                                                                               :references-namespaces []}
                                                                              {:ident :store/employees
                                                                               :references-namespaces [:db.schema.entity.namespace/employee]}]}
                             :db.schema.entity.namespace/employee {:namespace :db.schema.entity.namespace/employee
                                                                   :ns-entities [{:ident :employee/name
                                                                                  :references-namespaces []}
                                                                                 {:ident :employee/last-name
                                                                                  :references-namespaces []}
                                                                                 {:ident :employee/first-name
                                                                                  :references-namespaces []}]}}}
          expected-result {:schema-without-idents {:entities {:db.schema.entity.namespace/licensed-retailer {:namespace :db.schema.entity.namespace/licensed-retailer
                                                                                                             :ns-entities [{:ident :licensed-retailer/employees
                                                                                                                            :references-namespaces [:db.schema.entity.namespace/employee]}
                                                                                                                           {:ident :licensed-retailer/id
                                                                                                                            :references-namespaces []}
                                                                                                                           {:ident :licensed-retailer/address
                                                                                                                            :references-namespaces []}]}
                                                              :db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                                                                :ns-entities [{:ident :sale/id
                                                                                                               :references-namespaces []}
                                                                                                              {:ident :sale/location
                                                                                                               :references-namespaces [:db.schema.entity.namespace/licensed-retailer :db.schema.entity.namespace/store]}]}
                                                              :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                                                 :ns-entities [{:ident :store/capricious-accounting-department-id
                                                                                                                :references-namespaces []}
                                                                                                               {:ident :store/address
                                                                                                                :references-namespaces []}
                                                                                                               {:ident :store/id
                                                                                                                :references-namespaces []}
                                                                                                               {:ident :store/employees
                                                                                                                :references-namespaces [:db.schema.entity.namespace/employee]}]}
                                                              :db.schema.entity.namespace/employee {:namespace :db.schema.entity.namespace/employee
                                                                                                    :ns-entities [{:ident :employee/name
                                                                                                                   :references-namespaces []}
                                                                                                                  {:ident :employee/last-name
                                                                                                                   :references-namespaces []}
                                                                                                                  {:ident :employee/first-name
                                                                                                                   :references-namespaces []}]}}}
                           :remove-refed-by #{:db.schema.ident.namespace/ice-cream-flavor :db.schema.ident.namespace/cone-type}}
          selected-ns-idents [{:ident :sale/flavor
                               :references-namespaces [:db.schema.ident.namespace/ice-cream-flavor]}
                              {:ident :sale/cone
                               :references-namespaces [:db.schema.ident.namespace/cone-type]}
                              {:ident :sale/id
                               :references-namespaces []}
                              {:ident :sale/location
                               :references-namespaces [:db.schema.entity.namespace/licensed-retailer :db.schema.entity.namespace/store]}]]
      (is= expected-result (sut-events/remove-selected-idents schema :entities :ns-entities :db.schema.entity.namespace/sale selected-ns-idents #{:sale/cone :sale/flavor}))))

  (testing "Remove unneeded refed by"
    (let [schema {:idents {:db.schema.ident.namespace/cone-type {:namespace :db.schema.ident.namespace/cone-type
                                                                 :doc "Ice cream cone options currently available in store."
                                                                 :referenced-by [:db.schema.entity.namespace/sale]
                                                                 :ns-idents []}
                           :db.schema.ident.namespace/ice-cream-flavor {:namespace :db.schema.ident.namespace/ice-cream-flavor
                                                                        :doc "Ice cream flavor options currently available in store."
                                                                        :referenced-by [:db.schema.entity.namespace/sale]
                                                                        :ns-idents []}}
                  :entities {:db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                               :doc "An entity representing a single ice cream cone sale"
                                                               :referenced-by nil
                                                               :ns-entities []}
                             :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                :doc "An entity representing an individual ice cream store"
                                                                :referenced-by [:db.schema.entity.namespace/sale]
                                                                :ns-entities []}}}
          expected-result {:idents {:db.schema.ident.namespace/cone-type {:namespace :db.schema.ident.namespace/cone-type
                                                                          :doc "Ice cream cone options currently available in store."
                                                                          :referenced-by []
                                                                          :ns-idents []}
                                    :db.schema.ident.namespace/ice-cream-flavor {:namespace :db.schema.ident.namespace/ice-cream-flavor
                                                                                 :doc "Ice cream flavor options currently available in store."
                                                                                 :referenced-by []
                                                                                 :ns-idents []}}
                           :entities {:db.schema.entity.namespace/sale {:namespace :db.schema.entity.namespace/sale
                                                                        :doc "An entity representing a single ice cream cone sale"
                                                                        :referenced-by nil
                                                                        :ns-entities []}
                                      :db.schema.entity.namespace/store {:namespace :db.schema.entity.namespace/store
                                                                         :doc "An entity representing an individual ice cream store"
                                                                         :referenced-by [:db.schema.entity.namespace/sale]
                                                                         :ns-entities []}}}]
      (is= expected-result (sut-events/remove-unneeded-refed-by schema :db.schema.entity.namespace/sale #{:db.schema.ident.namespace/ice-cream-flavor :db.schema.ident.namespace/cone-type})))))
