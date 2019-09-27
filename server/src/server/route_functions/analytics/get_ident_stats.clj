(ns server.route-functions.analytics.get-ident-stats
  (:require
    [clojure.string :as str]
    [server.queries :as query])
  (:import
    [java.time Instant LocalDate ZoneOffset]
    [java.time.temporal ChronoField]
    [java.time.format DateTimeFormatter]))

(def month-day-year-formatter (DateTimeFormatter/ofPattern "MM/dd/yy"))
(def month-year-formatter (DateTimeFormatter/ofPattern "MM/yy"))

(defn inst->utc-local-date [i]
  (let [instant (Instant/ofEpochMilli (.getTime i))]
    (LocalDate/ofInstant instant ZoneOffset/UTC)))

(defn inst-to-str-representations [i]
  (let [ld (inst->utc-local-date i)]
    {:date-str (.format ld month-day-year-formatter)
     :month-year (.format ld month-year-formatter)
     :year (str (.get ld ChronoField/YEAR))}))

(defn sparkline-data [instances]
  (let [date-strs (map inst-to-str-representations instances)
        count-vals #(map (fn [[date insts]] {:date date :value (count insts) }) %)
        counted-group-by #(-> (group-by % date-strs) sort count-vals)
        by-day (counted-group-by :date-str)
        by-month-year (counted-group-by :month-year)
        by-year (counted-group-by :year)]
    (cond
      (>= 32 (count by-day)) by-day
      (>= 32 (count by-month-year)) by-month-year
      :else by-year)))

(defn response [{{:keys [db]} :datomic
                 {:strs [ident]} :query-params}]
  (let [attr (->> ident rest (str/join "") keyword)
        instances (query/attr-tx-instances db attr)]
    {:status 200
     :body (if (empty? instances)
             {:entity-count 0}
             {:entity-count (count instances)
              :oldest (-> instances first inst->utc-local-date (.format month-day-year-formatter))
              :newest (-> instances last inst->utc-local-date (.format month-day-year-formatter))
              :sparkline-data (sparkline-data instances)})}))

(comment

  (def x #inst "2019-08-23T13:29:20.942-00:00")
  (def ld (inst-to-str-representations x))

  (defn inspect-object
    [obj]
    (clojure.pprint/print-table
      [:name :return-type :declaring-class :parameter-types]
      (sort-by :name (filter :exception-types (:members (clojure.reflect/reflect obj))))))

  (inspect-object ld)
  (def dtf (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
  (.format ld dtf)

  (let [inst #inst "2019-08-23T13:29:20.942-00:00"
        ld (inst-to-str-representations inst)
        day (.get ld ChronoField/DAY_OF_MONTH)
        month (.get ld ChronoField/MONTH_OF_YEAR)
        year (.get ld ChronoField/YEAR)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (println (str/join "/" [month day year])) ; => "8/23/2019"
    (println (.format ld formatter)))         ; => "2019-08-23"
)
