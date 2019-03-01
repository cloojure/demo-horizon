(ns demo.flames
  (:require
    [demo.enflame :as flame]
    [demo.numbers :as num]
    [tupelo.core :as t]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(defn define-flames []
  (flame/define-flame
    {:id        :lower-limit
     :parents   [:app-state] ; #todo can we get rid of -query- here???
     :transform (fn [app-state -query-] (:lower-limit app-state))})
  ; #todo Rethink how these fn's are defined.  Need both app-state & query?
  ; #todo write blog on -query- naming pattern for unused params (not `_` !!!)

  (flame/define-flame
    {:id        :upper-limit
     :parents   [:app-state]
     :transform (fn [app-state -query-]
                  (:upper-limit app-state))})

  (flame/define-flame
    {:id        :tgt-letter
     :parents   [:app-state]
     :transform (fn [app-state -query-] (:tgt-letter app-state))})

  (flame/define-flame
    {:id        :stats
     :parents   [:app-state]
     :transform (fn [app-state -query-]
                  (t/with-exception-default {:num-total-letters "error"
                                             :num-tgt-letter    "error"
                                             :prob              "error"}
                    (t/with-map-vals app-state [lower-limit upper-limit tgt-letter]
                      (let [stats (num/letter-stats-num-words lower-limit upper-limit tgt-letter)]
                        stats))))})

  (flame/define-flame
    {:id        :ajax-response
     :parents   [:app-state]
     :transform (fn [app-state -query-] (:ajax-response app-state))})
  )
