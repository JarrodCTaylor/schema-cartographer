(ns client.routes.index.db
  (:require
    [clojure.spec.alpha :as s]
    [client.specs :as specs]
    [client.utils.helpers :as helper]))

; region = Functions ====================================================================
(defn valid-hex-color? [hex]
  (re-matches #"\#[A-F0-9]{0,6}" hex))
; endregion

; region = Index DB Specs ===============================================================

; region - Settings ---------------------------------------------------------------------
(s/def ::modal-visible? boolean?)
(s/def ::collapse-details? boolean?)
(s/def ::display-as-keywords? boolean?)
(s/def ::color-scheme #{"dark" "light"})
(s/def ::background-color valid-hex-color?)
(s/def ::settings (s/keys :req-un [::modal-visible? ::collapse-details? ::display-as-keywords? ::color-scheme ::background-color]))
; endregion

; region - Schema -----------------------------------------------------------------------
;; -- common
(s/def ::ident keyword?)
(s/def ::attribute? boolean?)
(s/def ::namespace keyword?)
(s/def ::doc (s/nilable string?))
(s/def ::deprecated? boolean?)
(s/def ::referenced-by (s/nilable (s/coll-of keyword?)))
(s/def ::replaced-by (s/coll-of keyword?))
;; -- entities
(s/def ::cardinality #{:db.cardinality/many :db.cardinality/one})
(s/def ::value-type keyword?)
(s/def ::unique #{:db.unique/identity :db.unique/value})
(s/def ::references-namespaces (s/coll-of keyword?))
(s/def ::is-component? boolean?)
(s/def ::no-history? boolean?)
(s/def ::tuple-attrs (s/coll-of keyword?))
;; (s/def ::attr-preds (s/coll-of symbol?))
(s/def ::attrs (s/coll-of keyword?))
;; (s/def ::preds (s/coll-of symbol?))
(s/def ::entity-detail (s/keys :req-un [::ident ::attribute? ::namespace]
                               :opt-un [::doc ::cardinality ::value-type ::unique ::deprecated? ::replaced-by ::references-namespaces ::is-component? ::no-history? ::tuple-attrs]))
(s/def ::ns-attrs (s/coll-of ::entity-detail))
(s/def ::entity-namespace (s/keys :req-un [::namespace ::ns-attrs]
                                  :opt-un [::doc ::referenced-by ::attrs]))
(s/def ::entities (s/every-kv keyword? ::entity-namespace))
;; --- attrs
(s/def ::attr-detail (s/keys :req-un [::ident ::attribute? ::namespace]
                              :opt-un [::deprecated? ::replaced-by]))
(s/def ::ns-attrs (s/coll-of ::attr-detail))
(s/def ::attr-namespace (s/keys :req-un [::namespace ::ns-attrs]
                                 :opt-un [::doc ::referenced-by]))
(s/def ::enumerations (s/every-kv keyword? ::attr-namespace))
(s/def ::schema (s/or
                  :empty empty?
                  :populated (s/keys :opt-un [::entities ::enumerations])))
; endregion

(s/def ::aside-filter string?)
(s/def ::currently-selected-ns (s/nilable keyword?))
(s/def ::previously-selected-ns (s/coll-of keyword?))
(s/def ::active-tab keyword?)
(s/def ::left-panel-active-tab keyword?)
(s/def ::load-schema-tabs (s/keys :req-un [::active-tab]))
(s/def ::load-schema-form ::specs/form)
(s/def ::index-db (s/keys :req-un [::settings ::aside-filter ::currently-selected-ns ::left-panel-active-tab
                                   ::previously-selected-ns ::schema]))
; endregion

; region = Default Index Route DB Map ===================================================
(defonce index-db
         {:settings {:modal-visible? false
                     :collapse-details? true
                     :display-as-keywords? false
                     :color-scheme "dark"
                     :background-color "#464B52"}
          :aside-filter ""
          :left-panel-active-tab :ns
          :currently-selected-ns nil
          :previously-selected-ns []
          :schema {}
          :graph-colors {"dark" {:color-1 "#464B52" ; Header and Margin Background
                                 :color-2 "#5E646E" ; Graph Background
                                 :color-3 "#BBC0C7" ; Text along arrow
                                 :color-4 "#BBC0C7" ; Box background
                                 :color-5 "#BBC0C7" ; Arrow Color
                                 }
                         "light" {:color-1 "#464B52"
                                  :color-2 "#EDF2F7"
                                  :color-3 "#464B52"
                                  :color-4 "#FFFFFF"
                                  :color-5 "#464B52"}}
          :modal-visibility-state {:new-ns false
                                   :delete-ns false
                                   :delete-attr false
                                   :edit-ns false
                                   :new-attr false
                                   :new-entity-attr false}
          :new-ns-form {:type (helper/empty-form-field {:required? true
                                                        :validator-fn :shared/non-empty-string
                                                        :error-message "Select new namespace type"})
                        :namespace (helper/empty-form-field {:error-message "Provide a valid namespace [a-zA-Z0-9:-_?!] that doesn't yet exist"
                                                             :validator-fn :new-ns-form/namespace
                                                             :required? true})
                        :doc (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                       :required? false})}
          :new-entity-attr-form {:attr (helper/empty-form-field {:validator-fn :shared/attr
                                                                  :error-message "Provide a valid attr value [a-zA-Z0-9:-_?!] that doesn't yet exist"
                                                                  :required? true})
                                 :doc (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                :required? false})
                                 :cardinality (helper/empty-form-field {:validator-fn :new-entity-attr-form/cardinality
                                                                        :error-message "A cardinality must be specified"
                                                                        :required? true})
                                 :value-type (helper/empty-form-field {:validator-fn :new-entity-attr-form/value-type
                                                                       :error-message "A value type must be specified"
                                                                       :required? true})
                                 :tuple-attrs (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                        :error-message "Does the attr need to be unique?"
                                                                        :required? false})
                                 :ref-namespaces (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                           :required? false})
                                 :deprecated (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                       :error-message "Mark attr as deprecated?"
                                                                       :required? false})
                                 :replaced-by (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                        :error-message "Attrs that have replaced the deprecation"
                                                                        :required? false})
                                 :unique (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                   :error-message "Does the attr need to be unique?"
                                                                   :required? false})
                                 :is-component (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                         :error-message "Does the attr a component?"
                                                                         :required? false})
                                 :no-history (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                       :error-message "Does the attr not track history?"
                                                                       :required? false})

                                 :attr-preds (helper/empty-form-field {:validator-fn :shared/non-required-fully-qualified-symbols
                                                                       :error-message "Preds much be fully qualified symbols separated by a space"
                                                                       :required? false})}
          :new-attr-form {:attr (helper/empty-form-field {:validator-fn :shared/attr
                                                            :error-message "Provide a valid attr value [a-zA-Z0-9:-_?!] that doesn't yet exist"
                                                            :required? true})
                           :doc (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                          :required? false})
                           :deprecated (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                 :error-message "Mark attr as deprecated?"
                                                                 :required? false})
                           :replaced-by (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                  :error-message "Attrs that have replaced the deprecation"
                                                                  :required? false})}
          :edit-ns-form {:doc (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                        :required? false})
                         :entity-attrs (helper/empty-form-field {:validator-fn :shared/non-empty-string #_:shared/fully-qualified-symbols
                                                                 :error-message "Attrs much be valid keywords separated by a space"
                                                                 :required? false})
                         :entity-preds (helper/empty-form-field {:validator-fn :shared/non-required-fully-qualified-symbols #_:shared/fully-qualified-symbols
                                                                 :error-message "Preds much be fully qualified symbols separated by a space"
                                                                 :required? false})}
          :edit-attr-form {:attr (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                             :error-message "Provide a valid attr value [a-zA-Z0-9:-_?!] that doesn't yet exist"
                                                             :required? true})
                            :doc (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                           :required? false})
                            :ref-namespaces (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                      :required? false})
                            :deprecated (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                  :error-message "Mark attr as deprecated?"
                                                                  :required? false})
                            :replaced-by (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                   :error-message "Attrs that have replaced the deprecation"
                                                                   :required? false})}
          :delete-ns-form {:namespaces (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                 :error-message "Select at least one Namespace to delete"
                                                                 :required? true})}
          :delete-attr-form {:enumerations (helper/empty-form-field {:validator-fn :shared/non-empty-string
                                                                      :error-message "Select at least one Attr to delete"
                                                                      :required? true})}})
; endregion
