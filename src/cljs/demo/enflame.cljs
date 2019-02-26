(ns demo.enflame ; #todo => re-state ???
  (:require
    [ajax.core :as ajax]
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [re-frame.db :as rfdb]
    [re-frame.events :as rfe]
    [re-frame.interceptor :as rfi]
    [re-frame.loggers :as rflog]
    [re-frame.router :as rfr]
    [schema.core :as s]
    [tupelo.core :as t]
    ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

;---------------------------------------------------------------------------------------------------
; #todo (definterceptor my-intc  ; added to :id field as kw
; #todo   "doc string"
; #todo   {:enter (fn [ctx] ...)       to match pedestal
; #todo    :leave (fn [ctx] ...) } )
; #todo event handlers: document that `state` is coeffects/effects (ignore the difference)
;     coeffects  =>  state-in
;       effects  =>  state-out

; #todo   maybe rename interceptor chain to intc-chain, proc-chain, transform-chain

; #todo   unify [:dispatch ...] effect handlers
; #todo     {:do-effects [  ; <= always a vector param, else a single effect
; #todo        {:effect/id :eff-tag-1  :par1 1  :par2 2}
; #todo        {:effect/id :eff-tag-2  :effect/delay {:value 200  :unit :ms} ;
; #todo         :some-param "hello"  :another-param :italics } ] }

; #todo make all routes define an intc chain.

; #todo [:delete-item 42] => {:event/id :delete-item :idx 42}
; #todo   {:event/id :set-timer  :units :ms :value 50 :action (fn [] (js/alert "Expired!") }

; #todo (dispatch-event {:event/id <some-id> ...} )   => event map
; #todo (add-task state {:effect/id <some-id> ...} )  => updated state

; #todo setup, prep, resources, augments, ancillary, annex, ctx, info, data
; #todo environment, adornments, supplements

; #todo teardown, completion, tasks, commands, orders

;---------------------------------------------------------------------------------------------------
; #todo Need a way to document event names and args
; #todo    (defevent set-showing [ctx])
; #todo event handlers take only params-map (fn [params :- tsk/Map] ...)  ; params => {:state <state>   :event <event>}

; #TODO CHANGE ALL EVENTS to be maps => {:id :set-showing   :new-filter-kw :completed ...}
; #TODO CHANGE ALL HANDLERS to be (defn some-handler [ctx event]   (with-map-vals event [id new-filter-kw] ...)
;---------------------------------------------------------------------------------------------------

;---------------------------------------------------------------------------------------------------

; #todo protect with a regex to make more like java
(s/defn parse-int     :- s/Int ; #todo => tupelo.cljs.parse
  "( [str-val]
     [str-val :default default-val] )
   A thin wrapper around js/parseInt  Parses the string str-val into a integer.
   If the optional default-val is specified, it will be returned in the event of an
   Nan."
  ([str-val :- s/Str]
   (let [result (js/parseInt (str/trim str-val))]
     (when (js/isNaN result)
       (throw (ex-info "parse-int: could not parse input value" ;(t/vals->map str-val)
                {:str-val str-val} )))
     result))
  ([str-val default-val]
   (t/with-exception-default default-val
     (parse-int str-val))))

; #todo protect with a regex to make more like java
(s/defn parse-float :- s/Num ; #todo => tupelo.cljs.parse
  "( [str-val]
     [str-val :default default-val] )
   A thin wrapper around js/parseFloat.  Parses the string str-val into a float.
   If the optional default-val is specified, it will be returned in the event of an
   NaN."
  ([str-val :- s/Str ]
   (let [result (js/parseFloat (str/trim str-val))]
     (when (js/isNaN result)
       (throw (ex-info "parse-float: could not parse input value" ; (t/vals->map str-val)
                {:str-val str-val})))
     result))
  ([str-val default-val]
   (t/with-exception-default default-val
     (parse-float str-val))))


(defn event-value [event]  (-> event .-target .-value))

;---------------------------------------------------------------------------------------------------
(defonce ctx-trim-queue-stack (atom true))
(defn ctx-trim [ctx]
  "Removes `:queue` and `:stack` entries from the context map to declutter printing"
  (if @ctx-trim-queue-stack
    (dissoc ctx :queue :stack)
    ctx))

; #todo macro definterceptor (auto-set :id same as name)
(defn interceptor ; #todo need test
  "Creates a simple interceptor that accepts & returns state. Usage:

      (flame/interceptor { :id    :some-intc
                           :enter (fn [& args] ...)
                           :leave (fn [& args] ...) } )

  NOTE: enflame uses Pedestal-style `:enter` & `:leave` keys for the interceptor map.
  "
  [ctx]         ; #todo :- tsk/KeyMap
  {:id     (or (t/grab :id ctx) ::unnamed-intc)
   :before (t/grab :enter ctx)
   :after  (t/grab :leave ctx)})
