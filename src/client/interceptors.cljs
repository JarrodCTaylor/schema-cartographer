(ns client.interceptors
  (:require
    [clojure.spec.alpha :as s]
    [expound.alpha :as expound]
    [client.db :as db]
    [re-frame.core :as rf]))

(defn check-and-throw
      "Every time we update the db check if the spec is valid if not throw exception"
      [db]
      (when-not (s/valid? ::db/db db)
                (throw (ex-info (str "spec check failed: " (expound/expound ::db/db db)) {}))))

;; = Additional Interceptors Can Be Added To The Vector As Needed
(def standard-interceptors [(when ^boolean goog.DEBUG (rf/after check-and-throw))])
