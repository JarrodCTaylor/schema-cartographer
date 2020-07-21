(ns client.config)

;; DEBUG is special. It is always defined but we override it
(def debug? ^boolean goog.DEBUG)

(goog-define alert-timeout-ms 3000)
(goog-define ci false)
