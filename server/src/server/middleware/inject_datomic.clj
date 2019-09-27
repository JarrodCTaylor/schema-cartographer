(ns server.middleware.inject-datomic
  (:require
    [datomic.client.api :as d]))

(defn get-db
  "This is used to allow easily overriding the injected db in tests."
  [db]
  db)

(defn datomic-arg-map [region system]
  {:server-type :ion
   :region region
   :system system ; <stack-name> also called <system-name>
   :endpoint (str "http://entry." system "." region ".datomic.net:8182/")
   :proxy-port 8182})

(def client (memoize (fn [region system]
                       (d/client (datomic-arg-map region system)))))

(defn db-conn [region system db-name]
  (d/connect (client region system) {:db-name db-name}))

(def inject-datomic-mw
  {:name ::inject-datomic
   :summary "Inject a Datomic connection into the request as a value to key `:datomic`"
   :wrap (fn [handler]
           (fn [{{:strs [region system db-name]} :headers :as request}]
             (let [conn (db-conn region system db-name)
                   db (get-db (d/db conn))]
               (handler (assoc request :datomic {:conn conn :db db})))))})
