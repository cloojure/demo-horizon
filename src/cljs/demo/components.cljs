(ns demo.components
  "These functions are all Reagent components"
  (:require
    [clojure.string :as str]
    [goog.string :as gstring]
    [oops.core :as oops]
    [reagent.core :as r]
    [demo.app-state :as state]
    [demo.enflame :as flame]
    [tupelo.char :as char]
    [tupelo.core :as t]
    [tupelo.parse :as parse]
    [tupelo.string :as ts]
    [schema.core :as s]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(defn evt->val [event]  (-> event .-target .-value))

; #todo => tupelo.cljs.key-value-string
(def kvs-enter "Enter")
(def kvs-tab "Tab")
(def kvs-escape "Escape")

(defn str-keep-left ; #todo => tupelo.string
  "Keeps the N left-most chars of a string"
  [str-val n]
  (str/join (take n (t/str->chars str-val))))

(defn str-keep-right ; #todo => tupelo.string
  "Keeps the N right-most chars of a string"
  [str-val n]
  (str/join (take-last n (t/str->chars str-val))))

(defn char->code-point ; #todo => tupelo.string
  "REturns the code-point of a character (len=1 string)"
  [char-val]
  (t/validate #(= 1 (count %)) char-val)
  (.codePointAt char-val 0))

(s/defn code-point->char ; #todo => tupelo.string
  "REturns the code-point of a character (len=1 string)"
  [code-point :- s/Int]
  (.fromCodePoint js/String code-point))

(defn input-text
  "Create an HTML <input> element. `opts` are clojure options that control the behavior of this
  Reagent component.  `attrs` are HTML element attributes. Usage:

    [input-text <options-map>]

  Sample Options: {:init-val (str lower-limit)
                   :max-len  3
                   :save-fn  (fn [str-arg]
                               (let [limit (parse/parse-int str-arg nil)]
                                 (when-not (nil? limit)
                                   (flame/dispatch-event [:lower-limit limit]))))
                   :abort-fn (fn [str-arg] (println :aborting))
                   :attrs    {:id         :lower-limit
                              :auto-focus true}}
"
  [opts]
  ; set up component local state
  (let [text-val (r/atom (str (:init-val opts)))] ; local state
    (fn [opts]
      (let [{:keys [save-fn abort-fn coercion-fn max-len ]} opts
            coercion-fn (or coercion-fn identity)
            do-save   (fn []
                        (swap! text-val #(str/trim (str (coercion-fn %))))
                        (save-fn @text-val))
            do-abort  (fn []
                        (reset! text-val "")
                        (when abort-fn (abort-fn)))
            attrs-dyn {; #todo need to add a validator fn (turn red if hit <ret> with bad value)
                       :type        "text"
                       :value       @text-val
                       :max-length  nil
                       :on-blur     do-save
                       :on-change   (fn [evt]
                                      (let [evt-str       (t/validate string? (evt->val evt))
                                            text-val-next (t/cond-it-> (str/trim evt-str)
                                                            (t/not-nil? max-len) (str-keep-right it max-len) )]
                                        (t/spy :on-change [evt-str text-val-next])
                                        (reset! text-val text-val-next)))
                       :on-key-down (fn [kbe] ; KeyboardEvent
                                      (let [key-value-str (.-key kbe)]
                                        (t/spy :on-key-down-rcvd key-value-str)
                                        (t/spy :on-key-down-value text-val)
                                        (cond
                                          (= key-value-str kvs-enter) (do-save)
                                          (= key-value-str kvs-tab) (do-save)
                                          (= key-value-str kvs-escape) (do-abort))))
                       :on-key-up   (fn [kbe] ; KeyboardEvent
                                      (t/spy :on-key-up-value text-val))}]
        [:input (into (:attrs opts) attrs-dyn)]))))

;---------------------------------------------------------------------------------------------------

;(defn ajax-says []
;  [:div
;   [:span {:style {:color :darkgreen}} [:strong "AJAX says: "]]
;   [:span {:style {:font-style :italic}} (char/nbsp 2) (flame/watching [:ajax-response])]])

(defn range-coercion-fn
  [str-arg]
  (t/cond-it-> (parse/parse-int str-arg nil)
    (t/not-nil? it) (-> it
                      (max state/lower-limit-hard)
                      (min state/upper-limit-hard)
                      (str))))
(defn lowercase-coercion-fn [arg] (str/lower-case arg))

(defn root-comp []
  (let [lower-limit (flame/watching [:lower-limit])
        upper-limit (flame/watching [:upper-limit])
        tgt-letter  (flame/watching [:tgt-letter])
        stats       (flame/watching [:stats])]
    [:div {:class "container"}
     [:hr]
     [:div
      [:h1 "Numbers to Strings Challenge"]
      [:hr]
      [:div
       [:div
        [:label (str "Lower Limit:" (char/nbsp 2))]
        [input-text {:init-val    (str lower-limit)
                     :coercion-fn range-coercion-fn
                     :save-fn     (fn [str-arg]
                                    (let [int-val (parse/parse-int str-arg nil)]
                                      (when-not (nil? int-val)
                                        (let [int-val (max int-val state/lower-limit-hard)]
                                          (flame/dispatch-event [:lower-limit int-val])))))
                     :attrs       {;:placeholder lower-limit
                                   :id         :lower-limit
                                   :auto-focus true}}]]
       [:div
        [:label (str "Upper Limit:" (char/nbsp 2))]
        [input-text {:init-val    (str upper-limit)
                     :coercion-fn range-coercion-fn
                     :save-fn     (fn [str-arg]
                                    (let [int-val (parse/parse-int str-arg nil)]
                                      (when-not (nil? int-val)
                                        (flame/dispatch-event [:upper-limit int-val]))))
                     :attrs       {:id :upper-limit}}]]
       [:div
        [:label (str "Target Letter:" (char/nbsp 2))]
        [input-text {:init-val (str tgt-letter)
                     :coercion-fn lowercase-coercion-fn
                     :max-len  1
                     :save-fn  (fn [arg]
                                 (let [letter (str/trim arg)]
                                   (when (and (= 1 (count letter))
                                           (char/alpha? letter))
                                     (flame/dispatch-event [:tgt-letter letter]))))
                     :attrs    {:id :tgt-letter}}]]
       [:hr]
       [:div [:label (str "Results")]]
       [:div (char/nbsp 2)]
       [:div
        [:span
         [:label (str "Num Target Letter:" (char/nbsp 4))]
         [:label (t/grab :num-tgt-letter stats)]]]
       [:div
        [:span
         [:label (str "Total Target Letters:" (char/nbsp 2))]
         [:label (t/grab :num-total-letters stats)]]]
       [:div
        [:span
         [:label (str "Target Letter Prob:" (char/nbsp 4))]
         [:label (t/grab :prob stats)]]]
       ]]
     [:hr]
     ;[ajax-says]
     ;[:hr]

     ]))








