(ns cljs.routes.index.db
  (:require
    [clojure.spec.alpha :as s]
    [cljs.specs :as specs]
    [cljs.utils.helpers :as helper]))

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

; region - Analytics --------------------------------------------------------------------
(s/def ::loading? boolean?)
(s/def ::visible? boolean?)
(s/def ::analytics (s/keys :req-un [::loading? ::visible?]))
; endregion

; region - Schema -----------------------------------------------------------------------
;; -- common
(s/def ::ident keyword?)
(s/def ::attribute? boolean?)
(s/def ::namespace keyword?)
(s/def ::doc (s/nilable string?)) ;; If present and nil could be an issue to fix
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
(s/def ::ns-entities (s/coll-of ::entity-detail))
(s/def ::entity-namespace (s/keys :req-un [::namespace ::ns-entities]
                                  :opt-un [::doc ::referenced-by ::attrs]))
(s/def ::entities (s/every-kv keyword? ::entity-namespace))
;; --- idents
(s/def ::ident-detail (s/keys :req-un [::ident ::attribute? ::namespace]
                              :opt-un [::deprecated? ::replaced-by]))
(s/def ::ns-idents (s/coll-of ::ident-detail))
(s/def ::ident-namespace (s/keys :req-un [::namespace ::ns-idents]
                                 :opt-un [::doc ::referenced-by]))
(s/def ::idents (s/every-kv keyword? ::ident-namespace))
(s/def ::schema (s/or
                  :empty empty?
                  :populated (s/keys :opt-un [::entities ::idents])))
; endregion

(s/def ::aside-filter string?)
(s/def ::currently-selected-ns (s/nilable keyword?))
(s/def ::previously-selected-ns (s/coll-of keyword?))
(s/def ::active-tab keyword?)
(s/def ::left-panel-active-tab keyword?)
(s/def ::load-schema-tabs (s/keys :req-un [::active-tab]))
(s/def ::load-schema-form ::specs/form)
(s/def ::index-db (s/keys :req-un [::settings ::analytics ::aside-filter ::currently-selected-ns ::left-panel-active-tab
                                   ::previously-selected-ns ::load-schema-tabs ::load-schema-form ::schema]))
; endregion

; region = Default Index Route DB Map ===================================================
(defonce index-db {:settings {:modal-visible? false
                              :collapse-details? true
                              :display-as-keywords? false
                              :color-scheme "dark"
                              :background-color "#464B52"}
                   :analytics {:loading? false
                               :visible? false}
                   :aside-filter ""
                   :left-panel-active-tab :ns
                   :currently-selected-ns nil
                   :previously-selected-ns []
                   :schema {}
                   :graph-colors {"dark" {:color-1  "#464B52" ; Header and Margin Background
                                          :color-2  "#5E646E" ; Graph Background
                                          :color-3  "#BBC0C7" ; Text along arrow
                                          :color-4  "#BBC0C7" ; Box background
                                          :color-5  "#BBC0C7" ; Arrow Color
                                          }
                                  "light" {:color-1  "#464B52"
                                           :color-2  "#EDF2F7"
                                           :color-3  "#464B52"
                                           :color-4  "#FFFFFF"
                                           :color-5  "#464B52"}}
                   :load-schema-tabs {:active-tab :local-server}
                   :load-schema-form {:system (helper/empty-form-field {:error-message "You must provide a system"
                                                                        :required? true})
                                      :region (helper/empty-form-field {:error-message "You must provide a region"
                                                                        :required? true})
                                      :database-name (helper/empty-form-field {:error-message "You must provide a database name"
                                                                               :required? true})}})
; endregion
