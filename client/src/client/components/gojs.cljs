(ns client.components.gojs
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [client.utils.helpers :refer [<sub >dis]]))

(rf/reg-event-db
  ::diagram-command-handler
  (fn [db [_ ch]]
    (assoc-in db [:routes :index :diagram :command-handler] ch)))

(rf/reg-event-db
  ::diagram
  (fn [db [_ ch]]
    (assoc-in db [:routes :index :diagram :diagram] ch)))

(rf/reg-sub
  ::diagram
  (fn [db _]
    (-> db :routes :index :diagram :diagram)))

(def make-graph-obj (js* "go.GraphObject.make"))

(defn diagram [node-data-array linked-data-array {:keys [color-1 color-2 color-3 color-4 color-5]}]
    (r/create-class
     {:reagent-render (fn [] [:div#diagramDiv {:style {:width "inherit" :height "inherit" :background-color color-2}}])
      :component-did-mount (fn [this]
                             (let [the-diagram (make-graph-obj (.-Diagram js/go)
                                                               "diagramDiv"
                                                               (clj->js {"undoManager.isEnabled" true
                                                                         :allowDelete false
                                                                         :allowCopy false
                                                                         :layout (make-graph-obj (.-ForceDirectedLayout js/go))}))
                                   _ (set! (.-doFocus ^js/go.Diagram the-diagram) (fn [] nil)) ;; Prevents a window.scroll bug in Chrome
                                   _ (>dis [::diagram ^js/go.Diagram the-diagram])
                                   _ (>dis [::diagram-command-handler (.-commandHandler ^js/go.Diagram the-diagram)])
                                   item-template (make-graph-obj (.-Panel js/go)
                                                                 "Horizontal"
                                                                 (make-graph-obj (.-Shape js/go)
                                                                                 (clj->js {:desiredSize (js/go.Size. 15 15)})
                                                                                 (js/go.Binding. "figure" "figure")
                                                                                 (js/go.Binding. "fill" "color"))
                                                                 (make-graph-obj (.-TextBlock js/go)
                                                                                 (clj->js {:stroke "#333333"
                                                                                           :font "bold 14px sans-serif"})
                                                                                 (js/go.Binding. "text" "attr")))]
                               (set! (.-nodeTemplate ^js/go.Diagram the-diagram)
                                     (make-graph-obj (.-Node js/go)
                                                     "Auto"
                                                     (clj->js {:selectionAdorned false
                                                               :resizable false
                                                               :fromSpot (.. js/go -Spot -AllSides)
                                                               :toSpot (.. js/go -Spot -AllSides)})
                                                     (.makeTwoWay (js/go.Binding. "location" "location"))
                                                     ; whenever the PanelExpanderButton changes the visible property of the "LIST" panel,
                                                     ; clear out any desiredSize set by the ResizingTool.
                                                     (.ofObject (js/go.Binding. "desiredSize" "visible" (fn [v]
                                                                                                          (js/go.Size. js/NaN js/NaN))) "LIST")
                                                     (make-graph-obj (.-Shape js/go) "Rectangle" (clj->js {#_#_:parameter1 12 ;; So some unthinkable reason this is how to specify border radius
                                                                                                           :fill color-1 :stroke color-1 :strokeWidth 1}))
                                                     (make-graph-obj (.-Panel js/go)
                                                                     "Table"
                                                                     (clj->js {#_#_:padding 8
                                                                               :stretch (.. js/go -GraphObject -Fill)
                                                                               :margin 2})
                                                                     (make-graph-obj (.-RowColumnDefinition js/go) (clj->js {:row 0
                                                                                                                             :background color-1
                                                                                                                             :separatorPadding 3
                                                                                                                             :stretch (.. js/go -GraphObject -Fill)}))
                                                                     ; Table header
                                                                     (make-graph-obj (.-TextBlock js/go)
                                                                                     (clj->js {
                                                                                               :row 0
                                                                                               :alignment (.. js/go -Spot -Center)
                                                                                               :margin (js/go.Margin. 0 14 0 2)
                                                                                               :stroke color-4 ;; This is the text color
                                                                                               :font "bold 18px sans-serif"})
                                                                                     (js/go.Binding. "text" "key"))
                                                                     ; Collapse/expand button
                                                                     #_(make-graph-obj "PanelExpanderButton"
                                                                                       "LIST" ;; The name of element whose visibility this button toggles
                                                                                       (clj->js {:row 0
                                                                                                 :alignment (.. js/go -Spot -TopRight)}))
                                                                     ; The list of panels each showing an attribute
                                                                     (make-graph-obj (.-Panel js/go)
                                                                                     "Vertical"
                                                                                     (clj->js {:name "LIST"
                                                                                               :row 1
                                                                                               :padding 10
                                                                                               :background color-4
                                                                                               :alignment (.. js/go -Spot -TopLeft)
                                                                                               :defaultAlignment (.. js/go -Spot -Left)
                                                                                               :stretch (.. js/go -GraphObject -Horizontal)
                                                                                               :itemTemplate item-template})
                                                                                     (js/go.Binding. "itemArray" "items")))))
                               (set! (.-linkTemplate ^js/go.Diagram the-diagram) (make-graph-obj (.-Link js/go)
                                                                                  ;; The whole link panel
                                                                                  (clj->js {:selectionAdorned true,
                                                                                            :layerName "Foreground",
                                                                                            :reshapable true,
                                                                                            :routing (.. js/go -Link -AvoidsNodes),
                                                                                            :corner 5,
                                                                                            :selectable true
                                                                                            :curve (.. js/go -Link -JumpOver)})
                                                                                  ;; Link shape
                                                                                  (make-graph-obj (.-Shape js/go) (clj->js {:stroke color-5 :strokeWidth 2.5}))
                                                                                  (make-graph-obj (.-Shape js/go) (clj->js {:toArrow "Standard" :stroke color-5 :strokeWidth 2.5 :fill color-5}))
                                                                                  ;; From label
                                                                                  (make-graph-obj (.-TextBlock js/go)
                                                                                                  (clj->js {:textAlign "center",
                                                                                                            :font "bold 14px sans-serif",
                                                                                                            :stroke color-3,
                                                                                                            :segmentIndex 0,
                                                                                                            :segmentOffset (js/go.Point. js/NaN js/NaN)
                                                                                                            :segmentOrientation (.. js/go -Link -OrientUpright)})
                                                                                                  (js/go.Binding. "text" "text"))
                                                                                  ;; To label
                                                                                  (make-graph-obj (.-TextBlock js/go)
                                                                                                  (clj->js {:textAlign "center",
                                                                                                            :font "bold 14px sans-serif",
                                                                                                            :stroke color-3
                                                                                                            :segmentIndex -1,
                                                                                                            :segmentOffset (js/go.Point. js/NaN js/NaN),
                                                                                                            :segmentOrientation (.. js/go -Link -OrientUpright)})
                                                                                                  (js/go.Binding. "text" "toText"))))
                               (set! (.-model ^js/go.Diagram the-diagram)
                                     (make-graph-obj (.-GraphLinksModel js/go)
                                                     (clj->js {:copiesArrays true,
                                                               :copiesArrayObjects true,
                                                               :nodeDataArray node-data-array
                                                               :linkDataArray linked-data-array})))))
      :component-will-update (fn [_ [_ node-data-array linked-data-array]]
                               (let [the-diagram (<sub [::diagram])]
                                 (set! (.-model ^js/go.Diagram the-diagram)
                                       (make-graph-obj (.-GraphLinksModel js/go)
                                                       (clj->js {:copiesArrays true,
                                                                 :copiesArrayObjects true,
                                                                 :nodeDataArray node-data-array
                                                                 :linkDataArray linked-data-array})))))}))
