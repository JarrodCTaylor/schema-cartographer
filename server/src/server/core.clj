(ns server.core
  (:require
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.string :as str]
    [clojure.pprint :as pp]
    [datomic.client.api :as d]
    [server.queries :as queries]
    [server.route-functions.schema.get-schema :refer [schema-data]]
    [server.middleware.inject-datomic :refer [db-conn]]))

(def cli-options
  [["-r" "--region REGION" "Region where Datomic cloud is located"]
   ["-s" "--system SYSTEM" "Datomic cloud system name"]
   ["-d" "--db DATABASE" "Database Name"]
   ["-o" "--output FILE" "Write schema edn to FILE"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Schema Cartographer Server:"
        "Usage: clojure -Alocal-server"
        ""
        "Schema Cartographer Schema Export:"
        "Usage: clojure -m server.core [options]"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn save-schema-edn [region system db-name schema-file-name]
  (let [conn (db-conn region system db-name)
        raw-schema (queries/schema (d/db conn))
        schema-data (schema-data raw-schema)
        output-location (str "doc/" schema-file-name ".edn")]
    (spit output-location (with-out-str (pp/pprint schema-data)))
    (println (str "== " output-location " successfully saved. =="))))

(defn -main [& args]
  (let [{:keys [summary]
         {:keys [help region system db output]} :options} (parse-opts args cli-options)]
    (if (or help (some nil? [region system db output]))
      (println (usage summary))
      (save-schema-edn region system db output))))