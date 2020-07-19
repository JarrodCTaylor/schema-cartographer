(ns complete-example-schema
  (:require
    [datomic.client.api :as d]))

(def annotation-schema-tx [{:db/ident :cartographer/entity
                            :db/valueType :db.type/keyword
                            :db/unique :db.unique/identity
                            :db/cardinality :db.cardinality/one
                            :db/doc "Creating an entity with this attr will cause its value to be considered an entity-grouping namespace in the application."}
                           {:db/ident :cartographer/enumeration
                            :db/valueType :db.type/keyword
                            :db/unique :db.unique/identity
                            :db/cardinality :db.cardinality/one
                            :db/doc "Creating an entity with this attr will cause its value to be considered an enumeration-grouping namespace in the application."}
                           {:db/ident :cartographer/deprecated?
                            :db/valueType :db.type/boolean
                            :db/cardinality :db.cardinality/one
                            :db/doc "Boolean flag indicating the field has been deprecated."}
                           {:db/ident :cartographer/replaced-by
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/many
                            :db/doc "Used to document when a deprecated field is replaced by other."}
                           {:db/ident :cartographer/references-namespaces
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/many
                            :db/doc "Used to indicate which specific :cartographer/entity or :cartographer/enumeration are intended to be referenced by :db.type/ref"}
                           {:db/ident :cartographer/validates-namespace
                            :db/valueType :db.type/ref
                            :db/cardinality :db.cardinality/one
                            :db/doc "Used to indicate which specific :cartographer/entity is intended to be validated by :db.type/ref"}])

(def ice-cream-shop-schema [;; --- Cone Enumerations ---------------------------
                            {:db/id "cone-type"
                             :cartographer/enumeration :cone-type
                             :db/doc  "Ice cream cone options, currently available in store."}
                            {:db/ident :cone-type/waffle}
                            {:db/ident :cone-type/sugar}
                            {:db/id "cake" :db/ident :cone-type/cake}
                            {:db/ident :cone-type/gravy
                             :cartographer/deprecated? true
                             :cartographer/replaced-by ["cake"]}
                            ;; --- Flavor Enumerations -------------------------
                            {:db/id    "ice-cream-flavor"
                             :cartographer/enumeration :ice-cream-flavor
                             :db/doc   "Ice cream flavor options, currently available in store."}
                            {:db/ident :ice-cream-flavor/strawberry}
                            {:db/ident :ice-cream-flavor/chocolate}
                            {:db/ident :ice-cream-flavor/vanilla}
                            ;; --- Employee ------------------------------------
                            {:db/id    "employee"
                             :cartographer/entity :employee
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
                             :cartographer/validates-namespace "employee"
                             :db.entity/attrs [:employee/first-name :employee/last-name]
                             :db.entity/preds 'i-am-not-real.entity-preds/valid-employee-name?}
                            ;; --- Stores --------------------------------------
                            {:db/id          "store"
                             :cartographer/entity :store
                             :db/doc         "An entity representing an individual ice cream store"}
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
                             :cartographer/references-namespaces ["employee"]}
                            {:db/ident       :store/location
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/one
                             :cartographer/references-namespaces ["licensed retailer" "store"]
                             :db/doc          "A reference to a store or licensed retailer entity"}
                            {:db/ident       :store/employees
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/many
                             :db/isComponent true
                             :cartographer/references-namespaces ["employee"]
                             :db/doc         "Employees who may work at a given store"}
                            {:db/ident       :store/capricious-accounting-id
                             :db/valueType   :db.type/string
                             :db/cardinality :db.cardinality/one
                             :db/noHistory    true
                             :db/doc         "An id that is subject to change based on the whims of the accounting department. No history is retained."}
                            ;; --- License Retailer ----------------------------
                            {:db/id    "licensed retailer"
                             :cartographer/entity :licensed-retailer
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
                             :cartographer/references-namespaces ["employee"]}
                            ;; --- Sales ---------------------------------------
                            {:cartographer/entity :sale
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
                             :cartographer/references-namespaces ["ice-cream-flavor"]}
                            {:db/ident       :sale/cone
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/one
                             :db/doc         "A reference to cone ident"
                             :cartographer/references-namespaces ["cone-type"]}
                            {:db/ident       :sale/location
                             :db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/one
                             :db/doc         "A reference to a store or licensed retailer entity"
                             :cartographer/references-namespaces ["store" "licensed retailer"]}])

(comment
  (d/transact conn {:tx-data annotation-schema-tx})
  (d/transact conn {:tx-data ice-cream-shop-schema}))
