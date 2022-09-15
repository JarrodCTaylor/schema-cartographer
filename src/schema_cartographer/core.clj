(ns schema-cartographer.core
  (:require
    [clojure.edn :as edn]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.string :as str]
    [clojure.pprint :as pp]
    [datomic.client.api :as client]
    [datomic.api :as peer]
    [schema-cartographer.queries :as queries]
    [schema-cartographer.annotation-audit :refer [log-schema-audit]]
    [schema-cartographer.explorer :refer [explore]]
    [schema-cartographer.schema :refer [data-map]]))

; region == Datomic Connection
(def client (memoize (fn [region system endpoint]
                       (client/client {:server-type :ion
                                       :region region
                                       :system system ; <stack-name> also called <system-name>
                                       :endpoint endpoint}))))

 (defn db-conn-from-args [region system db-name endpoint]
   (client/connect (client region system endpoint) {:db-name db-name}))

(defn db-conn-from-file [client-file db-name]
  (let [client (-> client-file slurp edn/read-string client/client)]
    (client/connect client {:db-name db-name})))
; endregion

(def cli-options
  [["-c" "--client-file CLIENT-FILE" "Filename containing edn client args"]
   ["-r" "--region REGION" "Region where Datomic cloud is located"]
   ["-s" "--system SYSTEM" "Datomic cloud system name"]
   ["-p" "--endpoint END-POINT" "ClientApiGatewayEndpoint"]
   ["-d" "--db DATABASE" "Database Name"]
   ["-o" "--output FILE" "Write schema edn to FILE"]
   ["-l" "--ref-search-limit REF-SEARCH-LIMIT" "The number of referenced entities to inspect when building relationships"
    ;; :default 1000000
    :parse-fn #(Integer/parseInt %)]
   ["-a" "--audit" "Audit schema annotations and log gaps. Boolean"]
   ["-e" "--explore" "Explore unannotated database and build schema. Boolean"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["== Schema Export =="
        "Usage: clojure -m schema-cartographer.core [options]"
        ""
        "Options:"
        options-summary
        ""
        "== Audit schema annotations and log gaps =="
        "clojure -m schema-cartographer.core -r [region] -s [system] -d [database] --audit"]
       (str/join \newline)))

(defn get-db [client-file db-name region system endpoint]
  (let [conn (if client-file
               (db-conn-from-file client-file db-name)
               (db-conn-from-args region system db-name endpoint))]
    (client/db conn)))

 (defn save-schema-edn [db schema-file-name db-type]
   (let [raw-schema (queries/annotated-schema db db-type)
         schema-data (data-map raw-schema)
         output-location schema-file-name]
     (spit output-location (with-out-str (pp/pprint schema-data)))
     (println (str "== " output-location " successfully saved. =="))))

(defn save-explore-schema-edn [db schema-file-name ref-search-limit db-type]
  (let [_ (println "== Querying Schema ==\n")
        raw-schema (queries/unannotated-schema {:db db :db-type db-type})
        _ (println "-- Exploring Database --\n")
        schema-data (explore db raw-schema ref-search-limit db-type)
        output-location schema-file-name]
    (spit output-location (with-out-str (pp/pprint schema-data)))
    (println (str "== " output-location " successfully saved. =="))))

(defn -main [& args]
  (let [{:keys [summary]
         {:keys [help region client-file system db endpoint output ref-search-limit audit explore]} :options} (parse-opts args cli-options)]
    (cond
      help                                                    (println (usage summary))
      (and (not audit) (nil? output))                         (println (usage summary))
      (and (some nil? [region system db]) (nil? client-file)) (println (usage summary))
      (and client-file (nil? db))                             (println (usage summary))
      audit (log-schema-audit (get-db client-file db region system endpoint))
      explore (save-explore-schema-edn (get-db client-file db region system endpoint) output ref-search-limit :cloud)
      :else (save-schema-edn (get-db client-file db region system endpoint) output :cloud))))