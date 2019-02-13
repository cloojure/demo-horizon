(ns todomvc.app-state
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as rf]
            [todomvc.enflame :as flame]
            [tupelo.core :as t]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

; -- Default :app-state Value  ---------------------------------------------------
; When the application first starts, this will be the value put in :app-state
; Unless, of course, there are todos in the LocalStore (see further below)
; Look in:
;   1.  `core.cljs` for  "(dispatch-sync [:initialise-state])"
;   2.  `events.cljs` for the registration of :initialise-state handler
(def app-state-default     ; what gets put into :app-state by default.
  {:current-page :home-page })

; -- Local Storage  ----------------------------------------------------------
; Part of the todomvc challenge is to store todos in LocalStorage, and
; on app startup, reload the todos from when the program was last run.
; But the challenge stipulates to NOT load the setting for the "showing"
; filter. Just the todos.
(def js-localstore-key "calories-enflame") ; localstore key

; Part of the TodoMVC Challenge is to store todos in local storage. Here we define an interceptor to do this.
; read in todos from localstore, and process into a sorted map
(def localstore-load-intc ; injects ctx with todos from the localstore.
  (flame/interceptor
    ; #todo make missing :enter or :leave => identity like pedestal
    {:id    :localstore-load-intc
     ; #todo add wrapper checks on :enter/:leave interceptors to ensure returned value is a "context", i.e. has {:data/type :enflame/context}
     :enter (fn [ctx]
              (let [app-state-edn-str  (.getItem js/localStorage js-localstore-key)
                    app-state-loaded (some-> app-state-edn-str (cljs.reader/read-string))
                    >>       (println :local-store-load app-state-loaded)
                    ctx-out  (-> ctx
                               (t/glue {:app-state app-state-loaded})
                              ;(assoc :app-state app-state-default)
                               )]
                (t/spyx :load-result (flame/ctx-trim ctx-out))
                ctx-out))
     :leave identity}))

(def localstore-save-intc
  (flame/interceptor
    {:id    :localstore-save-intc
     :enter identity
     :leave (fn [ctx]
              (let [app-state-edn-str (pr-str (t/grab :app-state ctx))]
                (js/console.info :local-store-save app-state-edn-str)
                (.setItem js/localStorage js-localstore-key app-state-edn-str))
              ctx)}))


