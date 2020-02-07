(ns client.test-resources.datomic-txs)

(def txs [;; ---- Enumerations
          {:db/id ":cartographer.enumeration/cone-type"
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
           :db/ident :ice-cream-flavor/chocolate}
          ;; -- Entities
          ;;; -- Licensed Retailer
          {:db/id ":cartographer.entity/licensed-retailer"
           :cartographer/entity :licensed-retailer
           :db/doc "An business who is licensed to sell our branded ice cream cones"}
          {:db/id ":licensed-retailer/id"
           :db/ident :licensed-retailer/id
           :db/valueType {:db/ident :db.type/uuid}
           :db/cardinality {:db/ident :db.cardinality/one}
           :db/unique {:db/ident :db.unique/identity}
           :db/doc "Unique id assigned to each licensed retailer"}
          {:db/id ":licensed-retailer/address"
           :db/ident :licensed-retailer/address
           :db/valueType {:db/ident :db.type/string}
           :db/cardinality {:db/ident :db.cardinality/one}
           :db/doc "Street address of a specific licensed retailer location"}
          {:db/id ":licensed-retailer/employees"
           :db/ident :licensed-retailer/employees
           :db/valueType {:db/ident :db.type/ref}
           :db/cardinality {:db/ident :db.cardinality/many}
           :cartographer/references-namespaces [":cartographer.entity/employee"]
           :db/doc "Employees who may work at a given licensed retailer location"}
          ;;; -- Store
          {:db/id ":cartographer.entity/store"
           :cartographer/entity :store
           :db/doc "An entity representing an individual ice cream store"}
          {:db/id ":store/id"
           :db/ident :store/id
           :db/valueType {:db/ident :db.type/uuid}
           :db/cardinality {:db/ident :db.cardinality/one}
           :db/unique {:db/ident :db.unique/identity}
           :db/doc "Unique id assigned to each store"}
          {:db/id ":store/address"
           :db/ident :store/address
           :db/valueType {:db/ident :db.type/string}
           :db/cardinality {:db/ident :db.cardinality/one}
           :db/doc "Street address of a specific store location"}
          {:db/id ":store/employees"
           :db/ident :store/employees
           :db/valueType {:db/ident :db.type/ref}
           :db/isComponent true
           :db/cardinality {:db/ident :db.cardinality/many}
           :cartographer/references-namespaces [":cartographer.entity/employee"]
           :db/doc "Employees who may work at a given store"}
          {:db/id ":store/capricious-accounting-department-id"
           :db/ident :store/capricious-accounting-department-id
           :db/valueType {:db/ident :db.type/string}
           :db/cardinality {:db/ident :db.cardinality/one}
           :db/noHistory true
           :db/doc "An id that is subject to change based on the whims of the accounting department. No history is retained."}
          ;;; -- Employee
          {:db/id ":cartographer.entity/employee"
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
           :db.entity/preds ['i-am-not-real.entity-preds/valid-employee-name?]}
          ;;; -- Sale
          {:db/id ":cartographer.entity/sale"
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
           :cartographer/references-namespaces [":cartographer.entity/licensed-retailer"
                                                ":cartographer.entity/store"]
           :db/doc "A reference to a store or licensed retailer entity"}])
