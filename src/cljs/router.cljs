(ns cljs.router
  (:require
    [reitit.frontend :as rfront]
    [cljs.routes.index.view :as index]))

;; == ROUTE DEFINITIONS ==
;; Every route will consist of a vector with the first element being a string
;; representing the path, and an options map. The map at a minimum will contain
;; a `:name` and `:view`. Optionally `:dispatch-on-entry` key can be
;; provided which contains a vector of event vectors to dispatch when the route
;; is entered. These event vectors can include the keys `:query-params` and/or
;; `:path-params` as needed which will be replaced with the actual query and
;; path parameter maps when the event is dispatched.
(def routes
  (rfront/router
    ["/"
     [""             {:name :index
                      :view [index/template]}]]))
