(ns client.test-runner
  (:require
    ;; require all the namespaces that you want to test
    [client.index-test]
    [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 5000))

