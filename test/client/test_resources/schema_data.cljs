(ns client.test-resources.schema-data)

(def schema {:enumerations {:cartographer.enumeration/cone-type {:namespace :cartographer.enumeration/cone-type
                                                                  :doc "Ice cream cone options currently available in store."
                                                                  :referenced-by [:cartographer.entity/sale]
                                                                  :ns-attrs [{:ident :cone-type/sugar
                                                                              :attribute? false
                                                                              :namespace :cartographer.enumeration/cone-type}
                                                                             {:ident :cone-type/cake
                                                                              :attribute? false
                                                                              :namespace :cartographer.enumeration/cone-type}
                                                                             {:ident :cone-type/gravy
                                                                              :attribute? false
                                                                              :namespace :cartographer.enumeration/cone-type
                                                                              :deprecated? true
                                                                              :replaced-by [:cone-type/cake]}
                                                                             {:ident :cone-type/waffle
                                                                              :attribute? false
                                                                              :namespace :cartographer.enumeration/cone-type}]}
                            :cartographer.enumeration/ice-cream-flavor {:namespace :cartographer.enumeration/ice-cream-flavor
                                                                         :doc "Ice cream flavor options currently available in store."
                                                                         :referenced-by [:cartographer.entity/sale]
                                                                         :ns-attrs [{:ident :ice-cream-flavor/vanilla
                                                                                     :attribute? false
                                                                                     :namespace :cartographer.enumeration/ice-cream-flavor}
                                                                                    {:ident :ice-cream-flavor/strawberry
                                                                                     :attribute? false
                                                                                     :namespace :cartographer.enumeration/ice-cream-flavor}
                                                                                    {:ident :ice-cream-flavor/chocolate
                                                                                     :attribute? false
                                                                                     :namespace :cartographer.enumeration/ice-cream-flavor}]}}
             :entities {:cartographer.entity/licensed-retailer {:namespace :cartographer.entity/licensed-retailer
                                                                       :doc "An business who is licensed to sell our branded ice cream cones"
                                                                       :referenced-by [:cartographer.entity/sale]
                                                                       :ns-attrs [{:ident :licensed-retailer/employees
                                                                                   :attribute? true
                                                                                   :doc "Employees who may work at a given licensed retailer location"
                                                                                   :cardinality :db.cardinality/many
                                                                                   :value-type :db.type/ref
                                                                                   :namespace :cartographer.entity/licensed-retailer
                                                                                   :references-namespaces [:cartographer.entity/employee]}
                                                                                  {:ident :licensed-retailer/id
                                                                                   :attribute? true
                                                                                   :doc "Unique id assigned to each licensed retailer"
                                                                                   :cardinality :db.cardinality/one
                                                                                   :value-type :db.type/uuid
                                                                                   :namespace :cartographer.entity/licensed-retailer
                                                                                   :unique :db.unique/identity}
                                                                                  {:ident :licensed-retailer/address
                                                                                   :attribute? true
                                                                                   :doc "Street address of a specific licensed retailer location"
                                                                                   :cardinality :db.cardinality/one
                                                                                   :value-type :db.type/string
                                                                                   :namespace :cartographer.entity/licensed-retailer}]}
                        :cartographer.entity/sale {:namespace :cartographer.entity/sale
                                                          :doc "An entity representing a single ice cream cone sale"
                                                          :referenced-by nil
                                                          :ns-attrs [{:ident :sale/flavor
                                                                      :attribute? true
                                                                      :doc "A reference to ice-cream-flavor ident"
                                                                      :cardinality :db.cardinality/one
                                                                      :value-type :db.type/ref
                                                                      :namespace :cartographer.entity/sale
                                                                      :references-namespaces [:cartographer.enumeration/ice-cream-flavor]}
                                                                     {:ident :sale/cone
                                                                      :attribute? true
                                                                      :doc "A reference to cone ident"
                                                                      :cardinality :db.cardinality/one
                                                                      :value-type :db.type/ref
                                                                      :namespace :cartographer.entity/sale
                                                                      :references-namespaces [:cartographer.enumeration/cone-type]}
                                                                     {:ident :sale/id
                                                                      :attribute? true
                                                                      :doc "Unique id assigned to each sale"
                                                                      :cardinality :db.cardinality/one
                                                                      :value-type :db.type/uuid
                                                                      :namespace :cartographer.entity/sale
                                                                      :unique :db.unique/identity}
                                                                     {:ident :sale/location
                                                                      :attribute? true
                                                                      :doc "A reference to a store or licensed retailer entity"
                                                                      :cardinality :db.cardinality/one
                                                                      :value-type :db.type/ref
                                                                      :namespace :cartographer.entity/sale
                                                                      :references-namespaces [:cartographer.entity/licensed-retailer :cartographer.entity/store]}]}
                        :cartographer.entity/store {:namespace :cartographer.entity/store
                                                           :doc "An entity representing an individual ice cream store"
                                                           :referenced-by [:cartographer.entity/sale]
                                                           :ns-attrs [{:ident :store/capricious-accounting-department-id
                                                                       :attribute? true
                                                                       :doc "An id that is subject to change based on the whims of the accounting department. No history is retained."
                                                                       :cardinality :db.cardinality/one
                                                                       :value-type :db.type/string
                                                                       :namespace :cartographer.entity/store
                                                                       :no-history? true}
                                                                      {:ident :store/address
                                                                       :attribute? true
                                                                       :doc "Street address of a specific store location"
                                                                       :cardinality :db.cardinality/one
                                                                       :value-type :db.type/string
                                                                       :namespace :cartographer.entity/store}
                                                                      {:ident :store/id
                                                                       :attribute? true
                                                                       :doc "Unique id assigned to each store"
                                                                       :cardinality :db.cardinality/one
                                                                       :value-type :db.type/uuid
                                                                       :namespace :cartographer.entity/store
                                                                       :unique :db.unique/identity}
                                                                      {:ident :store/employees
                                                                       :attribute? true
                                                                       :doc "Employees who may work at a given store"
                                                                       :cardinality :db.cardinality/many
                                                                       :value-type :db.type/ref
                                                                       :namespace :cartographer.entity/store
                                                                       :references-namespaces [:cartographer.entity/employee]
                                                                       :is-component? true}]}
                        :cartographer.entity/employee {:namespace :cartographer.entity/employee
                                                              :doc "An entity representing an individual employee"
                                                              :referenced-by [:cartographer.entity/store :cartographer.entity/licensed-retailer]
                                                              :ns-attrs [{:ident :employee/name
                                                                          :attribute? true
                                                                          :doc "Employee's must have a unique combination of first and last names."
                                                                          :cardinality :db.cardinality/one
                                                                          :value-type :db.type/tuple
                                                                          :namespace :cartographer.entity/employee
                                                                          :unique :db.unique/identity
                                                                          :tuple-attrs [:employee/first-name :employee/last-name]}
                                                                         {:ident :employee/last-name
                                                                          :attribute? true
                                                                          :doc "Employee's last name"
                                                                          :cardinality :db.cardinality/one
                                                                          :value-type :db.type/string
                                                                          :namespace :cartographer.entity/employee
                                                                          :attr-preds ['i-am-not-real.attr-preds/valid-last-name?]
                                                                          }
                                                                         {:ident :employee/first-name
                                                                          :attribute? true
                                                                          :doc "Employee's first name"
                                                                          :cardinality :db.cardinality/one
                                                                          :value-type :db.type/string
                                                                          :namespace :cartographer.entity/employee}]
                                                              :attrs [:employee/first-name :employee/last-name]
                                                              :preds ['i-am-not-real.entity-preds/valid-employee-name?]}}})