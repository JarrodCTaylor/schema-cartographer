(ns client.routes.index.view
  (:require
    ["react-transition-group" :refer (CSSTransition TransitionGroup)]
    [reagent.core :as r]
    [clojure.edn :as edn]
    [client.components.gojs :as gojs]
    [client.utils.helpers :refer [<sub >dis tip]]
    [client.routes.index.subs :as route-subs]
    [client.subs :as shared-subs]
    [client.events :as shared-events]
    [client.components.validated-input :as validated-input]
    [client.routes.index.events :as route-events]))

(defn nav []
  (let [{:keys [color-scheme]} (<sub [::route-subs/settings])]
    [:nav
     [:a.logo {:href "/"}
      [:img {:src (if (= "dark" color-scheme)
                    "img/dark-nav-logo.svg"
                    "img/light-nav-logo.svg") :height "56"}]]
     [:ul.nav-links
      [:li.nav-link [:a {:on-click #(>dis [::route-events/settings :modal-visible? true])} "Options"]]]]))

(defn breadcrumbs []
  (let [{:keys [by-way-of]} (<sub [::route-subs/aside-selection-summary-info])]
    [:div#by-way-of-breadcrumbs
     [:ul
      (for [{:keys [ns label]} by-way-of]
        [:li {:key ns} [:a {:on-click #(>dis [::route-events/select-breadcrumb-ns ns])} label]])]]))

(defn graph-actions
  []
  (let [^js cmd-handler (<sub [::route-subs/graph-command-handler])
        selected-ns (<sub [::route-subs/selected-ns])
        {:keys [color-scheme]} (<sub [::route-subs/settings])]
    (when selected-ns
      [:div#graph-actions
       [:img {:src (if (= "dark" color-scheme) "img/zoom-in-dark.svg" "img/zoom-in-light.svg")
              :on-click #(.increaseZoom cmd-handler)}]
       [:img {:src (if (= "dark" color-scheme) "img/zoom-out-dark.svg" "img/zoom-out-light.svg")
              :on-click #(.decreaseZoom cmd-handler)}]
       [:img {:src (if (= "dark" color-scheme) "img/size-to-fit-dark.svg" "img/size-to-fit-light.svg")
              :on-click #(.zoomToFit cmd-handler)}]
       [:img {:src (if (= "dark" color-scheme) "img/screenshot-dark.svg" "img/screenshot-light.svg")
              :on-click #(>dis [::route-events/save-graph-to-file])}]])))

(defn secondary-nav []
  [:div.secondary-nav
   [breadcrumbs]
   [graph-actions]])

; region === Name Space Tab
(defn create-new-ns-modal []
  (let [modal-visible? (<sub [::route-subs/modal-visible? :new-ns])
        {:keys [color-scheme]} (when modal-visible? (<sub [::route-subs/settings]))]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Add NameSpace"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/set-modal-visibility :new-ns false])}]]
      [:div.modal-body
       [:form#new-ns-form
        [validated-input/select-box {:color-scheme color-scheme :label "Type" :route :index :form :new-ns-form :field :type :options [{:value "entity" :label "Entity"} {:value "ident" :label "Ident"}]}]
        [validated-input/text {:label "Namespace"
                               :route :index
                               :form :new-ns-form
                               :field :namespace
                               :color-scheme color-scheme
                               :tool-tip "Values beginning with a ':' are assumed to be properly formatted keywords and will not be changed. Everything else will be turned into a kebab-cased keyword."}]
        [validated-input/text {:label "Doc String" :route :index :form :new-ns-form :field :doc}]
        [:button.button {:type "submit"
                         :on-click (fn [evt]
                                     (.preventDefault evt)
                                     (>dis [::shared-events/submit-if-form-valid
                                            :index
                                            :new-ns-form
                                            [::route-events/add-new-ns]]))}
         "Add NameSpace"]]]]]))

(defn delete-ns-modal []
  (let [modal-visible? (<sub [::route-subs/modal-visible? :delete-ns])
        {:keys [color-scheme]} (when modal-visible? (<sub [::route-subs/settings]))
        ns-options (when modal-visible? (<sub [::route-subs/added-ns]))
        existing-ref-notices (when modal-visible? (<sub [::route-subs/existing-ref-notices]))]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Delete NameSpace"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/set-modal-visibility :delete-ns false])}]]
      [:div.modal-body
       [:form#delete-ns-form
        [validated-input/select-box {:color-scheme color-scheme
                                     :label "Namespace(s)"
                                     :route :index
                                     :form :delete-ns-form
                                     :is-multi true
                                     :field :namespaces
                                     :on-change #(do
                                                   (>dis [::shared-events/set-form-field-value :index :delete-ns-form :namespaces (js->clj %)])
                                                   (>dis [::route-events/update-existing-ref-notices (js->clj %)]))
                                     :options ns-options}]
        (when (not-empty existing-ref-notices)
          [:div#delete-notices
           (for [{:keys [ns refed-by]} existing-ref-notices]
            [:div {:key ns}
             [:p (str ns " is referenced by:")]
             [:ul
              (for [n refed-by]
                ^{:key n} [:li n])]])])
        [:button.button {:type "submit"
                         :on-click (fn [evt]
                                     (.preventDefault evt)
                                     (>dis [::shared-events/submit-if-form-valid
                                            :index
                                            :delete-ns-form
                                            [::route-events/delete-ns]]))}
         "Delete NameSpace"]]]]]))

(defn namespace-option [section-label namespaces]
  (let [selected-ns (<sub [::route-subs/selected-ns])]
    [:<>
     [:p.menu-label section-label]
     [:> TransitionGroup {:component "ul" :class "menu-list"}
      (for [{:keys [label ns]} namespaces]
        ^{:key label}
        [:> CSSTransition {:classNames "shrink-grow" :timeout 350}
         [:li {:class (when (= selected-ns ns) "is-active")
               :on-click #(>dis [::route-events/select-ns ns])}
          [:span.label label]]])]]))

(defn namespace-tab []
  (let [aside-filter (<sub [::route-subs/aside-filter])
        {:keys [color-scheme]} (<sub [::route-subs/settings])
        {:keys [entities idents]} (<sub [::route-subs/nav-schema])]
    [:div#namespace-tab
     [:div#ns-filter-input
      [:i.icon [:img {:src (if (= "dark" color-scheme) "img/filter-dark.svg" "img/filter-light.svg")}]]
      [:input.filter-input {:type "text"
                            :value aside-filter
                            :on-change #(>dis [::route-events/update-aside-filter (-> % .-target .-value)])}]]
     [:div.namespaces
      [namespace-option "ENTITIES" entities]
      [namespace-option "IDENTS" idents]]]))
; endregion

; region === Name Space Details Tab
(defn namespace-details-tab []
  (let [{:keys [display-as-keywords?]} (<sub [::route-subs/settings])
        {:keys [doc label referenced-by attrs attrs-kw preds]} (<sub [::route-subs/aside-selection-summary-info])]
    [:div#namespace-details-tab
     [:> TransitionGroup
      [:> CSSTransition
       {:key label :classNames "panel-fade" :timeout 250}
       [:div#namespace-summary
        [:div [:span#namespace-label label]]
        (when doc
          [:div.inset
           [:h4 "Description"]
           [:p.inset doc]])
        (when (not-empty referenced-by)
          [:div.inset
           [:h4 "Referenced By"]
           [:ul
            (for [{:keys [ns label]} referenced-by]
              [:li.link {:key ns
                         :on-click #(>dis [::route-events/select-ns ns])} label])]])
        (when (not-empty attrs)
          [:div.inset
           [:h4 "Required Attributes"]
           [:ul
            (for [attr (if display-as-keywords? attrs-kw attrs)]
              [:li {:key attr} attr])]])
        (when (not-empty preds)
          [:div.inset
           [:h4 "Entity Predicates"]
           [:ul
            (for [attr preds]
              [:li {:key attr} attr])]])]]]]))

(defn edit-ns-modal []
  (let [modal-visible? (<sub [::route-subs/modal-visible? :edit-ns])
        {:keys [color-scheme]} (when modal-visible? (<sub [::route-subs/settings]))
        ns-ident-options (when modal-visible? (<sub [::route-subs/ns-attr-options]))
        entity? (when modal-visible? (<sub [::route-subs/entity-selected?]))
        {:keys [attrs-kw preds]} (when modal-visible? (<sub [::route-subs/aside-selection-summary-info]))]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Edit Namespace"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/set-modal-visibility :edit-ns false])}]]
      [:div.modal-body
       [:form#edit-ns-form
        [validated-input/text {:label "Doc String"
                               :color-scheme color-scheme
                               :route :index
                               :form :edit-ns-form
                               :tool-tip "The optional doc specifies a documentation string, and can be any string value."
                               :field :doc}]
        (when entity?
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "Entity Attributes"
                                       :route :index
                                       :form :edit-ns-form
                                       :field :entity-attrs
                                       :is-multi true
                                       :tool-tip "The :db.entity/attrs attribute is a multi-valued attribute of keywords, where each keyword names a required attribute."
                                       :options ns-ident-options
                                       :additional-dom (when attrs-kw
                                                         [:ul
                                                          (for [attr attrs-kw]
                                                            ^{:key attr} [:li attr])])}]
          [validated-input/text {:label "Entity Predicates"
                                 :route :index
                                 :form :edit-ns-form
                                 :field :entity-preds
                                 :color-scheme color-scheme
                                 :additional-dom (when preds
                                                   [:ul
                                                    (for [pred preds]
                                                      ^{:key pred} [:li pred])])
                                 :tool-tip "The :db.entity/preds attribute is a multi-valued attribute of symbols, where each symbol names a predicate of database value and entity id. Inside a transaction, Datomic will call all predicates, and abort the transaction if any predicate returns a value that is not true."}])
        [:button.button {:type "submit"
                         :on-click (fn [evt]
                                     (.preventDefault evt)
                                     (>dis [::shared-events/submit-if-form-valid
                                            :index
                                            :edit-ns-form
                                            [::route-events/edit-ns]]))}
         "Edit Namespace"]]]]]))

; endregion

; region === Idents Tab
(defn create-new-ident-modal []
  (let [modal-visible? (<sub [::route-subs/modal-visible? :new-ident])
        {:keys [color-scheme]} (when modal-visible? (<sub [::route-subs/settings]))
        deprecated (when modal-visible? (<sub [::shared-subs/form-field :index :new-ident-form :deprecated]))
        ns-options (when modal-visible? (<sub [::route-subs/ns-options]))]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Add Ident"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/set-modal-visibility :new-ident false])}]]
      [:div.modal-body
       [:form#new-ident-form
        [validated-input/text {:label "Ident"
                               :route :index
                               :form :new-ident-form
                               :field :ident
                               :color-scheme color-scheme
                               :tool-tip "Text will be turned into an appropriately formatted and namespaced keyword. Must not already exist in the currently selected namespace. Examples: \"first-name\" or \"Song Title\""}]
        [validated-input/text {:label "Doc String"
                               :color-scheme color-scheme
                               :route :index
                               :form :new-ident-form
                               :tool-tip "The optional doc specifies a documentation string, and can be any string value."
                               :field :doc}]
        [validated-input/select-box {:color-scheme color-scheme
                                     :label "Deprecated"
                                     :route :index
                                     :form :new-ident-form
                                     :field :deprecated
                                     :tool-tip "Documentation Only. Instead of removing schema this boolean flag indicates the field has been deprecated."
                                     :options [{:value false :label "False"} {:value true :label "True"}]}]
        (when (= {"value" true "label" "True"} (:value deprecated))
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "Replaced By"
                                       :route :index
                                       :form :new-ident-form
                                       :field :replaced-by
                                       :is-multi true
                                       :tool-tip "Documentation Only. Used to document what fields is intended to be the replacement for one that was deprecated."
                                       :options ns-options}])
        [:button.button {:type "submit"
                         :on-click (fn [evt]
                                     (.preventDefault evt)
                                     (>dis [::shared-events/submit-if-form-valid
                                            :index
                                            :new-ident-form
                                            [::route-events/add-new-ident]]))}
         "Add Ident"]]]]]))

(defn create-new-entity-ident-modal []
  (let [modal-visible? (<sub [::route-subs/modal-visible? :new-entity-ident])
        {:keys [color-scheme]} (when modal-visible? (<sub [::route-subs/settings]))
        value-type (when modal-visible? (<sub [::shared-subs/form-field :index :new-entity-ident-form :value-type]))
        cardinality (when modal-visible? (<sub [::shared-subs/form-field :index :new-entity-ident-form :cardinality]))
        deprecated (when modal-visible? (<sub [::shared-subs/form-field :index :new-entity-ident-form :deprecated]))
        ns-ident-options (when modal-visible? (<sub [::route-subs/ns-ident-options]))
        ns-options (when modal-visible? (<sub [::route-subs/ns-options]))]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Add Ident"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/set-modal-visibility :new-entity-ident false])}]]
      [:div.modal-body
       [:form#new-entity-ident-form
        [validated-input/text {:label "Ident"
                               :route :index
                               :form :new-entity-ident-form
                               :field :ident
                               :color-scheme color-scheme
                               :tool-tip "Text will be turned into an appropriately formatted and namespaced keyword. Must not already exist in the currently selected namespace. Examples: \"first-name\" or \"Song Title\""}]
        [validated-input/text {:label "Doc String"
                               :color-scheme color-scheme
                               :route :index
                               :form :new-entity-ident-form
                               :tool-tip "The optional doc specifies a documentation string, and can be any string value."
                               :field :doc}]
        [:div.two-column-row
         [validated-input/select-box {:color-scheme color-scheme
                                      :label "Cardinality"
                                      :route :index
                                      :form :new-entity-ident-form
                                      :field :cardinality
                                      :tool-tip "Specifies whether an attribute associates a single value or a set of values with an entity"
                                      :options (if (contains? #{{"value" "composite-tuple" "label" "Composite Tuple"}
                                                                {"value" "fixed-length-tuple" "label" "Fixed Length Tuple"}
                                                                {"value" "variable-length-tuple" "label" "Variable Length Tuple"}} (:value value-type))
                                                 [{:value "one" :label "One"}]
                                                 [{:value "one" :label "One"}
                                                  {:value "many" :label "Many"}])}]
         [validated-input/select-box {:color-scheme color-scheme
                                      :label "Value Type"
                                      :route :index
                                      :form :new-entity-ident-form
                                      :field :value-type
                                      :tool-tip "The :db/valueType attribute specifies the type of value that can be associated with an attribute."
                                      :options [{:value "bigdec" :label "Big Decimal"}
                                                {:value "bigint" :label "Big Integer"}
                                                {:value "boolean" :label "Boolean"}
                                                {:value "double" :label "Double"}
                                                {:value "float" :label "Float"}
                                                {:value "instant" :label "Instant"}
                                                {:value "keyword" :label "Keyword"}
                                                {:value "long" :label "Long"}
                                                {:value "ref" :label "Reference"}
                                                {:value "string" :label "String"}
                                                {:value "symbol" :label "Symbol"}
                                                {:value "composite-tuple" :label "Composite Tuple"}
                                                {:value "fixed-length-tuple" :label "Fixed Length Tuple"}
                                                {:value "variable-length-tuple" :label "Variable Length Tuple"}
                                                {:value "uuid" :label "UUID"}
                                                {:value "uri" :label "URI"}]}]]
        (when (= {"value" "ref" "label" "Reference"} (:value value-type))
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "References Namespaces"
                                       :route :index
                                       :form :new-entity-ident-form
                                       :field :ref-namespaces
                                       :is-multi true
                                       :tool-tip "What namespaces are expected to be referenced by this ident"
                                       :options ns-options}])
        (when (= {"value" "composite-tuple" "label" "Composite Tuple"} (:value value-type))
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "Tuple Attributes"
                                       :route :index
                                       :form :new-entity-ident-form
                                       :field :tuple-attrs
                                       :is-multi true
                                       :on-change (fn [evt]
                                                    (let [selections (js->clj evt)]
                                                      (when (<= (count selections) 8)
                                                        (>dis [::shared-events/set-form-field-value :index :new-entity-ident-form :tuple-attrs selections]))))
                                       :tool-tip "Composite tuples are derived from other attributes of the same entity. Composite tuple types have a :db/tupleAttrs attribute, whose value is 2-8 keywords naming other attributes."
                                       :options ns-ident-options}])
        (when (= {"value" "fixed-length-tuple" "label" "Fixed Length Tuple"} (:value value-type))
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "Tuple Attributes"
                                       :route :index
                                       :form :new-entity-ident-form
                                       :field :tuple-attrs
                                       :is-multi true
                                       :on-change (fn [evt]
                                                    (let [selections (js->clj evt)]
                                                      (when (<= (count selections) 8)
                                                        (>dis [::shared-events/set-form-field-value :index :new-entity-ident-form :tuple-attrs selections]))))
                                       :tool-tip "Heterogeneous fixed length tuples have a :db/tupleTypes attribute, whose value is a vector of 2-8 scalar value types."
                                       :options [{:value "bigdec" :label "Big Decimal"}
                                                 {:value "bigint" :label "Big Integer"}
                                                 {:value "boolean" :label "Boolean"}
                                                 {:value "double" :label "Double"}
                                                 {:value "instant" :label "Instant"}
                                                 {:value "keyword" :label "Keyword"}
                                                 {:value "long" :label "Long"}
                                                 {:value "ref" :label "Reference"}
                                                 {:value "string" :label "String"}
                                                 {:value "symbol" :label "Symbol"}
                                                 {:value "uuid" :label "UUID"}
                                                 {:value "uri" :label "URI"}]}])
        (when (= {"value" "variable-length-tuple" "label" "Variable Length Tuple"} (:value value-type))
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "Tuple Attributes"
                                       :route :index
                                       :form :new-entity-ident-form
                                       :field :tuple-attrs
                                       :tool-tip "Homogeneous variable length tuples have a :db/tupleType attribute, whose value is a keyword naming a scalar value type."
                                       :options [{:value "bigdec" :label "Big Decimal"}
                                                 {:value "bigint" :label "Big Integer"}
                                                 {:value "boolean" :label "Boolean"}
                                                 {:value "double" :label "Double"}
                                                 {:value "instant" :label "Instant"}
                                                 {:value "keyword" :label "Keyword"}
                                                 {:value "long" :label "Long"}
                                                 {:value "ref" :label "Reference"}
                                                 {:value "string" :label "String"}
                                                 {:value "symbol" :label "Symbol"}
                                                 {:value "uuid" :label "UUID"}
                                                 {:value "uri" :label "URI"}]}])
        [:div.two-column-row
         [validated-input/select-box {:color-scheme color-scheme
                                      :label "Unique"
                                      :route :index
                                      :form :new-entity-ident-form
                                      :field :unique
                                      :tool-tip "In order to add a uniqueness constraint to an attribute, both of the following must be true: 1) The attribute must have a :db/cardinality of :db.cardinality/one. 2) If there are values present for that attribute, they must be unique in the set of current database assertions."
                                      :options (if (= {"value" "many" "label" "Many"} (:value cardinality))
                                                 [{:value false :label "False"}]
                                                 [{:value false :label "False"}
                                                  {:value true :label "True"}])}]
         [validated-input/select-box {:color-scheme color-scheme
                                      :label "Is Component"
                                      :route :index
                                      :form :new-entity-ident-form
                                      :field :is-component
                                      :tool-tip "The optional :db/isComponent attribute specifies that an attribute whose :db/valueType is :db.type/ref refers to a sub-component of the entity to which the attribute is applied. When you retract an entity with :db.fn/retractEntity, all sub-components are also retracted."
                                      :options (if (= {"value" "ref" "label" "Reference"} (:value value-type))
                                                 [{:value false :label "False"}
                                                  {:value true :label "True"}]
                                                 [{:value false :label "False"}])}]]
        [:div.two-column-row
         [validated-input/select-box {:color-scheme color-scheme
                                      :label "No History"
                                      :route :index
                                      :form :new-entity-ident-form
                                      :field :no-history
                                      :tool-tip "The purpose of :db/noHistory is to conserve storage, not to make semantic guarantees about removing information. The effect of :db/noHistory happens in the background, and some amount of history may be visible even for attributes with :db/noHistory set to true. db/noHistory is often used for high churn attributes along with attributes that you do not require a history of."
                                      :options [{:value false :label "False"}
                                                {:value true :label "True"}]}]
         [validated-input/select-box {:color-scheme color-scheme
                                      :label "Deprecated"
                                      :route :index
                                      :form :new-entity-ident-form
                                      :field :deprecated
                                      :tool-tip "Documentation Only. Instead of removing schema this boolean flag indicates the field has been deprecated."
                                      :options [{:value false :label "False"} {:value true :label "True"}]}]]
        (when (= {"value" true "label" "True"} (:value deprecated))
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "Replaced By"
                                       :route :index
                                       :form :new-entity-ident-form
                                       :field :replaced-by
                                       :is-multi true
                                       :tool-tip "Documentation Only. Used to document what fields is intended to be the replacement for one that was deprecated."
                                       :options ns-options}])
        [validated-input/text {:label "Attribute Predicates"
                               :route :index
                               :form :new-entity-ident-form
                               :field :attr-preds
                               :color-scheme color-scheme
                               :tool-tip "<p>
                                             A fully-qualified symbol that names a predicate of a value.
                                             Multiple can be provided separated by a space (e.g., \"some-ns/ensure-proper-len some-ns/all-lower-case\").
                                          </p>
                                          <p>
                                             You may want to constrain an attribute value by more than just its storage/representation type.
                                             For example, an email address is not just a string, but a string with a particular format.
                                             In Datomic, you can assert attribute predicates about an attribute.
                                             Attribute predicates are asserted via the :db.attr/preds attribute, and are fully-qualified symbols that name a predicate of a value.
                                             Predicates return true (and only true) to indicate success.
                                             All other values indicate failure and are reported back as transaction errors.
                                          </p>"}]
        [:button.button {:type "submit"
                         :on-click (fn [evt]
                                     (.preventDefault evt)
                                     (>dis [::shared-events/submit-if-form-valid
                                            :index
                                            :new-entity-ident-form
                                            [::route-events/add-new-entity-ident]]))}
         "Add Ident"]]]]]))

(defn edit-ident-modal []
  (let [modal-visible? (<sub [::route-subs/modal-visible? :edit-ident])
        {:keys [color-scheme]} (when modal-visible? (<sub [::route-subs/settings]))
        deprecated (when modal-visible? (<sub [::shared-subs/form-field :index :edit-ident-form :deprecated]))
        already-deprecated? (when modal-visible? (<sub [::route-subs/ident-already-deprecated?]))
        {:keys [replaced-by-options existing-replaced-by]} (when modal-visible? (<sub [::route-subs/edit-ident-replaced-by-options]))
        ident-options (when modal-visible? (<sub [::route-subs/ns-ident-options]))
        ref-options (when modal-visible? (<sub [::route-subs/edit-ident-ref-options]))
        {:keys [is-ref-type? existing-references-namespaces]} (when modal-visible? (<sub [::route-subs/selected-ident-to-edit-ref-info]))
        ident-selected? (when modal-visible? (<sub [::route-subs/edit-ident-selected?]))]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Edit Ident"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/set-modal-visibility :edit-ident false])}]]
      [:div.modal-body
       [:form#edit-ident-form
        [validated-input/select-box {:color-scheme color-scheme
                                     :label "Ident Select"
                                     :route :index
                                     :form :edit-ident-form
                                     :field :ident
                                     :tool-tip "Select ident from current namespace to edit"
                                     :on-change #(do
                                                   (>dis [::shared-events/set-form-field-value :index :edit-ident-form :ident (js->clj %)])
                                                   (>dis [::route-events/set-edit-ident-values]))
                                     :options ident-options}]
        (when ident-selected?
          [validated-input/text {:label "Doc String"
                                 :color-scheme color-scheme
                                 :route :index
                                 :form :edit-ident-form
                                 :tool-tip "The optional doc specifies a documentation string, and can be any string value."
                                 :field :doc}])
        (when is-ref-type?
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "References Namespaces"
                                       :route :index
                                       :form :edit-ident-form
                                       :field :ref-namespaces
                                       :is-multi true
                                       :tool-tip "What namespaces are expected to be referenced by this ident"
                                       :options ref-options
                                       :additional-dom (when existing-references-namespaces
                                                         [:ul
                                                          (for [ref existing-references-namespaces]
                                                            ^{:key ref} [:li ref])])}])

        (when ident-selected?
          (if already-deprecated?
            [:div.field
             [:label.label "Ident has been Deprecated"]]
            [validated-input/select-box {:color-scheme color-scheme
                                         :label "Deprecated"
                                         :route :index
                                         :form :edit-ident-form
                                         :field :deprecated
                                         :tool-tip "Documentation Only. Instead of removing schema this boolean flag indicates the field has been deprecated."
                                         :options [{:value false :label "False"} {:value true :label "True"}]}]))
        (when (and ident-selected? (= {"value" true "label" "True"} (js->clj (:value deprecated))))
          [validated-input/select-box {:color-scheme color-scheme
                                       :label "Replaced By"
                                       :route :index
                                       :form :edit-ident-form
                                       :field :replaced-by
                                       :is-multi true
                                       :additional-dom (when existing-replaced-by
                                                         [:ul
                                                          (for [ident existing-replaced-by]
                                                            ^{:key ident} [:li ident])])
                                       :tool-tip "Documentation Only. Used to document what fields is intended to be the replacement for one that was deprecated."
                                       :options replaced-by-options}])
        (when ident-selected?
          [:button.button {:type "submit"
                           :on-click (fn [evt]
                                       (.preventDefault evt)
                                       (>dis [::shared-events/submit-if-form-valid
                                              :index
                                              :edit-ident-form
                                              [::route-events/edit-ident]]))}
           "Edit Ident"])]]]]))

(defn delete-ident-modal []
  (let [modal-visible? (<sub [::route-subs/modal-visible? :delete-ident])
        {:keys [color-scheme]} (when modal-visible? (<sub [::route-subs/settings]))
        ident-options (when modal-visible? (<sub [::route-subs/added-idents]))]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Delete Ident(s)"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/set-modal-visibility :delete-ident false])}]]
      [:div.modal-body
       [:form#new-ns-form
        [validated-input/select-box {:color-scheme color-scheme
                                     :label "Ident(s)"
                                     :route :index
                                     :form :delete-ident-form
                                     :is-multi true
                                     :field :idents
                                     :options ident-options}]
        [:button.button {:type "submit"
                         :on-click (fn [evt]
                                     (.preventDefault evt)
                                     (>dis [::shared-events/submit-if-form-valid
                                            :index
                                            :delete-ident-form
                                            [::route-events/delete-idents]]))}
         "Delete Idents"]]]]]))

(defn entity-card [{:keys [unique is-component? ident ident-kw cardinality doc value-type
                           deprecated? references-namespaces attr-preds tuple-attrs no-history? replaced-by]}]
  (let [{:keys [collapse-details?]} (<sub [::route-subs/settings])
        component-state (r/atom {:collapsed? collapse-details?})]
    (fn [{:keys [unique is-component? ident ident-kw cardinality doc value-type
                 deprecated? references-namespaces attr-preds tuple-attrs no-history? replaced-by]}]
      (let [{:keys [display-as-keywords?]} (<sub [::route-subs/settings])]
        [:div.entity-card
         [:div.entity-heading {:on-click #(do
                                            (>dis [::route-events/set-transition-max-height])
                                            (swap! component-state update :collapsed? not))
                               :style {:cursor "pointer"}}
          [:span.icon-div
           [:img.chevron {:class (when-not (:collapsed? @component-state) " chevron-open")
                          :src "img/chevron-right-dark.svg"
                          :style {:height "18px" :width "18px"}}]]
          [:span.ns-name (if display-as-keywords? ident-kw ident)]
          (when unique
            [tip [:span.attr-icon [:img {:src "img/key.svg"}]] "Unique" "top"])
          (when deprecated?
            [tip [:span.attr-icon [:img {:src "img/skull-dark.svg"}]] "Deprecated" "top"])
          (when no-history?
            [tip [:span.attr-icon [:img {:src "img/no-history-dark.svg"}]] "No History" "top"])]
         [:div.card-details.inset.collapsible {:class (when (:collapsed? @component-state) "collapsed")}
          (when value-type
            [:div [:span.heading "Type: "] value-type])
          (when attr-preds
            [:div
             [:span.heading "Attr Preds: "]
             [:ul
              (for [attr attr-preds]
                [:li {:key attr} (str attr)])]])
          (when tuple-attrs
            [:div
             [:span.heading "Tuple Attrs: "]
             [:ul
              (for [attr tuple-attrs]
                [:li {:key attr} (str attr)])]])
          (when cardinality
            [:div [:span.heading "Cardinality: "] cardinality])
          (when is-component?
            [:div [:span.heading "Is Component: "] "True"])
          (when (not-empty references-namespaces)
            [:div
             [:span.heading "References: "]
             [:ul
              (for [{:keys [label kw]} references-namespaces]
                [:li.link {:key label
                           :on-click #(>dis [::route-events/select-ns-and-push-previous kw])}
                 label])]])
          (when doc
            [:div
             [:span.heading "Description: "]
             [:div doc]])
          (when (not-empty replaced-by)
            [:div
             [:span.heading "Replaced By: "]
             [:ul
              (for [{:keys [label kw]} replaced-by]
                [:li.link {:key label
                           :on-click #(>dis [::route-events/select-ns-and-push-previous kw])}
                 (if display-as-keywords? (str kw) label)])]])]]))))

(defn ident-card [{:keys [ident ident-kw deprecated? doc replaced-by]}]
  (let [{:keys [display-as-keywords?]} (<sub [::route-subs/settings])]
    [:div.ident-card
     [:div.ident-heading
      [:span.ident-name (if display-as-keywords? ident-kw ident)]
      (when deprecated?
        [tip [:span.attr-icon [:img {:src "img/skull-dark.svg"}]] "Deprecated" "top"])]
     (when (or (not-empty doc) (not-empty replaced-by))
       [:div.card-details.inset
        (when doc
          [:div.detail
           [:span.heading "Description: "]
           [:div doc]])
        (when (not-empty replaced-by)
          [:div.detail
           [:span.heading "Replaced By: "]
           [:ul
            (for [{:keys [label kw]} replaced-by]
              [:li.link {:key label
                         :on-click #(>dis [::route-events/select-ns-and-push-previous kw])}
               (if display-as-keywords? (str kw) label)])]])])]))

(defn idents-tab []
  (let [selected-ns-detail (<sub [::route-subs/aside-selection-details])
        idents (:ns-idents selected-ns-detail)
        entities (:ns-entities selected-ns-detail)]
    [:div#idents-tab
     (if entities
       [:> TransitionGroup
        (for [{:keys [namespace ident] :as entity} entities]
          ^{:key (str namespace ident)}
          [:> CSSTransition
           {:classNames "panel-fade" :timeout 250}
           [entity-card entity]])]
       [:> TransitionGroup
        (for [ident idents]
          ^{:key (str namespace ident)}
          [:> CSSTransition
           {:classNames "panel-fade" :timeout 250}
           [ident-card ident]])])]))
; endregion

(defn left-panel []
  (let [active-tab (<sub [::route-subs/left-panel-active-tab])
        entity-selected? (<sub [::route-subs/entity-selected?])
        ident-selected? (<sub [::route-subs/ident-selected?])
        {:keys [color-scheme]} (<sub [::route-subs/settings])
        {:keys [doc referenced-by attrs attrs-kw preds]} (<sub [::route-subs/aside-selection-summary-info])
        {:keys [ns-have-been-added? idents-have-been-added?]} (<sub [::route-subs/schema-added-in-app?])
        tab (fn [tab-kw tab-name] [:div.tab {:class (when (= active-tab tab-kw) "active")
                                             :on-click #(>dis [::route-events/set-left-panel-active-tab tab-kw])}
                                   [:span tab-name]])]
    [:div.left-panel
     [:div.tabs
      [tab :ns "Namespaces"]
      [tab :ns-details "NS Details"]
      [tab :idents "Idents"]]
     [:div.panel-contents
      (case active-tab
        :ns [namespace-tab]
        :ns-details [namespace-details-tab]
        :idents [idents-tab])]
     (cond
       (= :ns active-tab) [:div#action-buttons
                           (when ns-have-been-added?
                             [:button.schema-action-btn.action-button
                              {:on-click #(do
                                            (>dis [::shared-events/reset-and-clear-form-field-values :index :delete-ns-form {}])
                                            (>dis [::route-events/update-existing-ref-notices []])
                                            (>dis [::route-events/set-modal-visibility :delete-ns true]))}
                              [:img {:style {:height "15px" :width "15px"}
                                     :src (if (= "dark" color-scheme) "img/delete-dark.svg" "img/delete-light.svg")}]])
                           [:button.schema-action-btn.action-button
                            {:on-click #(do
                                          (>dis [::shared-events/reset-and-clear-form-field-values :index :new-ns-form {:type {"value" "entity" "label" "Entity"}}])
                                          (>dis [::route-events/set-modal-visibility :new-ns true]))}
                            [:img {:style {:height "15px" :width "15px"}
                                   :src (if (= "dark" color-scheme) "img/plus-dark.svg" "img/plus-light.svg")}]]]
       (and entity-selected? (= :ns-details active-tab)) [:button.schema-action-btn.action-button
                                                          {:on-click #(do
                                                                        (>dis [::shared-events/reset-and-clear-form-field-values :index :edit-ns-form {:doc doc}])
                                                                        (>dis [::route-events/set-modal-visibility :edit-ns true]))}
                                                          [:img {:style {:height "15px" :width "15px"}
                                                                 :src (if (= "dark" color-scheme) "img/edit-dark.svg" "img/edit-light.svg")}]]
       (and entity-selected? (= :idents active-tab)) [:div#action-buttons
                                                      (when idents-have-been-added?
                                                        [:button.schema-action-btn.action-button
                                                         {:on-click #(do
                                                                       (>dis [::shared-events/reset-and-clear-form-field-values :index :delete-ident-form {}])
                                                                       (>dis [::route-events/set-modal-visibility :delete-ident true]))}
                                                         [:img {:style {:height "15px" :width "15px"}
                                                                :src (if (= "dark" color-scheme) "img/delete-dark.svg" "img/delete-light.svg")}]])
                                                      [:button.schema-action-btn.action-button
                                                       {:on-click #(do
                                                                     (>dis [::shared-events/reset-and-clear-form-field-values :index :edit-ident-form {}])
                                                                     (>dis [::route-events/set-modal-visibility :edit-ident true]))}
                                                       [:img {:style {:height "15px" :width "15px"}
                                                              :src (if (= "dark" color-scheme) "img/edit-dark.svg" "img/edit-light.svg")}]]
                                                      [:button.schema-action-btn.action-button
                                                       {:on-click #(do
                                                                     (>dis [::shared-events/reset-and-clear-form-field-values :index :new-entity-ident-form {}])
                                                                     (>dis [::route-events/set-modal-visibility :new-entity-ident true]))}
                                                       [:img {:style {:height "15px" :width "15px"}
                                                              :src (if (= "dark" color-scheme) "img/plus-dark.svg" "img/plus-light.svg")}]]]
       (and ident-selected? (= :idents active-tab)) [:div#action-buttons
                                                     (when idents-have-been-added?
                                                       [:button.schema-action-btn.action-button
                                                        {:on-click #(do
                                                                      (>dis [::shared-events/reset-and-clear-form-field-values :index :delete-ident-form {}])
                                                                      (>dis [::route-events/set-modal-visibility :delete-ident true]))}
                                                        [:img {:style {:height "15px" :width "15px"}
                                                               :src (if (= "dark" color-scheme) "img/delete-dark.svg" "img/delete-light.svg")}]])
                                                     [:button.schema-action-btn.action-button
                                                      {:on-click #(do
                                                                    (>dis [::shared-events/reset-and-clear-form-field-values :index :edit-ident-form {}])
                                                                    (>dis [::route-events/set-modal-visibility :edit-ident true]))}
                                                      [:img {:style {:height "15px" :width "15px"}
                                                             :src (if (= "dark" color-scheme) "img/edit-dark.svg" "img/edit-light.svg")}]]
                                                     [:button.schema-action-btn.action-button
                                                      {:on-click #(do
                                                                    (>dis [::shared-events/reset-and-clear-form-field-values :index :new-ident-form {:deprecated {"value" false "label" "False"}}])
                                                                    (>dis [::route-events/set-modal-visibility :new-ident true]))}
                                                      [:img {:style {:height "15px" :width "15px"}
                                                             :src (if (= "dark" color-scheme) "img/plus-dark.svg" "img/plus-light.svg")}]]])]))

(defn right-panel []
  (let [node-data-array (clj->js (<sub [::route-subs/node-data-array]))
        linked-data-array (clj->js (<sub [::route-subs/linked-data-array]))
        color-scheme (<sub [::shared-subs/graph-colors])]
    [:div.right-panel.body-panel
     [:div.panel-body
      (when node-data-array
        [:div#go-diagram
         ^{:key color-scheme} [gojs/diagram node-data-array linked-data-array color-scheme]])]]))

(defn read-schema-load-and-dispatch [file-contents]
  (let [schema (-> file-contents
                   .-target
                   .-result
                   edn/read-string)]
    (>dis [::route-events/load-schema schema])))

(defn load-schema-file-input
  [on-load-event]
  [:div.file
   [:div.file
    [:label.file-label
     [:input.file-input
      {:type "file"
       :accept ".edn"
       :style {:display "none"}
       :on-change (fn [evt]
                    (let [reader (js/FileReader.)
                          file (-> evt .-target .-files (aget 0))]
                      (set! (.-onload reader) #(on-load-event %))
                      (.readAsText reader file)))}]
     [:span#load-schema-btn.button
      [:span "Load Schema File"]]]]])

(defn load-schema []
  [:div#load-schema
   [:button#create-empty-schema-btn.button
    {:on-click #(>dis [::route-events/create-empty-schema])}
    "Create New Schema"]
   [load-schema-file-input read-schema-load-and-dispatch]])

(defn options-modal []
  (let [{:keys [modal-visible? collapse-details? display-as-keywords? color-scheme]} (<sub [::route-subs/settings])]
    [:div.modal-overlay.centered {:class (when modal-visible? "is-visible")}
     [:div.modal
      [:div.modal-header
       [:h4 "Options"]
       [:img {:src (if (= "dark" color-scheme)
                     "img/modal-close-dark.svg"
                     "img/modal-close-light.svg")
              :height "24"
              :on-click #(>dis [::route-events/settings :modal-visible? false])}]]
      [:div.modal-body

       [:div.setting
        [:h4.title "Namespace Details Default State"]
        [:div.split-select
         [:div.left
          {:class (when collapse-details? "selected-option")
           :on-click #(>dis [::route-events/settings :collapse-details? true])}
          [:span "Collapsed"]]
         [:div.right
          {:class (when-not collapse-details? "selected-option")
           :on-click #(>dis [::route-events/settings :collapse-details? false])}
          [:span "Expanded"]]]]

       [:div.setting
        [:h4.title "Attribute Display Style"]
        [:div.split-select
          [:div.left
           {:class (when display-as-keywords? "selected-option")
            :on-click #(>dis [::route-events/settings :display-as-keywords? true])}
           [:span "Keyword"]]
          [:div.right
           {:class (when-not display-as-keywords? "selected-option")
            :on-click #(>dis [::route-events/settings :display-as-keywords? false])}
           [:span "Pretty"]]]]

       [:div.setting
        [:h4.title "Color Scheme"]
        [:div.split-select
         [:div.left
          {:class (when (= "dark" color-scheme) "selected-option")
           :on-click #(do
                        (>dis [::route-events/settings :color-scheme "dark"])
                        (>dis [::route-events/settings :background-color "#282A36"]))}
          [:span "Dark"]]
         [:div.right
          {:class (when (= "light" color-scheme) "selected-option")
           :on-click #(do
                        (>dis [::route-events/settings :color-scheme "light"])
                        (>dis [::route-events/settings :background-color "#282828"]))}
          [:span "Light"]]]]
       [:div.setting
        [:h4.title "Unload Schema"]
        [:button#unload-btn.button {:on-click #(>dis [::route-events/unload-schema])}
         "Unload Schema"]]
       [:div.setting
        [:h4.title "Download Schema Transactions" [tip
                                                 [:img {:src (if (= "dark" color-scheme) "img/info-dark.svg" "img/info-light.svg")
                                                        :style {:height "15px" :top "2px" :position "relative" :left "5px" :margin-top "-5px"}}]
                                                 "EDN file containing the Datomic transactions to recreate the schema" "top"]]
        [:button#export-btn.button {:on-click #(>dis [::route-events/export-schema-txs])}
         "Download"]]
       [:div.setting
        [:h4.title "Download Schema File" [tip
                                           [:img {:src (if (= "dark" color-scheme) "img/info-dark.svg" "img/info-light.svg")
                                                  :style {:height "15px" :top "2px" :position "relative" :left "5px" :margin-top "-5px"}}]
                                           "EDN file that can be loaded into the schema cartographer application" "top"]]
        [:button#export-btn.button {:on-click #(>dis [::route-events/export-schema-file])}
         "Download"]]]]]))

(defn template []
  (let [schema-loaded? (<sub [::route-subs/schema-loaded?])]
    [:div
     [options-modal]
     [create-new-ns-modal]
     [create-new-entity-ident-modal]
     [create-new-ident-modal]
     [edit-ns-modal]
     [edit-ident-modal]
     [delete-ns-modal]
     [delete-ident-modal]
     [nav]
     (if schema-loaded?
       [:<>
        [secondary-nav]
        [:div.content-grid
         [left-panel]
         [right-panel]]
        [:div [:h3 "Footer"]]]
        [load-schema])]))
