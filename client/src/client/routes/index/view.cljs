(ns client.routes.index.view
  (:require
    [transition-group]
    [csstransition]
    [reagent.core :as r]
    [clojure.edn :as edn]
    [client.components.gojs :as gojs]
    [client.utils.helpers :refer [<sub >dis]]
    [client.routes.index.subs :as route-subs]
    [client.subs :as shared-subs]
    [client.events :as shared-events]
    [client.components.validated-input :as validated-input]
    [client.routes.index.events :as route-events]))

(defn primary-nav []
  (let [aside-filter (<sub [::route-subs/aside-filter])
        {:keys [color-scheme]} (<sub [::route-subs/settings])]
    [:div#primary-nav
     [:div#logo [:img {:src (if (= "dark" color-scheme)
                              "img/dark-schema-cartographer-1.svg"
                              "img/light-schema-cartographer-1.svg")}]]
     [:i#settings-icon.fa.fa-2x.fa-cog.is-pulled-right {:style {:cursor "pointer"}
                                                        :on-click #(do
                                                                     (>dis [::route-events/hide-analytic-info])
                                                                     (>dis [::route-events/settings :modal-visible? true]))}]
     [:div#ns-filter-input.control.has-icons-left
      [:input.input {:type "text  "
                     :placeholder "Filter"
                     :value aside-filter
                     :on-change #(>dis [::route-events/update-aside-filter (-> % .-target .-value)])}]
      [:span.icon.is-small.is-left
       [:i.fas.fa-filter]]]]))

