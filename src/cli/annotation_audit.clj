(ns cli.annotation-audit
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.set :as set]
    [datomic.client.api :as d]
    [cli.queries :as query]))

(defn audit-schema [schema]
  (let [defined-namespaces (->> schema
                                (map #(select-keys % [:cartographer/enumeration :cartographer/entity]))
                                (map vals)
                                (remove nil?)
                                (map first)
                                (map name)
                                set)
        actual-namespaces (->> schema (map :db/ident) (remove nil?) (map (fn [attr] {:ns (namespace attr) :kw (name attr) :ident attr})) set)
        unannotated-ns (set/difference (->> actual-namespaces (map :ns) set) defined-namespaces)
        unannotated-attrs (select-keys (group-by :ns actual-namespaces) unannotated-ns)
        missing-ns-refs (->> schema
                             (filter (fn [{:keys [db/valueType cartographer/references-namespaces]}]
                                       (and (= {:db/ident :db.type/ref} valueType)
                                            (nil? references-namespaces))))
                             (map :db/ident)
                             sort)]
    {:unannotated-attrs unannotated-attrs
     :missing-ns-refs missing-ns-refs}))

(defn log-schema-audit [conn]
  (let [schema (query/schema (d/db conn))
        {:keys [unannotated-attrs missing-ns-refs]} schema]
    (if (some not-empty [unannotated-attrs missing-ns-refs])
      (do
        (println "\n=== Gaps In Annotations ===")
        (when unannotated-attrs
          (println "\n--- Attrs without a namespace ---")
          (doseq [[the-ns attrs] unannotated-attrs]
            (println " " the-ns)
            (doseq [attr attrs]
              (println "  " (:ident attr)))))
        (when missing-ns-refs
          (println "\n--- valueType ref with no annotated references ---")
          (doseq [ident missing-ns-refs]
            (println " " ident)))
        (println "\n"))
      (println "\n === Congrats! Your schema is fully annotated ===\n"))))
