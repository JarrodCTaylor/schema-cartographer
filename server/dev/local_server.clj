(ns local-server
  (:require
    [org.httpkit.server :as httpkit]
    [server.handler :refer [app]]))

(def server (atom nil))

(defn -main
      ([] (-main "9876"))
      ([port]
       ;; run-server returns a function that stops the server
       (->> (httpkit/run-server app {:port     (Integer/parseInt port)
                                     :max-body 100000000
                                     :join     false})
            (reset! server))
       (println "server started on port:" port)))

(defn stop []
      (@server))

(comment
  (-main)

  (stop))

