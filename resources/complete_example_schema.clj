(ns complete-example-schema
  (:require
    [datomic.client.api :as d]))

(def annotation-schema-tx [{:db/ident :db.schema/deprecated?
                            :db/valueType :db.type/boolean
                            :db/cardinality :db.cardinality/one
                            :db/doc "DOCUMENTATION ONLY. Boolean flag indicating the field has been deprecated."}
                           {:db/ident :db.schema/replaced-by
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/many
                            :db/doc "DOCUMENTATION ONLY. Used to document when a deprecated field is replaced by other."}
                           {:db/ident :db.schema/references-namespaces
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/many
                            :db/doc "DOCUMENTATION ONLY. Used to indicate which specific :db/idents are intended to be referenced by :db.type/ref"}
                           {:db/ident :db.schema/validates-namespace
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/one
                            :db/doc "DOCUMENTATION ONLY. Used to indicate which specific :db/idents are intended to be validated by :db.type/ref"}])

(def ice-cream-shop-schema [;; --- Flavor Idents -------------------------------
                            {:db/id    "ice-cream-flavor"
                             :db/ident :db.schema.ident.namespace/ice-cream-flavor
                             :db/doc   "Ice cream flavor options, currently available in store."}
                            {:db/ident :ice-cream-flavor/strawberry}
                            {:db/ident :ice-cream-flavor/chocolate}
                            {:db/ident :ice-cream-flavor/vanilla}
                            ;; --- Cone Idents ---------------------------------
                            {:db/id    "cone-type"
                             :db/ident :db.schema.ident.namespace/cone-type
                             :db/doc   "Ice cream cone options, currently available in store."}
                            {:db/ident :cone-type/waffle}
                            {:db/ident :cone-type/sugar}
                            {:db/id "cake" :db/ident :cone-type/cake}
                            {:db/ident :cone-type/gravy
                             :db.schema/deprecated? true
                             :db.schema/replaced-by ["cake"]}
                            ;; --- Employee ------------------------------------
                            {:db/id    "employee"
                             :db/ident :db.schema.entity.namespace/employee
                             :db/doc   "An entity representing an individual employee"}
                            {:db/ident       :employee/first-name
                             :db/valueType   :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db/doc         "Employee's first name"}
                            {:db/ident       :employee/last-name
                             :db/valueType   :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db.attr/preds  'i-am-not-real.attr-preds/valid-last-name?
                             :db/doc         "Employee's last name"}
                            {:db/ident       :employee/name
                             :db/valueType   :db.type/tuple
                             :db/tupleAttrs  [:employee/first-name :employee/last-name]
                             :db/cardinality :db.cardinality/one
                             :db/unique      :db.unique/identity
                             :db/doc         "Employee's must have a unique combination of first and last names."}
                            {:db/ident       :employee/validate
                             :db.schema/validates-namespace "employee"
                             :db.entity/attrs [:employee/first-name :employee/last-name]
                             :db.entity/preds 'i-am-not-real.entity-preds/valid-employee-name?}
                            ;; --- Stores --------------------------------------
                            {:db/id    "store"
                             :db/ident :db.schema.entity.namespace/store
                             :db/doc   "An entity representing an individual ice cream store"}
                            {:db/id          "store-id"
                             :db/ident       :store/id
                             :db/valueType   :db.type/uuid
                             :db/cardinality :db.cardinality/one
                             :db/unique      :db.unique/identity
                             :db/doc         "Unique id assigned to each store"}
                            {:db/ident       :store/address
                             :db/valueType   :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db/doc         "Street address of a specific store location"}
                            {:db/ident       :store/employees
                             :db/valueType   :db.type/ref
                             :db/isComponent true
                             :db/cardinality :db.cardinality/many
                             :db/doc         "Employees who may work at a given store"
                             :db.schema/references-namespaces ["employee"]}
                            {:db/ident       :store/capricious-accounting-id
                             :db/valueType   :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db/noHistory    true
                             :db/doc         "An id that is subject to change based on the whims of the accounting department. No history is retained."}
                            ;; --- License Retailer ----------------------------
                            {:db/id    "licensed retailer"
                             :db/ident :db.schema.entity.namespace/licensed-retailer
                             :db/doc   "An business who is licensed to sell our branded ice cream cones"}
                            {:db/id          "licensed-retailer-id"
                             :db/ident       :licensed-retailer/id
                             :db/valueType   :db.type/uuid
                             :db/cardinality :db.cardinality/one
                             :db/unique      :db.unique/identity
                             :db/doc         "Unique id assigned to each licensed retailer"}
                            {:db/ident       :licensed-retailer/address
                             :db/valueType   :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db/doc         "Street address of a specific licensed retailer location"}
                            {:db/ident       :licensed-retailer/employees
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/many
                             :db/doc         "Employees who may work at a given licensed retailer location"
                             :db.schema/references-namespaces ["employee"]}
                            ;; --- Sales ---------------------------------------
                            {:db/ident :db.schema.entity.namespace/sale
                             :db/doc "An entity representing a single ice cream cone sale"}
                            {:db/ident       :sale/id
                             :db/valueType   :db.type/uuid
                             :db/cardinality :db.cardinality/one
                             :db/unique      :db.unique/identity
                             :db/doc         "Unique id assigned to each sale"}
                            {:db/ident       :sale/flavor
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/one
                             :db/doc         "A reference to ice-cream-flavor ident"
                             :db.schema/references-namespaces ["ice-cream-flavor"]}
                            {:db/ident       :sale/cone
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/one
                             :db/doc         "A reference to cone ident"
                             :db.schema/references-namespaces ["cone-type"]}
                            {:db/ident       :sale/location
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/one
                             :db/doc         "A reference to a store or licensed retailer entity"
                             :db.schema/references-namespaces ["store" "licensed retailer"]}])

(comment
  (d/transact conn {:tx-data annotation-schema-tx})
  (d/transact conn {:tx-data ice-cream-shop-schema}))
