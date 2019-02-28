(ns demo.flames
  (:require
    [demo.enflame :as flame]
    [demo.numbers :as num]
    [tupelo.core :as t]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(defn initialize []
  (flame/define-flame
    {:id              :upper-limit
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] ; #todo can we get rid of -query- here???
                        (:upper-limit app-state))})

  (flame/define-flame
    {:id              :lower-limit
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:lower-limit app-state))})

  (flame/define-flame
    {:id              :tgt-letter
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:tgt-letter app-state))})

  (flame/define-flame
    {:id              :stats
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-]
                        (t/with-exception-default {:num-total-letters "error" :num-tgt-letter "error" :prob "error"}
                          (t/with-map-vals app-state [lower-limit upper-limit tgt-letter]
                            (let [
                                  stats {:num-total-letters 666, :num-tgt-letter 42, :prob 3.14} ; #todo fix dummy val
                                  stats (num/letter-stats-num-words lower-limit upper-limit tgt-letter)
                                  ]
                              stats))))})

  (flame/define-flame
    {:id              :ajax-response
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-] (:ajax-response app-state))})
  )
