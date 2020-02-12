(ns cli.core
  (:require
    [clojure.edn :as edn]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.string :as str]
    [clojure.pprint :as pp]
    [datomic.client.api :as d]
    [cli.queries :as queries]
    [cli.annotation-audit :refer [log-schema-audit]]
    [cli.schema :refer [data-map]]))

; region == Datomic Connection
(defn datomic-arg-map [region system]
  {:server-type :ion
   :region region
   :system system ; <stack-name> also called <system-name>
   :endpoint (str "http://entry." system "." region ".datomic.net:8182/")
   :proxy-port 8182})

(def client (memoize (fn [region system]
                       (d/client (datomic-arg-map region system)))))

(defn db-conn [client-file region system db-name]
  (let [client (if client-file
                 (-> client-file slurp edn/read-string d/client)
                 (client region system))]
    (d/connect client {:db-name db-name})))
; endregion

(def cli-options
  [["-c" "--client-file CLIENT-FILE" "Filename containing edn client args"]
   ["-r" "--region REGION" "Region where Datomic cloud is located. Deprecated, see --client-file."]
   ["-s" "--system SYSTEM" "Datomic cloud system name. Deprecated, see --client-file."]
   ["-d" "--db DATABASE" "Database Name"]
   ["-o" "--output FILE" "Write schema edn to FILE"]
   ["-a" "--audit" "Audit schema annotations and log gaps. Boolean"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["== Schema Export =="
        "Usage: clojure -m clj.core [options]"
        ""
        "Options:"
        options-summary
        ""
        "== Audit schema annotations and log gaps =="
        "clojure -m clj.core -r [region] -s [system] -d [database] --audit"]
       (str/join \newline)))

(defn save-schema-edn [conn schema-file-name]
  (let [raw-schema (queries/schema (d/db conn))
        schema-data (data-map raw-schema)
        output-location (str "doc/" schema-file-name ".edn")]
    (spit output-location (with-out-str (pp/pprint schema-data)))
    (println (str "== " output-location " successfully saved. =="))))

(defn -main [& args]
  (let [{:keys [summary]
         {:keys [help region client-file system db output audit]} :options} (parse-opts args cli-options)]
    (cond
      (or help
          (and (not audit) (nil? output))
          (not (and db (or client-file (and region system))))) (println (usage summary))
      audit (log-schema-audit (db-conn client-file region system db))
      :else (save-schema-edn (db-conn client-file region system db) output))))
