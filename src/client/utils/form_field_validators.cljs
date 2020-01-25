(ns client.utils.form-field-validators
  (:require
    [clojure.string :as str]
    [client.utils.helpers :refer [title->kebab >dis]]
    [client.events :refer [input-error?] :as shared-events]
    [clojure.set :as set]))

; region == Shared Validators
(defmethod input-error? :shared/non-empty-string [{:keys [value dirty? required?]}]
  (and dirty?
       required?
       (str/blank? value)))

(defmethod input-error? :shared/non-empty-string [{:keys [value dirty? required?]}]
  (and dirty?
       required?
       (str/blank? value)))

(defmethod input-error? :shared/empty-or-number [{:keys [value dirty? required?]}]
  (and dirty?
       required?
       (and
         (not (str/blank? value))
         (not (re-matches #"\d\.?\d*" value)))))

(defmethod input-error? :shared/non-required-fully-qualified-symbols [{:keys [value dirty?]}]
  (let [syms (str/split value #"\s")]
    (and dirty?
         (not (str/blank? value))
         (not-every? #(re-matches #"^[a-zA-Z0-9\s\:\-_\?\!\<\>]+/[a-zA-Z0-9\s\:\-\_\?\!\<\>]+$" %) syms))))
; endregion

; region == New NS From Validators
(defmethod input-error? :new-ns-form/namespace [{:keys [value dirty? form db]}]
  (let [schema (-> db :routes :index :schema)
        entities (->> schema :entities keys set)
        idents (->> schema :idents keys set)
        {:keys [type]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
        entity? (= "entity" (get type "value"))
        ns-str (if (str/starts-with? value ":")
                 (str/replace-first value ":" "")
                 (title->kebab value))
        ns-kw (keyword (str "db.schema." (if entity? "entity" "ident") ".namespace") ns-str)]
    (and dirty?
       (or (str/blank? value)
           (contains? (set/union entities idents) ns-kw)
           (not (re-matches #"^[a-zA-Z0-9\s\:\-\>\<\_\?\!]+$" value))))))
; endregion

; region == New Ident Form
(defmethod input-error? :shared/ident [{:keys [value dirty? db]}]
  (let [value (if (str/blank? value) "**REGEX BARF ON NIL**" value)
        schema (-> db :routes :index :schema)
        selected-ns (-> db :routes :index :currently-selected-ns)
        entity? (= "db.schema.entity.namespace" (namespace selected-ns))
        type (if entity? :entities :idents)
        ident-key (if entity? :ns-entities :ns-idents)
        idents (->> schema type selected-ns ident-key (map :ident) set)
        ns-str (if (str/starts-with? value ":")
                 (str/replace-first value ":" "")
                 (title->kebab value))
        ns-kw (keyword (name selected-ns) ns-str)]
    (and dirty?
         (or (contains? idents ns-kw)
             (not (re-matches #"^[a-zA-Z0-9\s\:\-\>\<\_\?\!]+$" value))))))

(defmethod input-error? :new-entity-ident-form/value-type [{:keys [value dirty?]}]
  (when (not= {"value" "ref" "label" "Reference"} value)
    (>dis [::shared-events/set-form-field-value :index :new-entity-ident-form :is-component {"value" false "label" "False"}]))
  (when (contains? #{{"value" "composite-tuple" "label" "Composite Tuple"}
                     {"value" "fixed-length-tuple" "label" "Fixed Length Tuple"}
                     {"value" "variable-length-tuple" "label" "Variable Length Tuple"}} value)
    (>dis [::shared-events/set-form-field-value :index :new-entity-ident-form :cardinality {"value" "one" "label" "One"}]))
  (and dirty?
       (str/blank? value)))

(defmethod input-error? :new-entity-ident-form/cardinality [{:keys [value dirty?]}]
  (when (= {"value" "many" "label" "Many"} value)
    (>dis [::shared-events/set-form-field-value :index :new-entity-ident-form :unique {"value" false "label" "False"}]))
  (and dirty?
       (str/blank? value)))
; endregion
