(ns schema-cartographer.unit.utils
  (:require
    [datomic.client.api :as d]))

(def client (d/client {:system "cartographer-test"
                       :server-type :dev-local}))

(defn ensure-db [db-name]
  (when-not (some #{db-name} (d/list-databases client {}))
    (d/create-database client {:db-name db-name})))

(defn db-conn [db-name]
  (ensure-db db-name)
  (d/connect client {:db-name db-name}))

(defn setup-speculative-db [db-name schema txs]
  (let [conn (db-conn db-name)
        db (d/with-db conn)
        db-with-schema (:db-after (d/with db {:tx-data schema}))]
    (:db-after (d/with db-with-schema {:tx-data txs}))))