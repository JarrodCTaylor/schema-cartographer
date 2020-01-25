(ns cli.annotation-audit
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.set :as set]
    [datomic.client.api :as d]
    [cli.queries :as query]))

(defn get-schema
  "Lives as a stand alone function to allow redef'ing in tests"
  [conn]
  (query/schema (d/db conn)))

(defn audit-schema [conn]
  (let [ns-str "db\\.schema\\.(?:entity|ident)\\.namespace"
        schema (get-schema conn)
        idents (->> schema
                    (map :db/ident)
                    (map (fn [ident] {:ns (namespace ident) :kw (name ident) :ident ident})))
        filter-idents #(->> idents
                            (filter (fn [{:keys [ns]}] (re-matches %1 ns)))
                            (map %2)
                            set)
        defined-namespaces (filter-idents (re-pattern ns-str) :kw)
        actual-namespaces (filter-idents (re-pattern (str "^((?!" ns-str ").)*$")) :ns)
        unannotated-ns (set/difference actual-namespaces defined-namespaces)
        unannotated-idents (select-keys (group-by :ns idents) (map name unannotated-ns))
        missing-ns-refs (->> schema
                             (filter (fn [{:keys [db/valueType db.schema/references-namespaces]}]
                                       (and (= {:db/ident :db.type/ref} valueType)
                                            (nil? references-namespaces))))
                             (map :db/ident)
                             sort)]
    {:unannotated-idents unannotated-idents
     :missing-ns-refs missing-ns-refs}))

(defn log-schema-audit [conn]
  (let [{:keys [unannotated-idents missing-ns-refs]} (audit-schema conn)]
    (if (some not-empty [unannotated-idents missing-ns-refs])
      (do
        (println "\n=== Gaps In Annotations ===")
        (when unannotated-idents
          (println "\n--- idents without a namespace ---")
          (doseq [[the-ns idents] unannotated-idents]
            (println " " the-ns)
            (doseq [ident idents]
              (println "  " (:ident ident)))))
        (when missing-ns-refs
          (println "\n--- ref type with no annotated references ---")
          (doseq [ident missing-ns-refs]
            (println " " ident)))
        (println "\n"))
      (println "\n === Congrats! Your schema is fully annotated ===\n"))))

