(ns schema-cartographer.utils
  (:require
    [cognitect.anomalies :as anomalies]))

(def retryable-anomaly? #{::anomalies/busy
                          ::anomalies/unavailable
                          ::anomalies/interrupted})

(defn with-retry
  "Try fn, return result if successful. If fn throws a retryable anomaly
   then try again upto 20 times with linear backoff."
  [fn]
  (let [retryable? #(-> % ex-data ::anomalies/category retryable-anomaly?)
        backoff #(when (<= % 20) (* 200 %))]
    (loop [n 1]
      (let [[success? return-value] (try [true (fn)]
                                         (catch Exception e [false e]))]
        (if success?
          return-value
          (if-let [ms (and (retryable? return-value) (backoff n))]
            (do
              (println "Retryable anomaly encountered [" return-value "]. Retrying after" ms "ms....")
              (Thread/sleep ms)
              (recur (inc n)))
            (throw return-value)))))))
