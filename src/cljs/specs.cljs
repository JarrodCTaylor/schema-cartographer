(ns cljs.specs
  (:require
    [clojure.spec.alpha :as s]))

; region = Analytics ====================================================================
(s/def ::value (s/nilable string?))
(s/def ::dirty? boolean?)
(s/def ::required? boolean?)
(s/def ::has-error? boolean?)
(s/def ::error-message string?)
(s/def ::form-field (s/keys :req-un [::value ::dirty? ::required? ::has-error? ::error-message]))
(s/def ::form (s/map-of keyword? ::form-field))
; endregion