; #todo allow one of :enter or :leave to be blank => identity
; #todo add :error key like pedestal?

(def event-dispatch-intc
  "Interceptor for managing event dispatch"
  (interceptor      ; #todo need test
    {:id    :dispatch-all-intc
     :enter identity
     :leave (fn dispatch-all-intc-leave
              [ctx]
              (t/with-result ctx
                ;(println :dispatch-all-intc :enter (ctx-trim ctx))
                (let [dispatch-cmd      (let [cmd (:dispatch ctx)]
                                          (if cmd [cmd] []))
                      dispatch-n-cmds   (get ctx :dispatch-n [])
                      dispatch-all-cmds (get ctx :dispatch-all [])
                      dispatch-cmds     (t/glue dispatch-cmd dispatch-n-cmds dispatch-all-cmds)]
                  (println :dispatch-all-intc :dispatch-cmds dispatch-cmds)
                  (doseq [dispatch-cmd dispatch-cmds]
                    (if-not (vector? dispatch-cmd)
                      (rflog/console :error "dispatch-all-intc: bad dispatch-cmd=" dispatch-cmd)
                      (rfr/dispatch dispatch-cmd))))
                (println :dispatch-all-intc :leave)
                ))}))

(def app-state-intc
  "Interceptor for managing app state"
  (interceptor
    {:id    :app-state-intc
     :enter (fn app-state-intc-enter
              [ctx]
             ;(println :app-state-intc-enter :enter (ctx-trim ctx))
              (let [ctx-out (-> ctx
                              (t/glue {:data/type :enflame/context
                                       :app-state @rfdb/app-db
                                       ; KLUDGE: Move `:event` from :coeffects sub-map to parent `context` map.
                                       ; KLUDGE:   Allows us to use unmodified re-frame as "hosting" lib
                                       :event     (t/fetch-in ctx [:coeffects :event])})
                              (dissoc :coeffects))]
                (t/with-result ctx-out
                 ;(println :app-state-intc-enter :end (ctx-trim ctx-out))
                )))
     :leave (fn app-state-intc-leave
              [ctx]
              (t/with-result ctx
                (let [app-state (t/grab :app-state ctx)]
                  (if-not (identical? @rfdb/app-db app-state)
                    (do
                     ;(t/spy :app-state-intc-leave "rfdb/app-db atom - resetting...")
                      (reset! rfdb/app-db app-state))
                    (do
                     ;(t/spy :app-state-intc-leave "rfdb/app-db atom unchanged")
                      )))
               ;(t/spy :app-state-intc-leave (ctx-trim ctx))
                ))}))

(def ajax-options-keys
  "Set of map keys accepted as options by cljs-ajax"
  #{:body :cookie-policy :error-handler :finally :format :handler :headers :params
    :progress-handler :response-format :timeout :url-params :with-credentials})

(def ajax-intc
  "Interceptor for performing AJAX requests"
  (interceptor
    {:id    :ajax-intc
     :enter identity
     :leave (fn [ctx]
              (t/with-result ctx
                (let [ajax (:ajax ctx)]
                  ;(t/spyx :ajax-intc-start ctx)
                  ;(t/spyx :ajax-intc-start ajax)
                  (when-not (nil? ajax)
                    (let [method            (t/grab :method ajax)
                          uri               (t/grab :uri ajax)
                          ajax-opts-present (set/intersection (set (keys ajax)) ajax-options-keys)
                          opts-map          (t/submap-by-keys ajax ajax-opts-present)]
                      ;(t/spy :ajax-intc-ready (t/vals->map method uri opts-map))
                      (condp = method
                        :get (ajax/GET uri opts-map)
                        :put (ajax/PUT uri opts-map)
                        :post (ajax/POST uri opts-map)
                        (throw (ex-info "ajax-intc: unrecognized :method" ajax))))))))}))


;---------------------------------------------------------------------------------------------------
; #todo need macro  (definterceptor todos-done {:name ...   :enter ...   :leave ...} )

