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
        enumerations (->> schema :enumerations keys set)
        {:keys [type]} (reduce (fn [res [k {:keys [value]}]] (assoc res k value)) {} form)
        entity? (= "entity" (get type "value"))
        ns-str (if (str/starts-with? value ":")
                 (str/replace-first value ":" "")
                 (title->kebab value))
        ns-kw (keyword (str "cartographer." (if entity? "entity" "enumeration")) ns-str)]
    (and dirty?
       (or (str/blank? value)
           (contains? (set/union entities enumerations) ns-kw)
           (not (re-matches #"^[a-zA-Z0-9\s\:\-\>\<\_\?\!]+$" value))))))
; endregion

; region == New Attr Form
(defmethod input-error? :shared/attr [{:keys [value dirty? db]}]
  (let [value (if (str/blank? value) "**REGEX BARF ON NIL**" value)
        schema (-> db :routes :index :schema)
        selected-ns (-> db :routes :index :currently-selected-ns)
        entity? (= "cartographer.entity" (namespace selected-ns))
        type (if entity? :entities :enumerations)
        attrs (->> schema type selected-ns :ns-attrs (map :ident) set)
        ns-str (if (str/starts-with? value ":")
                 (str/replace-first value ":" "")
                 (title->kebab value))
        ns-kw (keyword (name selected-ns) ns-str)]
    (and dirty?
         (or (contains? attrs ns-kw)
             (not (re-matches #"^[a-zA-Z0-9\s\:\-\>\<\_\?\!]+$" value))))))

(defmethod input-error? :new-entity-attr-form/value-type [{:keys [value dirty?]}]
  (when (not= {"value" "ref" "label" "Reference"} value)
    (>dis [::shared-events/set-form-field-value :index :new-entity-attr-form :is-component {"value" false "label" "False"}]))
  (when (contains? #{{"value" "tuple" "label" "Composite Tuple"}
                     {"value" "fixed-length-tuple" "label" "Fixed Length Tuple"}
                     {"value" "variable-length-tuple" "label" "Variable Length Tuple"}} value)
    (>dis [::shared-events/set-form-field-value :index :new-entity-attr-form :cardinality {"value" "one" "label" "One"}]))
  (and dirty?
       (str/blank? value)))

(defmethod input-error? :new-entity-attr-form/cardinality [{:keys [value dirty?]}]
  (when (= {"value" "many" "label" "Many"} value)
    (>dis [::shared-events/set-form-field-value :index :new-entity-attr-form :unique {"value" false "label" "False"}]))
  (and dirty?
       (str/blank? value)))
; endregion
