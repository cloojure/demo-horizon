(ns demo.core
  (:require
   ;[bidi.bidi :as bidi]
    [demo.components :as components]
    [demo.enflame :as flame]
    [demo.events :as events]
    [demo.flames :as flames]
    [devtools.core :as devtools]
    [goog.events]
    [reagent.core :as r]
    [tupelo.core :as t]
  )
  (:import [goog.history Html5History EventType]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)
(println
"This text is printed from src/demo/core.cljs.
Go ahead and edit it and see reloading in action. Again, or not.")
(println "Hello World! " )
(t/spyx :something (+ 2 3) [1 2 3])

;---------------------------------------------------------------------------------------------------
(defn ajax-handler [response]
  (.log js/console (str "cljs-ajax: successfully read:  " response))
  (flame/dispatch-event [:ajax-response response]) )

(defn ajax-error-handler [{:keys [status status-text]}]
  (js/console.log (str "cljs-ajax: something bad happened:  " status " " status-text)))

; -- Debugging aids ----------------------------------------------------------
(devtools/install!) ; we love https://github.com/binaryage/cljs-devtools

; #todo  make an `event` type & factory fn: (event :set-showing :all) instead of bare vec:  [:set-showing :all]
; #todo fix secretary (-> bidi?) to avoid dup (:filter x2) and make more like pedestal

;---------------------------------------------------------------------------------------------------

(defonce figwheel-reload-count (atom 0))
(defn figwheel-reload   ; called from project.clj -> :cljsbuild -> :figwheel -> :on-jsload
  "This fn is called automatically by Figwheel whenever you change a file."
  []
  (println "figwheel-reload enter => " @figwheel-reload-count)
  (swap! figwheel-reload-count inc)

  ; must call both of these on each page reload since the contents are not top-level-def's
  (events/event-config)
  (flames/flame-config)

  (println "figwheel-reload leave => " @figwheel-reload-count))

(defn app-start
  "Initiates the cljs application. Called upon page load/reload."
  []
  (println "**********  app-start - enter")
  (figwheel-reload) ; define flames and events

  ; Put an initial value into :app-state.
  ; Using the sync version of dispatch means that value is in place before we go onto the next step.
 ;(t/spy :initialize-app-state :before )
  (flame/dispatch-event-sync [:initialize-app-state])
 ;(t/spy :initialize-app-state :after )
  ; #todo remove this - make a built-in :init that every event-handler verifies & waits for (top priority)
  ; #todo add concept of priority to event dispatch

  (flame/dispatch-event [:ajax-demo :get "/fox.txt" {:handler       ajax-handler
                                                     :error-handler ajax-error-handler}])

  (r/render [components/root-comp] (js/document.getElementById "tgt-div"))
  (println "**********  app-start - leave")
)

;***************************************************************************************************
; kick off the app when this file is loaded
(app-start)



