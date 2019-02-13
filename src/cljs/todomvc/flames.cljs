(ns todomvc.flames
  (:require
    [todomvc.enflame :as flame]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(defn initialize []
  (flame/define-flame
    {:id              :current-page ; #todo move to an arg and add :id field and :tx-fnfn metadata
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:current-page app-state))})

  (flame/define-flame
    {:id              :usernames
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:usernames app-state))})

  (flame/define-flame
    {:id              :reg-state
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:reg-state app-state))})

  (flame/define-flame
    {:id              :display-mode
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:display-mode app-state))})

  (flame/define-flame
    {:id              :ajax-response
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:ajax-response app-state))})
  )
