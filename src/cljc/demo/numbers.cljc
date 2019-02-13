(ns demo.numbers
  (:require
    #?@(:clj [[tupelo.core :as t]])
    #?@(:cljs [[tupelo.core :as t :include-macros true]]))
  )

(defmacro logr-numbers
  [& body]
  `(do
     (println "logr-numbers-enter")
     (let [result# (do ~@body)]
       (println "logr-numbers-leave" result#)
       result#)))

(defn add2 [x y] (+ x y))

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
   4 "fourty"
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

(defn number->word
  [num]
  (when-not (<= MIN-VALUE num MAX-VALUE)
    (throw (ex-info "number out of range" (t/vals->map MIN-VALUE MAX-VALUE num))))
  (t/spyx
    (cond
      (zero? num) zero-word
      (<= 1 num 9) (t/fetch digit-words num)
      (<= 10 num 19) (t/fetch teen-words num)
      (<= 20 num 99) (t/fetch teen-words num)

      ))
  )
