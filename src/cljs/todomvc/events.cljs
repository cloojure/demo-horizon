(ns todomvc.events
  (:require
    [oops.core :as oops]
    [todomvc.app-state :as app-state]
    [todomvc.enflame :as flame]
    [tupelo.core :as t]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(def common-interceptors
  [flame/trace-print
   app-state/localstore-save-intc
   flame/ajax-intc ])

; #todo idea: maybe move event into the ctx (like req/resp in http?)
(defn register-handlers []

  ; This event is dispatched when the app's `main` ns is loaded (todomvc.core). It establishes
  ; initial application state in the context map `:app-state` key. That means merging:
  ;   1. Any todos stored in the browser's LocalStore (from the last session of this app)
  ;   2. Default initial values
  (flame/define-event ;  #todo => flame/define-event-handler  #awt
    {:event-id          :initialize-app-state
     ; #todo make :event-id mandatory arg to flame/define-event like tped/definterceptor
     ; #todo add :event-id to handler-fn metadata
     :interceptor-chain [flame/trace-print app-state/localstore-load-intc]
     :handler-fn        (fn [ctx -event-] ; #todo => make an explicit interceptor?  #awt (:event-handler)
                          ctx)}) ; noop

  (flame/define-event ;  #todo => flame/define-event-handler  #awt
    {:event-id          :reset-db
     :interceptor-chain common-interceptors
     :handler-fn        (fn [ctx -event-] ; #todo => make an explicit interceptor?  #awt (:event-handler)
                          (let [ctx-out (t/glue ctx {:app-state app-state/app-state-default}) ]
                            ctx-out))})

  ; #todo make event mechanism check each interceptor & handler-fn for legal ctx on enter and leave
  (flame/define-event
    {:event-id          :register-begin
     :interceptor-chain [flame/trace-print app-state/localstore-save-intc ]
     :handler-fn        (fn show-registration-page
                          [ctx -event-]
                          (assoc-in ctx [:app-state :current-page] :registration-page) )})
  (flame/define-event
    {:event-id          :register-name
     :interceptor-chain [ flame/trace-print app-state/localstore-save-intc ]
     :handler-fn        (fn register-name
                          [ctx event]
                          (let [[-evt- username] event
                               ;>>        (t/spyx :register-name event)
                                usernames (get-in ctx [:app-state :usernames] {})]
                            (if (contains? usernames username)
                              (do
                               ;(t/spyx :register-name :user-already-registered username)
                                (assoc-in ctx [:app-state :reg-state :user-already-registered] true))
                              (let [usernames-out (t/glue usernames {username {}})]
                               ;(t/spyx :register-name :adding-new-user username)
                                (let [ctx-out (t/it-> ctx
                                                (assoc-in it [:app-state :reg-state :user-already-registered] false)
                                                (assoc-in it [:app-state :usernames] usernames-out))]
                                  ctx-out)))))})

  (flame/define-event
    {:event-id          :ajax-demo
     :interceptor-chain common-interceptors
     :handler-fn        (fn [ctx [-e- method uri opts]]
                          (t/glue ctx {:ajax (t/glue {:method method :uri uri} opts)}))})

  (flame/define-event
    {:event-id          :ajax-response
     :interceptor-chain common-interceptors
     :handler-fn        (fn [ctx [-e- response]]
                          (assoc-in ctx [:app-state :ajax-response] response))})

)








