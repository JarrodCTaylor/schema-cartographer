(ns client.test-resources.verbose-is-equal
  (:require
    [cljs.test :refer-macros [is]]))

(defn is=
  [expected actual]
  (is (= expected actual)
      (with-out-str (let [id (str (random-uuid))
                          [things-only-in-expected things-only-in-actual _] (clojure.data/diff expected actual)]
                      (when (some not-empty [things-only-in-expected things-only-in-actual])
                        (.group js/console (str "Test Id: " id))
                        (.group js/console "Actual Result")
                        (.log js/console actual)
                        (.groupEnd js/console "Actual Result"))
                      (when things-only-in-expected
                        (.group js/console "Actual Result Is Missing")
                        (.log js/console things-only-in-expected)
                        (.groupEnd js/console "Actual Result Is Missing"))
                      (when things-only-in-actual
                        (.group js/console "Actual Result Additionally Has")
                        (.log js/console things-only-in-actual)
                        (.groupEnd js/console "Actual Result Additionally Has"))
                      (.groupEnd js/console (str "Test Id: " id))
                      (println "ID: " id)))))
