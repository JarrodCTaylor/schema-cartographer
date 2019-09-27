(ns repl
    (:require [figwheel.main.api]))

(figwheel.main.api/start {:mode :serve} "dev")
(figwheel.main.api/cljs-repl "dev")