(defn aside-section [section-label namespaces]
  (let [selected-ns (<sub [::route-subs/aside-selection])]
    [:<>
     [:p.menu-label section-label]
     [:> transition-group {:component "ul" :class "menu-list"}
      (for [{:keys [label ns]} namespaces]
        ^{:key label}
        [:> csstransition {:classNames "shrink-grow" :timeout 350}
         [:li
          [:a {:class (when (= selected-ns ns) "is-active")
               :on-click #(>dis [::route-events/select-ns ns])} label]]])]]))

(defn left-aside []
  (let [{:keys [entities idents]} (<sub [::route-subs/nav-schema])]
    [:aside#aside
     [:nav.menu
      [:div#aside-body
       [aside-section "Entities" entities]
       [aside-section "Idents" idents]]]]))

(defn sparkline-graph [data]
  (>dis [::route-events/init-sparkline data])
  [:<>
   [:svg.sparkline {:width "150" :height "40" :stroke-width "3"}]
   [:span.tooltip {:hidden "true"}]])

(defn entity-ns-analytic-info []
  (let [entity-selected? (<sub [::route-subs/entity-selected?])
        visible? (<sub [::route-subs/analytic-info-visible?])
        loading? (<sub [::route-subs/analytics-loading?])
        {:keys [entity-count oldest newest sparkline-data]} (<sub [::route-subs/analytic-info])]
    (when entity-selected?
      [:div#entity-ns-analytic-info.dropdown.is-right {:class (when visible? "is-active")}
       [:div.dropdown-trigger
        [:i#info-icon.fa.fa-2x.fa-info-circle.is-pulled-right {:on-click #(>dis [::route-events/toggle-analytic-info-visible])
                                                               :style {:cursor "pointer"}
                                                               :aria-haspopup "true" :aria-controls "analytic-info"}]]
       [:div#analytic-info.dropdown-menu {:role "menu"}
        (cond
          (= entity-count 0) [:div.dropdown-content
                              [:div.dropdown-item
                               [:p [:strong "None Exist"]]]]
          (>= entity-count 1) [:div.dropdown-content
                               [:div.dropdown-item
                                [:p [:strong "Count: "] entity-count]
                                [:p [:strong "Newest: "] newest]
                                [:p [:strong "Oldest: "] oldest]]
                               [:div#sparkline-graph.dropdown-item
                                [sparkline-graph sparkline-data]]]
          :else [:div.dropdown-content
                 [:div.dropdown-item
                  [:button.button.is-medium.is-fullwidth {:id "pull-analytics-button"
                                                          :class (when loading? "is-loading")
                                                          :on-click (fn [evt]
                                                                      (.preventDefault evt)
                                                                      (>dis [::route-events/get-analytic-info]))}
                   "Pull Analytics"]]])]])))

(defn namespace-summary []
  (let [{:keys [display-as-keywords?]} (<sub [::route-subs/settings])
        {:keys [doc label referenced-by by-way-of attrs attrs-kw preds]} (<sub [::route-subs/aside-selection-summary-info])]
    [:div#namespace-controls
     [:> transition-group
      [:> csstransition
       {:key label :classNames "panel-fade" :timeout 250}
       [:div#namespace-summary.notification
        [:div#by-way-of-breadcrumbs
         [:nav.breadcrumb.has-succeeds-separator {:aria-label "breadcrumbs"}
          [:ul
           (for [{:keys [ns label]} by-way-of]
             [:li {:key ns} [:a {:on-click #(>dis [::route-events/select-breadcrumb-ns ns])} label]])]]]
        [:div
         [:div [:span.title.is-2 label (when doc ": ")] [:span.is-size-5 doc]]]
        [:div
         (when (not-empty referenced-by) [:span.has-text-weight-bold.title.is-5 "Referenced By: "])
         (for [{:keys [ns label]} referenced-by]
           [:span.tag.is-tag-2 {:key ns
                                :on-click #(>dis [::route-events/select-ns ns])} label])]
        [:div
         (when (not-empty attrs) [:span.has-text-weight-bold.title.is-5 "Required Attributes: "])
         (for [attr (if display-as-keywords? attrs-kw attrs)]
           [:span.tag.is-tag-3 {:key attr} attr])]
        [:div
         (when (not-empty preds) [:span.has-text-weight-bold.title.is-5 "Entity Predicates: "])
         (for [attr preds]
           [:span.tag.is-tag-3 {:key attr} attr])]]]]]))

(defn entity-summary [{:keys [unique is-component? ident ident-kw cardinality doc value-type
                              deprecated? references-namespaces attr-preds tuple-attrs no-history?]}]
  (let [{:keys [collapse-details?]} (<sub [::route-subs/settings])
        component-state (r/atom {:collapsed? collapse-details?})]
    (fn [_]
      (let [{:keys [display-as-keywords?]} (<sub [::route-subs/settings])]
        [:nav.panel
         [:p.panel-heading {:on-click #(do
                                         (>dis [::route-events/set-transition-max-height])
                                         (swap! component-state update :collapsed? not))
                            :style {:cursor "pointer"}}
          [:span.icon-div
           [:i.fas.chevron {:class (str "fa-chevron-right" (when-not (:collapsed? @component-state) " chevron-open"))}]] (if display-as-keywords? ident-kw ident)
          (when unique
            [:span.attr-icon.is-pulled-right {:data-tooltip-top true :data-tooltip-content "Unique"} [:i.fas.fa-key.detail-icon]])
          (when deprecated?
            [:span.attr-icon.is-pulled-right {:data-tooltip-top true :data-tooltip-content "Deprecated"} [:i.fas.fa-skull.detail-icon]])
          (when no-history?
            [:span.attr-icon.is-pulled-right {:data-tooltip-top true :data-tooltip-content "No History"} [:i.fas.fa-ban.detail-icon]])]
         [:div.panel-details.collapsible {:class (when (:collapsed? @component-state) "collapsed")}
          (when value-type
            [:p.panel-block [:span.has-text-weight-bold {:style {:margin-right "3px"}} "Type:"] value-type])
          (when attr-preds
            [:p.panel-block
             [:span.has-text-weight-bold
              {:style {:margin-right "3px"}}
              "Attr Preds: " (for [attr attr-preds]
                               [:span.tag.is-tag-3 {:key attr} (str attr)])]])
          (when tuple-attrs
            [:p.panel-block
             [:span.has-text-weight-bold
              {:style {:margin-right "3px"}}
              "Tuple Attrs: " (for [attr tuple-attrs]
                                [:span.tag.is-tag-3 {:key attr} (str attr)])]])
          (when cardinality
            [:p.panel-block [:span.has-text-weight-bold {:style {:margin-right "3px"}} "Cardinality:"] cardinality])
          (when is-component?
            [:p.panel-block [:span.has-text-weight-bold {:style {:margin-right "3px"}} "Is Component: True"]])
          (when (not-empty references-namespaces)
            [:p.panel-block
             [:span.has-text-weight-bold
              {:style {:margin-right "3px"}}
              "References: " (for [{:keys [label kw]} references-namespaces]
                               [:span.tag.is-tag-1 {:key label
                                                    :on-click #(>dis [::route-events/select-ns-and-push-previous kw])}
                                label])]])
          (when doc
            [:p.panel-block doc])]]))))

(defn namespace-details []
  (let [selected-ns-detail (<sub [::route-subs/aside-selection-details])
        idents (:ns-idents selected-ns-detail)
        entities (:ns-entities selected-ns-detail)]
    [:article.namespace-details.body-panel
     [:div.panel-body
      (if entities
        [:> transition-group
         (for [{:keys [namespace ident] :as entity} entities]
           ^{:key (str namespace ident)}
           [:> csstransition
            {:classNames "panel-fade" :timeout 250}
            [entity-summary entity]])]
        [:> transition-group
         (for [{:keys [ident namespace doc deprecated?]} idents]
           ^{:key (str namespace ident)}
           [:> csstransition
            {:classNames "panel-fade" :timeout 250}
            [:nav.panel
             [:p.panel-heading ident (when deprecated?
                                       [:span.attr-icon.is-pulled-right {:data-tooltip-top true :data-tooltip-content "Deprecated"} [:i.fas.fa-skull.detail-icon]])]
             (when doc [:p.panel-block doc])]])])]
     [:div#detail-bottom.panel-body]]))

(defn relationship-graph []
  (let [node-data-array (clj->js (<sub [::route-subs/node-data-array]))
        linked-data-array (clj->js (<sub [::route-subs/linked-data-array]))
        color-scheme (<sub [::shared-subs/graph-colors])
        cmd-handler (<sub [::route-subs/graph-command-handler])]
    [:article.relationship-graph.body-panel
     [:div.panel-body
      (when node-data-array
        [:<>
         [:div#relationship-graph-controls.is-pulled-right
          [:i.fa.fa-2x.fa-search-minus {:on-click #(.decreaseZoom cmd-handler)
                                        :style {:cursor "pointer"}}]
          [:i.fa.fa-2x.fa-search-plus {:on-click #(.increaseZoom cmd-handler)
                                       :style {:cursor "pointer"}}]
          [:i.fa.fa-2x.fa-arrows-alt {:on-click #(.zoomToFit cmd-handler)
                                      :style {:cursor "pointer"}}]
          [:i.fa.fa-2x.fa-camera {:on-click #(>dis [::route-events/save-graph-to-file])
                                  :style {:cursor "pointer"}}]]
         [:div#go-diagram
          ^{:key color-scheme} [gojs/diagram node-data-array linked-data-array color-scheme]]])]]))

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
                      (.readAsText reader file)
                      (>dis [::route-events/read-only true])))}]
     [:span#file-input-cta.file-cta.button.is-medium
      [:span.file-icon [:i.fas.fa-upload]]
      [:span.file-label "Select File"]]]]])

(defn load-schema-inputs []
  (let [active-tab (<sub [::route-subs/load-schema-active-tab])
        {:keys [color-scheme]} (<sub [::route-subs/settings])]
    [:div#load-schema-inputs
     [:div.card.is-centered
      [:div.card-image
       [:figure
        [:img {:src (case color-scheme
                      "dark" "img/dark-schema-cartographer-2.svg"
                      "light" "img/light-schema-cartographer-2.svg")}]]]
      [:div#load-form.card-content
        [:p.panel-heading "Load Schema"]
        [:p.panel-tabs
         [:a {:class (when (= :local-server active-tab) "is-active")
              :on-click #(>dis [::route-events/switch-load-schema-tab :local-server])}
          "Local Server"]
         [:a {:class (when (= :file active-tab) "is-active")
              :on-click #(>dis [::route-events/switch-load-schema-tab :file])}
          "File"]]
        (if (= :file active-tab)
          [:> transition-group {:class "tab-transition"}
           [:> csstransition
            {:key "from-file" :classNames "tab-fade" :timeout 250}
            [:div#file-upload.panel-block
             [load-schema-file-input read-schema-load-and-dispatch]]]]
          [:> transition-group {:class "tab-transition"}
           [:> csstransition
            {:key "from-server" :classNames "tab-fade" :timeout 250}
            [:div#local-server-block.panel-block
             [:form#local-server-form
              [validated-input/text {:label "System" :route :index :form :load-schema-form :field :system}]
              [validated-input/text {:label "Region" :route :index :form :load-schema-form :field :region}]
              [validated-input/text {:label "Database Name" :route :index :form :load-schema-form :field :database-name}]
              [:button.button.is-medium.is-fullwidth {:type "submit"
                                                      :on-click (fn [evt]
                                                                  (.preventDefault evt)
                                                                  (>dis [::shared-events/submit-if-form-valid
                                                                         :index
                                                                         :load-schema-form
                                                                         [::route-events/get-schema]
                                                                         [::route-events/read-only false]]))}
               "Get Schema"]]]]])]]]))

(defn settings-modal []
  (let [{:keys [modal-visible? collapse-details? display-as-keywords? color-scheme]} (<sub [::route-subs/settings])]
    [:div#settings-modal.modal {:class (when modal-visible? "is-active")}
     [:div.modal-background {:on-click #(>dis [::route-events/settings :modal-visible? false])}]
     [:div.modal-card
      [:header.modal-card-head
       [:p.modal-card-title "Settings"]
       [:button.delete {:on-click #(>dis [::route-events/settings :modal-visible? false])}]]
      [:section.modal-card-body
       [:div.settings-section
        [:h4.title.is-4 "Namespace Details Default State"]
        [:div.field.has-addons
         [:p.control
          [:a.button
           {:class (when collapse-details? "selected-option")
            :on-click #(>dis [::route-events/settings :collapse-details? true])}
           [:span "Collapsed"]]]
         [:p.control
          [:a.button
           {:class (when-not collapse-details? "selected-option")
            :on-click #(>dis [::route-events/settings :collapse-details? false])}
           [:span "Expanded"]]]]]
       [:div.settings-section
        [:h4.title.is-4 "Attribute Display Style"]
        [:div.field.has-addons
         [:p.control
          [:a.button
           {:class (when display-as-keywords? "selected-option")
            :on-click #(>dis [::route-events/settings :display-as-keywords? true])}
           [:span "Keyword"]]]
         [:p.control
          [:a.button
           {:class (when-not display-as-keywords? "selected-option")
            :on-click #(>dis [::route-events/settings :display-as-keywords? false])}
           [:span "Pretty"]]]]]
       [:div.settings-section
        [:h4.title.is-4 "Color Scheme"]
        [:div.field.has-addons
         [:p.control
          [:a.button
           {:class (when (= "dark" color-scheme) "selected-option")
            :on-click #(do
                         (>dis [::route-events/settings :color-scheme "dark"])
                         (>dis [::route-events/settings :background-color "#282A36"]))}
           [:span "Dark"]]]
         [:p.control
          [:a.button
           {:class (when (= "light" color-scheme) "selected-option")
            :on-click #(do
                         (>dis [::route-events/settings :color-scheme "light"])
                         (>dis [::route-events/settings :background-color "#282828"]))}
           [:span "Light"]]]]]
       [:div.settings-section
        [:h4.title.is-4 "Unload Schema"]
        [:button.button.is-danger.unload-schema-button {:on-click #(>dis [::route-events/unload-schema])}
         "Unload Schema"]]]]]))

(defn template []
  (let [schema-loaded? (<sub [::route-subs/schema-loaded?])
        read-only? (<sub [::route-subs/read-only?])]
    (if schema-loaded?
      [:div.grid-container
       (when-not read-only? [entity-ns-analytic-info])
       [settings-modal]
       [primary-nav]
       [left-aside]
       [namespace-summary]
       [namespace-details]
       [relationship-graph]]
      [load-schema-inputs])))
