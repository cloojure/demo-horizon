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
  [{:keys [value on-save on-stop]}] ; #todo -> (with-map-vals [title on-save on-stop] ...)
  (let [text-val (r/atom value) ; local state
        stop-fn  (fn []
                   (reset! text-val "")
                   (when on-stop (on-stop)))
        save-fn  (fn []
                   (on-save (-> @text-val str str/trim ))
                   ; #todo parse into integer or replace with default (warning msg?)
                  ;(stop-fn)
                   )]
    (fn [props]
      [:input
       (merge (dissoc props :on-save :on-stop :value)
         {:type        "text"
          :value       @text-val
          :auto-focus  true
          :on-blur     save-fn
          :on-change   #(reset! text-val (flame/event-value %))
          :on-key-down #(let [rcvd (.-which %)] ; KeyboardEvent property
                          (condp = rcvd
                            char/code-point-return (save-fn)
                            char/code-point-tab (save-fn)
                            char/code-point-escape (stop-fn)
                            nil))})])))

;---------------------------------------------------------------------------------------------------

;(defn ajax-says []
;  [:div
;   [:span {:style {:color :darkgreen}} [:strong "AJAX says: "]]
;   [:span {:style {:font-style :italic}} (nbsp 2) (flame/watching [:ajax-response])]])

(defn root-comp []
  (let [upper-limit (flame/watching [:upper-limit])
        lower-limit (flame/watching [:lower-limit])
        tgt-letter  (flame/watching [:tgt-letter])
        stats       (flame/watching [:stats])]
    [:div {:class "container"}
     [:hr]
     [:div
      [:h1 "Numbers to Strings Challenge"]
      [:hr]
      [:div
       [:div
        [:label (str "Lower Limit:" (nbsp 2))]
        [input-field
         {:id      :lower-limit
          ;:placeholder lower-limit
          :value   (str lower-limit)
          :on-save (fn [str-arg]
                     (let [limit (flame/parse-int str-arg nil)]
                       (when-not (nil? limit)
                         (flame/dispatch-event [:lower-limit limit]))))}]]
       [:div
        [:label (str "Upper Limit:" (nbsp 2))]
        [input-field
         {:id      :upper-limit
          :value   (str upper-limit)
          :on-save (fn [str-arg]
                     (let [limit (flame/parse-int str-arg nil)]
                       (when-not (nil? limit)
                         (flame/dispatch-event [:upper-limit limit]))))}]]
       [:div
        [:label (str "Target Letter:" (nbsp 2))]
        [input-field
         {:id      :tgt-letter
          :value   (str tgt-letter)
          :on-save (fn [arg]
                     (let [letter (str/trim arg)]
                       (when (and (= 1 (count letter))
                               (char/alpha? letter))
                         (flame/dispatch-event [:tgt-letter letter]))))}]]
       [:hr]
       [:div [:label (str "Results")]]
       [:div (nbsp 2)]
       [:div
        [:span
         [:label (str "Num Target Letter:" (nbsp 4))]
         [:label (t/grab :num-tgt-letter stats)]]]
       [:div
        [:span
         [:label (str "Total Target Letters:" (nbsp 2))]
         [:label (t/grab :num-total-letters stats)]]]
       [:div
        [:span
         [:label (str "Target Letter Prob:" (nbsp 4))]
         [:label (t/grab :prob stats)]]]
       ]]
     [:hr]
     ;[ajax-says]
     ;[:hr]

     ]))








