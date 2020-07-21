(ns client.test-resources.ci-globals
  (:require
    ["node-localstorage" :refer (LocalStorage)]))

(defn global-stubs []
  (set! (.-localStorage js/global) (LocalStorage "./scratch"))
  (set! (.-document js/global) (clj->js {:getElementsByClassName (fn [_] [])})))
