(ns todomvc.events
  (:require
    [oops.core :as oops]
    [todomvc.app-state :as app-state]
    [todomvc.enflame :as flame]
    [tupelo.core :as t]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

; #todo need plumatic schema and tsk/KeyMap
(defn set-display-mode
  "Saves current 'showing' mode (3 filter buttons at the bottom of the display)"
  [ctx [-e- new-filter-kw]] ; :- #{ :all, :active or :completed }
  (t/spyx :set-display-mode new-filter-kw)
  (let [all-filter-modes #{:all :active :completed}
        new-filter-kw    (if (contains? all-filter-modes new-filter-kw)
                           new-filter-kw
                           :all)]
    (assoc-in ctx [:app-state :display-mode] new-filter-kw)))

(defn add-todo [ctx [-e- todo-title]]
  (update-in ctx [:app-state :todos] ; #todo make this be (with-path ctx [:app-state :todos] ...) macro
    (fn [todos]     ; #todo kill this part
      ; must choose a new id greater than any existing id (possibly from localstore todos)
      (let [todo-ids (keys todos)
            new-id   (if (t/not-empty? todo-ids)
                       (inc (apply max todo-ids))
                       0)]
        (t/glue todos {new-id {:id new-id :title todo-title :completed false}})))))

(defn toggle-completed [ctx [-e- todo-id]]
  (update-in ctx [:app-state :todos todo-id :completed] not))

(defn update-title [ctx [-e- todo-id todo-title]]
  (assoc-in ctx [:app-state :todos todo-id :title] todo-title))

(defn delete-todo [ctx [-e- todo-id]]
  (t/dissoc-in ctx [:app-state :todos todo-id]))

(defn clear-completed-todos
  [ctx -event-]
  (let [todos         (t/fetch-in ctx [:app-state :todos])
        completed-ids (->> (vals todos) ; find id's for todos where (:completed -> true)
                        (filter :completed)
                        (mapv :id))
        todos-new     (reduce dissoc todos completed-ids) ; delete todos which are completed
        ctx-out        (assoc-in ctx [:app-state :todos] todos-new)]
    ctx-out))

; #todo make example using destruct/restruct
(defn toggle-completed-all
  "Toggles the completed status for each todo"
  [ctx -event-]
  (let [todos         (t/fetch-in ctx [:app-state :todos])
        new-completed (not-every? :completed (vals todos)) ; work out: toggle true or false?
        todos-new     (reduce #(assoc-in %1 [%2 :completed] new-completed)
                        todos
                        (keys todos))
        ctx-out        (assoc-in ctx [:app-state :todos] todos-new)]
    ctx-out))

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
                          (t/spy :initialize-app-state-enter (flame/ctx-trim ctx))
                          (let [ctx-out ctx
                                ;ctx-out (t/glue ctx {:app-state app-state/app-state-default})
                                ]
                            (t/spy :initialize-app-state-ret (flame/ctx-trim ctx-out))
                            ctx-out))})

  (flame/define-event ;  #todo => flame/define-event-handler  #awt
    {:event-id          :reset-db
     :interceptor-chain common-interceptors
     :handler-fn        (fn [ctx -event-] ; #todo => make an explicit interceptor?  #awt (:event-handler)
                          (let [ctx-out (t/glue ctx {:app-state app-state/app-state-default}) ]
                            ctx-out))})

  (flame/define-event
    {:event-id          :register-begin
     :interceptor-chain [flame/trace-print app-state/localstore-save-intc ]
     :handler-fn        (fn show-registration-page
                          [ctx -event-]
                          (let [ctx-out (assoc-in ctx [:app-state :current-page] :registration-page) ]
                            ctx-out))})
  (flame/define-event
    {:event-id          :register-name
     :interceptor-chain [ flame/trace-print app-state/localstore-save-intc ]
     :handler-fn        (fn register-name
                          [ctx event]
                          (let [[-evt- username] event
                                >>        (t/spyx :register-name event)
                                usernames (get-in ctx [:app-state :usernames] {})]
                            (if (contains? usernames username)
                              (do
                                (t/spyx :register-name :user-already-registered username)
                                (assoc-in ctx [:app-state :reg-state :user-already-registered] true))
                              (let [usernames-out (t/glue usernames {username {}})]
                                (t/spyx :register-name :adding-new-user username)
                                (let [ctx-out (t/it-> ctx
                                                (assoc-in it [:app-state :reg-state :user-already-registered] false)
                                                (assoc-in it [:app-state :usernames] usernames-out))]
                                  ctx-out)))))})



  (flame/define-event
    {:event-id          :add-todo
     :interceptor-chain common-interceptors
     :handler-fn        add-todo})

  (flame/define-event
    {:event-id          :toggle-completed
     :interceptor-chain common-interceptors
     :handler-fn        toggle-completed})

  (flame/define-event
    {:event-id          :update-title
     :interceptor-chain common-interceptors
     :handler-fn        update-title})

  (flame/define-event
    {:event-id          :delete-todo
     :interceptor-chain common-interceptors
     :handler-fn        delete-todo})

  (flame/define-event
    {:event-id          :clear-completed
     :interceptor-chain common-interceptors
     :handler-fn        clear-completed-todos})

  (flame/define-event
    {:event-id          :complete-all-toggle
     :interceptor-chain common-interceptors
     :handler-fn        toggle-completed-all})

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








