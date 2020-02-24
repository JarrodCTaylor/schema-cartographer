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

 (defn db-conn-from-args [region system db-name]
   (d/connect (client region system) {:db-name db-name}))

(defn db-conn-from-file [client-file db-name]
  (let [client (-> client-file slurp edn/read-string d/client)]
    (d/connect client {:db-name db-name})))
; endregion

(def cli-options
  [["-c" "--client-file CLIENT-FILE" "Filename containing edn client args"]
   ["-r" "--region REGION" "Region where Datomic cloud is located"]
   ["-s" "--system SYSTEM" "Datomic cloud system name"]
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

 (defn save-schema-edn [client-file region system db-name schema-file-name]
   (let [conn (if client-file
                (db-conn-from-file client-file db-name)
                (db-conn-from-args region system db-name))
         raw-schema (queries/schema (d/db conn))
         schema-data (data-map raw-schema)
         output-location (str "doc/" schema-file-name ".edn")]
     (spit output-location (with-out-str (pp/pprint schema-data)))
     (println (str "== " output-location " successfully saved. =="))))

(defn -main [& args]
  (let [{:keys [summary]
         {:keys [help region client-file system db output audit]} :options} (parse-opts args cli-options)]
    (cond
      help                                                    (println (usage summary))
      (and (not audit) (nil? output))                         (println (usage summary))
      (and (some nil? [region system db]) (nil? client-file)) (println (usage summary))
      (and client-file (nil? db))                             (println (usage summary))
      audit (log-schema-audit (if client-file
                                (db-conn-from-file client-file db)
                                (db-conn-from-args region system db)))
      :else (save-schema-edn client-file region system db output))))
