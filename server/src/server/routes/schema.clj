(ns server.routes.schema
  (:require
    [server.route-functions.schema.get-schema :as get-schema]
    [server.middleware.inject-datomic :refer [inject-datomic-mw]]))

(def routes
  ["/schema" {:middleware [inject-datomic-mw]
              :get get-schema/response}])
