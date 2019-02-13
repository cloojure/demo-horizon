(ns demo.flames
  (:require
    [demo.enflame :as flame]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(defn initialize []
  (flame/define-flame
    {:id              :current-page ; #todo move to an arg and add :id field and :tx-fnfn metadata
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:current-page app-state))})

  (flame/define-flame
    {:id              :upper-limit
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:upper-limit app-state))})

  (flame/define-flame
    {:id              :ajax-response
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:ajax-response app-state))})
  )
