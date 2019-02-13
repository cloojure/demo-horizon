(ns todomvc.components
  "These functions are all Reagent components"
  (:require
    [clojure.string :as str]
    [goog.string :as gstring]
    [oops.core :as oops]
    [reagent.core :as r]
    [todomvc.enflame :as flame]
    [tupelo.char :as char]
    [tupelo.core :as t]
    [tupelo.string :as ts]
    ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

; #todo => tupelo.char
(def nbsp-char  (gstring/unescapeEntities "&nbsp;")) ; get a char we can use in hiccup
(defn nbsp
  "Return a string of N non-breaking-space (NBSP) chars (default=1)."
  ([] (nbsp 1))
  ([N] (str/join (repeat N nbsp-char))))

(defn input-field
  [{:keys [title on-save on-stop]}] ; #todo -> (with-map-vals [title on-save on-stop] ...)
  (let [text-val (r/atom title) ; local state
        stop-fn  (fn []
                   (reset! text-val "")
                   (when on-stop (on-stop)))
        save-fn  (fn []
                   (on-save (-> @text-val str str/trim))
                   (stop-fn))]
    (fn [props]
      [:input
       (merge (dissoc props :on-save :on-stop :title)
         {:type        "text"
          :value       @text-val
          :auto-focus  true
          :on-blur     save-fn
          :on-change   #(reset! text-val (flame/event-value %))
          :on-key-down #(let [rcvd (.-which %)] ; KeyboardEvent property
                          (condp = rcvd
                            char/code-point-return (save-fn)
                            char/code-point-escape (stop-fn)
                            nil))})])))

(defn task-list-row []
  (let [editing (r/atom false)]
    (fn [todo-curr]
      (t/spy :task-list-row todo-curr)
      (let [{:keys [id completed title]} todo-curr]
        [:li.list-group-item
         ;{:class (cond->
         ;          "" completed (str " completed")
         ;          @editing (str " editing"))}
         [:div.row
          [:div.form-inline.col-xs-1
           [:input  ; .toggle
            {:type      :checkbox
             :checked   completed
             :on-change #(flame/dispatch-event [:toggle-completed id])}]]
          [:div.form-group.col-xs-10
           [:label {:on-double-click #(reset! editing true)} title]]
          [:div.col-xs-1
           [:button.btn.btn-xs
            {:on-click #(flame/dispatch-event [:delete-todo id])
             ;:style    {:float :right}
             } "X"]]
          (when @editing
            [input-field
             {:class   "edit"
              :title   title
              :on-save #(if (seq %)
                          (flame/dispatch-event [:update-title id %])
                          (flame/dispatch-event [:delete-todo id]))
              :on-stop #(reset! editing false)}])]]))))

(defn task-list []
  (let [visible-todos (flame/watching [:visible-todos :a :b])
        all-complete? (flame/watching [:all-complete?])]
    [:div.panel-body       ; #main
     [:input#toggle-all
      {:type      "checkbox" :checked   all-complete?
       :on-change #(flame/dispatch-event [:complete-all-toggle])}]
     [:label {:for "toggle-all"}
      "Mark all as complete"]
     [:ul.list-group           ; #todo-list
      (for [todo-curr visible-todos]
        ^{:key (:id todo-curr)} [task-list-row todo-curr])]])) ; delegate to task-list-row component

; These buttons will dispatch events that will cause browser navigation observed by History
; and propagated via secretary.
(defn footer-controls []
  (let [[num-active num-completed] (flame/watching [:footer-counts 1 2])
        display-mode (flame/watching [:display-mode])]
    [:div.panel-footer        ; #footer
     [:span ; #todo-count
      [:strong num-active]
      (ts/pluralize-with num-active " item") " left  (" display-mode ")"]
     [:span "----"]
     [:div.btn-group.btn-group-xs
      [:button.btn.btn-xs {:type     :button :id :all :class "filters"
                :on-click #(flame/dispatch-event [:set-display-mode :all])} "All"]
      [:button.btn.btn-xs {:type     :button :id :active
                :on-click #(flame/dispatch-event [:set-display-mode :active])} "Active"]
      [:button.btn.btn-xs {:type     :button :id :completed
                :on-click #(flame/dispatch-event [:set-display-mode :completed])} "Completed"] ]
     [:span "----"]
     [:div.btn-group.btn-group-xs
      (when (pos? num-completed)
        [:button.btn.btn-xs {:type :button ; :id      :completed
                     :on-click #(flame/dispatch-event [:clear-completed])} "Clear Completed"])]
     ]))

(defn task-entry []
  [:header          ; #header
   [:h1 "todos"]
   [input-field
    {:id          "new-todo"
     :placeholder "What needs to be done?"
     :on-save     #(when (t/not-empty? (str/trim %))
                     (flame/dispatch-event [:add-todo %]))}]])

(defn todo-root []
  [:div
   [:section#todoapp
    [task-entry]
    (when (t/not-empty? (flame/watching [:todos]))
      [task-list])
    [footer-controls]]
   ])

;---------------------------------------------------------------------------------------------------

(defn ajax-says []
  [:div
   [:span {:style {:color :darkgreen}} [:strong "AJAX says: "]]
   [:span {:style {:font-style :italic}} (nbsp 2) (flame/watching [:ajax-response])]])

(defn registration-page []
  (let [reg-state (flame/watching [:reg-state])]
    (t/spyx reg-state)
    [:div
     [:div
      [:label (str "Username:" (nbsp 2))]
      [input-field
       {:id          "user-name"
        :placeholder "Joe Smith"
        :on-save     #(when (t/not-empty? (str/trim %))
                        (flame/dispatch-event [:register-name %]))}]]
     [:div
      (if (:user-already-registered reg-state)
        [:label "Error - Username Already Registered"]
        [:span {:style {:font-style :italic}} "username available"])]]))

(defn root-comp []       ; was simple-component
  (let [app-state    (flame/watching [:app-state])
        current-page (flame/watching [:current-page])]
    [:div {:class "container"}
     [:hr]
     [:div
      [:h1 "Calorie Counter App"]
      [:hr]
      (println :root :app-state app-state)
      (condp = current-page
        :registration-page [registration-page]

        :home-page
        [:button {:on-click #(flame/dispatch-event [:register-begin])}
         "Register"]

        [:label (str "Unknown current-page:" (nbsp 2) current-page)])]
     [:hr]
     [ajax-says]
     [:hr]
     [:div
      [:button {:style    {:color :red}
                :on-click #(flame/dispatch-event [:reset-db])}
       "Reset DB"]]
     ]))








