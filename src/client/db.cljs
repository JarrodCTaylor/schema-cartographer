(ns client.db
  (:require
    [clojure.spec.alpha :as s]
    [client.routes.index.db :as index]))

; region = Specs =====================================================================
(s/def ::active-route keyword?)
(s/def ::map-of-keywords-and-strs (s/every-kv keyword string?))

; region - Active Route --------------------------------------------------------------
(s/def ::name keyword?)
(s/def ::view vector?)
(s/def ::dispatch-on-entry vector?)
(s/def ::path-params ::map-of-keywords-and-strs)
(s/def ::query-params ::map-of-keywords-and-strs)
(s/def ::data (s/keys :req-un [::name ::view]
                      :opt-un [::dispatch-on-entry]))
(s/def ::active-route (s/keys :req-un [::data ::path-params ::query-params]))
; endregion

; region -- Route DBs ----------------------------------------------------------------
(s/def ::index ::index/index-db)
(s/def ::routes (s/keys :req-un [::index]))
; endregion

; region -- Alerts -------------------------------------------------------------------
(s/def ::message string?)
(s/def ::type string?)
(s/def ::alert (s/keys :req-un [::uuid ::message ::type]))
(s/def ::alerts (s/* ::alert))
; endregion
; endregion

; region = DB Spec ===================================================================
(s/def ::db (s/keys :req-un [::active-route
                             ::alerts
                             ::routes]))

(defonce default-db {;; = Top Level ============================================
                     :active-route {:path-params {}
                                    :query-params {}
                                    :data {:name :loading
                                           :view [:div "Loading..."]}}
                     :alerts '()
                     ;; = index Route ==========================================
                     :routes {:index index/index-db}})
; endregion

