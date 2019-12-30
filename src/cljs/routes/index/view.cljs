(ns cljs.routes.index.view
  (:require
    ["react-transition-group" :refer (CSSTransition TransitionGroup)]
    [reagent.core :as r]
    [clojure.edn :as edn]
    [cljs.components.gojs :as gojs]
    [cljs.utils.helpers :refer [<sub >dis]]
    [cljs.routes.index.subs :as route-subs]
    [cljs.subs :as shared-subs]
    [cljs.events :as shared-events]
    [cljs.components.validated-input :as validated-input]
    [cljs.routes.index.events :as route-events]))

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

(defn entity-card [{:keys [unique is-component? ident ident-kw cardinality doc value-type
                           deprecated? references-namespaces attr-preds tuple-attrs no-history?]}]
  (let [{:keys [collapse-details?]} (<sub [::route-subs/settings])
        component-state (r/atom {:collapsed? collapse-details?})]
    (fn [_]
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
            [:span.attr-icon {:data-tooltip-left true :data-tooltip-content "Unique"} [:img {:src "img/key.svg"}]])
          (when deprecated?
            [:span.attr-icon {:data-tooltip-left true :data-tooltip-content "Deprecated"} [:img {:src "img/skull-dark.svg"}]])
          (when no-history?
            [:span.attr-icon {:data-tooltip-left true :data-tooltip-content "No History"} [:img {:src "img/no-history-dark.svg"}]])]
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
             [:div doc]])]]))))

(defn ident-card [{:keys [ident ident-kw deprecated? doc]}]
  (let [{:keys [display-as-keywords?]} (<sub [::route-subs/settings])]
    [:div.ident-card
     [:div.ident-heading
      [:span.ident-name (if display-as-keywords? ident-kw ident)]
      (when deprecated?
        [:span.attr-icon {:data-tooltip-left true :data-tooltip-content "Deprecated"} [:img {:src "img/skull-dark.svg"}]])]
     (when doc
       [:div
        [:span
         {:style {:font-weight "bold" :margin-right "3px"}}
         "Description: "]
        [:div doc]])]))

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

(defn left-panel []
  (let [active-tab (<sub [::route-subs/left-panel-active-tab])
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
        :idents [idents-tab])]]))

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
   [load-schema-file-input read-schema-load-and-dispatch]])

(defn options-modal []
  (let [{:keys [modal-visible? collapse-details? display-as-keywords? color-scheme]} (<sub [::route-subs/settings])]
    [:div.modal-overlay {:class (when modal-visible? "is-visible")}
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
         "Unload Schema"]]]]]))

(defn template []
  (let [schema-loaded? (<sub [::route-subs/schema-loaded?])]
    [:div
     [options-modal]
     [nav]
     (if schema-loaded?
       [:<>
        [secondary-nav]
        [:div.content-grid
         [left-panel]
         [right-panel]]
        [:div [:h3 "Footer"]]]
       [load-schema])]))
