(ns server.routes.analytics
  (:require
    [server.route-functions.analytics.get-ident-stats :as get-stats]
    [server.middleware.inject-datomic :refer [inject-datomic-mw]]))



(def routes
  ["/analytics" {:middleware [inject-datomic-mw]
                 :get get-stats/response}])
