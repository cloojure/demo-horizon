(ns demo.events
  (:require
    [oops.core :as oops]
    [demo.app-state :as app-state]
    [demo.enflame :as flame]
    [tupelo.core :as t]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(def common-interceptors
  [flame/trace-print
   app-state/localstore-save-intc
   flame/ajax-intc ])

; #todo idea: maybe move event into the ctx (like req/resp in http?)
(defn define-events []

  ; This event is dispatched when the app's `main` ns is loaded (demo.core). It establishes
  ; initial application state in the context map `:app-state` key. That means merging:
  ;   1. Any todos stored in the browser's LocalStore (from the last session of this app)
  ;   2. Default initial values
  (flame/define-event ;  #todo => flame/define-event-handler  #awt
    {:event-id          :initialize-app-state
     ; #todo make :event-id mandatory arg to flame/define-event like tped/definterceptor
     ; #todo add :event-id to handler-fn metadata
     :interceptor-chain [flame/trace-print app-state/localstore-load-intc]
     :handler-fn        (fn [ctx -event-] ; #todo => make an explicit interceptor?
                          (assoc-in ctx [:app-state] app-state/default-state))})
  ; #todo add event to ctx under :event (remove from handler args)
  ; #todo rename key to :event-handler-fn

  ; #todo make event mechanism check each interceptor & handler-fn for legal ctx on enter and leave

  (flame/define-event
    {:event-id          :lower-limit
     :interceptor-chain common-interceptors
     :handler-fn        (fn lower-limit-fn
                          [ctx event]
                          (let [[-evt- lower-limit] event]
                            (assoc-in ctx [:app-state :lower-limit] lower-limit)))})

  (flame/define-event
    {:event-id          :upper-limit
     :interceptor-chain common-interceptors
     :handler-fn        (fn upper-limit-fn
                          [ctx event] ; #todo  make :event a key in ctx, so this becomes a standard interceptor
                          (let [[-evt- upper-limit] event
                                upper-limit (min upper-limit app-state/upper-limit-hard) ]
                            (assoc-in ctx [:app-state :upper-limit] upper-limit)))})

  (flame/define-event
    {:event-id          :tgt-letter
     :interceptor-chain common-interceptors
     :handler-fn        (fn tgt-letter-fn
                          [ctx event]
                          (let [[-evt- tgt-letter] event]
                            (assoc-in ctx [:app-state :tgt-letter] tgt-letter)))})

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








