(ns demo.components
  "These functions are all Reagent components"
  (:require
    [clojure.string :as str]
    [goog.string :as gstring]
    [oops.core :as oops]
    [reagent.core :as r]
    [demo.enflame :as flame]
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







