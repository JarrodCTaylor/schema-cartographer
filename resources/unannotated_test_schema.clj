(ns unannotated-test-schema)

(defn valid-last-name? [s]
  true)


(defn valid-employee-name? [s]
  true)

(def unannotated-ice-cream-shop-schema [;; --- Cone Enumerations ---------------------------
                                        {:db/ident :cone-type/waffle}
                                        {:db/ident :cone-type/sugar}
                                        ;; --- Flavor Enumerations -------------------------
                                        {:db/ident :ice-cream-flavor/strawberry}
                                        {:db/ident :ice-cream-flavor/chocolate}
                                        {:db/ident :ice-cream-flavor/vanilla}
                                        ;; --- Employee ------------------------------------
                                        {:db/ident       :employee/first-name
                                         :db/valueType   :db.type/string
                                         :db/cardinality :db.cardinality/one}
                                        {:db/ident       :employee/last-name
                                         :db/valueType   :db.type/string
                                         :db/cardinality :db.cardinality/one
                                         :db.attr/preds  'unannotated-test-schema/valid-last-name?}
                                        {:db/ident       :employee/name
                                         :db/valueType   :db.type/tuple
                                         :db/tupleAttrs  [:employee/first-name :employee/last-name]
                                         :db/cardinality :db.cardinality/one
                                         :db/unique      :db.unique/identity}
                                        {:db/ident       :employee/validate
                                         :db.entity/attrs [:employee/first-name :employee/last-name]
                                         :db.entity/preds 'unannotated-test-schema/valid-employee-name?}
                                        ;; --- Stores --------------------------------------
                                        {:db/ident       :store/id
                                         :db/valueType   :db.type/uuid
                                         :db/cardinality :db.cardinality/one
                                         :db/unique      :db.unique/identity}
                                        {:db/ident       :store/address
                                         :db/valueType   :db.type/string
                                         :db/cardinality :db.cardinality/one}
                                        {:db/ident       :store/employees
                                         :db/valueType   :db.type/ref
                                         ;; references-namespaces ["employee"]
                                         :db/isComponent true
                                         :db/cardinality :db.cardinality/many}
                                        {:db/ident       :store/capricious-accounting-id
                                         :db/valueType   :db.type/string
                                         :db/cardinality :db.cardinality/one
                                         :db/noHistory    true}
                                        ;; --- License Retailer ----------------------------
                                        {:db/ident       :licensed-retailer/id
                                         :db/valueType   :db.type/uuid
                                         :db/cardinality :db.cardinality/one
                                         :db/unique      :db.unique/identity}
                                        {:db/ident       :licensed-retailer/address
                                         :db/valueType   :db.type/string
                                         :db/cardinality :db.cardinality/one}
                                        {:db/ident       :licensed-retailer/employees
                                         :db/valueType   :db.type/ref
                                         ; references-namespaces ["employee"]
                                         :db/cardinality :db.cardinality/many}
                                        ;; --- Sales ---------------------------------------
                                        {:db/ident       :sale/id
                                         :db/valueType   :db.type/uuid
                                         :db/cardinality :db.cardinality/one
                                         :db/unique      :db.unique/identity}
                                        {:db/ident       :sale/cone
                                         :db/valueType   :db.type/ref
                                         ; references-namespaces ["cone-type"]
                                         :db/cardinality :db.cardinality/one}
                                        {:db/ident       :sale/location
                                         :db/valueType   :db.type/ref
                                         ; references-namespaces ["store" "licensed retailer"]
                                         :db/cardinality :db.cardinality/one}])

(def unannotated-transactions [{:db/id "employee"
                                :employee/first-name  "Test"
                                :employee/last-name "Person"}
                               {:db/id "store"
                                :store/id #uuid "02EC6029-4A47-49DB-975C-CAAB9C73528B"
                                :store/employees ["employee"]}
                               {:sale/id #uuid "02EC6029-4A47-49DB-975C-C45731B7999A"
                                :sale/cone :cone-type/waffle
                                :sale/location "store"}
                               {:db/id "licensed-retailer"
                                :licensed-retailer/id #uuid "565E871-4A47-71A2-435C-C45731B7999A"
                                :licensed-retailer/address "123 Any Street"
                                :licensed-retailer/employees ["employee"]}
                               {:sale/id #uuid "02EC6029-4A47-999B-975C-C45731B7999A"
                                :sale/cone :cone-type/waffle
                                :sale/location "store"}
                               {:sale/id #uuid "876A87C2-4A47-999B-975C-C45731B7999A"
                                :sale/cone :cone-type/sugar
                                :sale/location "licensed-retailer"}])