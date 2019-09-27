(ns server.unit.route-functions.analytics.get-ident-stats-test
  (:require
    [server.route-functions.analytics.get-ident-stats :as sut]
    [clojure.instant :as inst]
    [clojure.test :refer [deftest testing is]]))

(defn sample-insts [years months days]
  (for [year years
        month months
        day days]
    (inst/read-instant-date (str year "-" month "-" day "T00:00:00Z"))))

(deftest test-sparkline-data-less-than-32-unique-days
  (let [expected-response [{:date "08/10/19" :value 1}
                           {:date "08/11/19" :value 1}
                           {:date "08/12/19" :value 1}
                           {:date "08/13/19" :value 1}
                           {:date "08/14/19" :value 1}
                           {:date "08/15/19" :value 3}
                           {:date "08/16/19" :value 1}
                           {:date "08/17/19" :value 1}
                           {:date "08/18/19" :value 1}
                           {:date "08/19/19" :value 1}
                           {:date "08/20/19" :value 4}
                           {:date "08/21/19" :value 1}
                           {:date "08/22/19" :value 1}
                           {:date "08/23/19" :value 1}
                           {:date "08/24/19" :value 1}]
        instances (sample-insts ["2019"] ["08"] (range 10 25))
        actual-response (sut/sparkline-data (into instances [#inst "2019-08-15T13:29:20.942-00:00"
                                                             #inst "2019-08-15T13:29:20.942-00:00"
                                                             #inst "2019-08-20T13:29:20.942-00:00"
                                                             #inst "2019-08-20T13:29:20.942-00:00"
                                                             #inst "2019-08-20T13:29:20.942-00:00"]))]
    (is (= expected-response actual-response))))

(deftest test-sparkline-data-more-than-32-unique-days-less-than-32-unique-months
  (let [expected-response [{:date "08/19" :value 15}
                           {:date "09/19" :value 15}
                           {:date "10/19" :value 15}]
        instances (sample-insts ["2019"] ["08" "09" "10"] (range 10 25))
        actual-response (sut/sparkline-data instances)]
    (is (= expected-response actual-response))))

(deftest test-sparkline-data-more-than-32-unique-months
  (let [expected-response [{:date "2019" :value 180}
                           {:date "2020" :value 180}
                           {:date "2021" :value 180}]
        instances (sample-insts ["2019" "2020" "2021"]
                                ["01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12"]
                                (range 10 25))
        actual-response (sut/sparkline-data instances)]
    (is (= expected-response actual-response))))