(defn define-event
  "Defines the event handler given a context map with keys [:event-id :interceptor-chain :handler-fn]"
  [ctx]
  (t/with-map-vals ctx [event-id interceptor-chain handler-fn]
    (when-not (keyword? event-id) (throw (ex-info "illegal event-id" event-id)))
    (when-not (vector? interceptor-chain) (throw (ex-info "illegal interceptor-chain" interceptor-chain)))
    (when-not (every? map? interceptor-chain) (throw (ex-info "illegal interceptor" interceptor-chain))) ; #todo detail intc map
    (when-not (fn? handler-fn) (throw (ex-info "illegal handler-fn" handler-fn)))
    (let [handler-intc (interceptor
                         {:id    event-id
                          :enter (fn [ctx]
                                   (let [event   (t/grab :event ctx)
                                         ctx-out (handler-fn ctx event)]
                                     ctx-out))
                          :leave identity})]
      (rfe/register event-id
        [app-state-intc event-dispatch-intc interceptor-chain handler-intc]))))

; #todo need unregister-event
; #todo throw if receive non-registered event (flag to disable this?)

; #todo need plumatic schema:  event => [:kw-evt-name & args]
(defn dispatch-event
  [& args]
  (t/spy :dispatch-event-enter args)
  (apply rf/dispatch args)
  (t/spy :dispatch-event-leave))

(defn dispatch-event-sync
  [& args]
  (t/spy :dispatch-event-sync-enter args)
  (apply rf/dispatch-sync args)
  (t/spy :dispatch-event-sync-leave args))

;****************************************************************
; Define built-in :app-state reactive
(rf/reg-sub :app-state (fn [app-state -query-] app-state)) ; loaded from rfdb/app-db ratom
;****************************************************************

(defn watching [reactive-id] @(rf/subscribe reactive-id)) ; #todo was (listen ...)

; #todo macro to insert reactive as fn-name;  :sorted-todos => (fn sorted-todos-fn ...)
; #todo (flame/define-reactive! :sorted-todos ...) => (fn sorted-todos-fn ...)
(defn define-flame
  ; #todo reactive facet flame flare view vista vision scene snippet projection chunk flake shard splinter
  ; #todo slice fragment shatter sliver factor element flare beam ray glint ember glow
  "Defines a reactive view of global state given a context map with keys [:id :reactive-inputs :tx-fn]"
  [ctx]
  (t/with-map-vals ctx [id reactive-inputs tx-fn]
    (when-not (keyword? id) (throw (ex-info "id must be a keyword" id)))
    (when-not (vector? reactive-inputs) (throw (ex-info "reactive-inputs must be a vector" reactive-inputs)))
    (when-not (every? keyword? reactive-inputs) (throw (ex-info "reactive values must be keywords" reactive-inputs)))
    (when-not (fn? tx-fn) (throw (ex-info "tx-fn must be a function" tx-fn)))
    (let [sugar-forms (vec (apply concat
                             (for [curr-input reactive-inputs]
                               [:<- [curr-input]])))
          args-vec    (vec (concat [id] sugar-forms [tx-fn]))]
      (apply rf/reg-sub args-vec))))

; #todo need macro  (with-path state [:app-state :todos] ...) ; extract and replace in ctx

;---------------------------------------------------------------------------------------------------
; tracing interceptor (modified rfstd/debug

(def trace
  "An interceptor which logs/instruments an event handler's actions to
  `js/console.log`. See examples/demo/src/events.cljs for use.
  Output includes:
  1. the event vector
  2. orig app-state
  3. new app-state          "
  (interceptor
    {:id    :trace
     :enter (fn trace-enter
              [ctx]
              (t/with-result ctx
                (let [ctx (ctx-trim ctx)]
                  (rflog/console :log "Handling re-frame event:" (t/grab :event ctx))
                  (rflog/console :log :trace :enter ctx))))

     :leave (fn trace-leave
              [ctx]
              (t/with-result ctx
                (let [ctx (ctx-trim ctx)]
                  (rflog/console :log :trace :leave ctx))))}))

(def trace-print
  "An interceptor which logs/instruments an event handler's actions using `println`.
  See examples/demo/src/events.cljs for use.
  Output includes:
  1. the event vector
  2. orig app-state
  3. new app-state     "
  (interceptor
    {:id    :trace-print
     :enter (fn trace-print-enter
              [ctx]
              (t/with-result ctx
                (println :trace-print-enter :event (t/grab :event ctx))
                (println :trace-print-enter :ctx (ctx-trim ctx))))
     :leave (fn trace-print-leave
              [ctx]
              (t/with-result ctx
                (println :trace-print-leave :ctx (ctx-trim ctx))))}))

; #todo   => (event-handler-set!    :evt-name  (fn [& args] ...)) or subscribe-to  subscribe-to-event
; #todo   => (event-handler-clear!  :evt-name)
; #todo option for log wrapper (with-event-logging  logger-fn
; #todo                          (event-handler-set! :evt-name  (fn [& args] ...)))











