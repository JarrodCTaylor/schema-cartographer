(ns server.handler
  (:require
    [muuntaja.core :as m]
    [reitit.ring :as ring]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [ring.middleware.params :as params]
    [server.middleware.cors :refer [cors-mw options-mw]]
    [server.routes.analytics :as analytics]
    [server.routes.schema :as schema]))

(def app
  (ring/ring-handler
    (ring/router
      ["/api/v1" [schema/routes]
                 [analytics/routes]]
      {:data {:muuntaja m/instance
              :middleware [options-mw
                           cors-mw
                           params/wrap-params
                           muuntaja/format-middleware]}})
    (ring/create-default-handler)))
