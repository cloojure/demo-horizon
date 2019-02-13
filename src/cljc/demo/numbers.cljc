(ns demo.numbers
  (:require
    [clojure.string :as str]
    #?@(:clj [[tupelo.core :as t]])
    #?@(:cljs [[tupelo.core :as t :include-macros true]]))
  )

(def MIN-VALUE 0)
(def MAX-VALUE 999)

(def zero-word "zero")

(def digit-words
  {0 ""
   1 "one"
   2 "two"
   3 "three"
   4 "four"
   5 "five"
   6 "six"
   7 "seven"
   8 "eight"
   9 "nine" } )

(def teen-words
  {10 "ten"
   11 "eleven"
   12 "twelve"
   13 "thirteen"
   14 "fourteen"
   15 "fifteen"
   16 "sixteen"
   17 "seventeen"
   18 "eighteen"
   19 "nineteen"})

(def tens-words
  {2 "twenty"
   3 "thirty"
   4 "forty"
   5 "fifty"
   6 "sixty"
   7 "seventy"
   8 "eighty"
   9 "ninety"})

(def hundreds-suffix "hundred")

(defn digit-word
  [num]
  (when-not (<= 1 num 9)
    (throw (ex-info "digit-word: number out of range" (t/vals->map num))))
  (t/fetch digit-words num))

(defn hundreds-frag
  [num]
  (let [hundreds (quot num 100)
        result   (if (zero? hundreds)
                   ""
                   (str (digit-word hundreds) "-" hundreds-suffix))]
    result))

(defn two-digit-frag
  [num]
  (when-not (<= 1 num 99)
    (throw (ex-info "digit-word: number out of range" (t/vals->map num))))
  (cond
    (<= 1 num 9) (t/fetch digit-words num)
    (<= 10 num 19) (t/fetch teen-words num)
    (<= 20 num 99) (let [tens-prefix (t/fetch tens-words (quot num 10))
                         digit-word  (t/fetch digit-words (mod num 10))
                         result      (str tens-prefix
                                       (if (pos? (count digit-word))
                                         (str "-" digit-word)
                                         ""))]
                     result)
    :else (throw (ex-info "value out of range" (t/vals->map num)))))

(defn number->text
  [num]
  (when-not (<= MIN-VALUE num MAX-VALUE)
    (throw (ex-info "number out of range" (t/vals->map MIN-VALUE MAX-VALUE num))))
  (cond
    (zero? num) zero-word
    :else (let [pow-01-str (two-digit-frag (mod num 100))
                pow-2-str  (hundreds-frag num)
                result     (str/trim (str pow-2-str " " pow-01-str)) ]
            result)))

(defn number->text-squash
  [num]
  (t/it-> num
    (number->text it)
    (t/str->chars it)
    (t/drop-if #(contains? #{\- \space} %) it)
    (str/join it)) )

(defn number-words-squash
  "Given the min & max of a number range (inclusive) between 0 and 999, returns a vector
  containing the squashed text for each number (eg 123 => 'onehundredtwentythree')"
  [num-min num-max]
  (when-not (<= num-min num-max)
    (throw (ex-info "bounds out of order" (t/vals->map num-min num-max))))
  (when-not (<= MIN-VALUE num-min num-max MAX-VALUE)
    (throw (ex-info "number out of range" (t/vals->map MIN-VALUE MAX-VALUE num-min num-max))))
  (str/join (for [num (t/thru num-min num-max)]
              (number->text-squash num))))

(defn letter-freqs-num-words
  "Returns a clojure frequencies map for number-words in the specified range (inclusive)."
  [num-min num-max]
  (let [freqs (frequencies (number-words-squash num-min num-max))]
    freqs))

(defn letter-stats-num-words
  "Given a letter and the min & max of a number range (inclusive) between 0 and 999, returns the
  probability of picking that letter at random from all the text representation of numbers
  in that range, plus other stats."
  [num-min num-max tgt-letter]
  (let [freqs             (letter-freqs-num-words num-min num-max)
        num-total-letters (apply + (vals freqs))
        num-tgt-letter    (get freqs tgt-letter 0) ;default to zero if not found
        prob              (/ (double num-tgt-letter) (double num-total-letters))
        result            (t/vals->map num-total-letters num-tgt-letter prob)]
    result))

(defn letter-prob-num-words
  "Given a letter and the min & max of a number range (inclusive) between 0 and 999, returns the
  probability of picking that letter at random from all the text representation of numbers
  in that range."
  [num-min num-max tgt-letter]
  (t/grab :prob (letter-stats-num-words num-min num-max tgt-letter)))




















