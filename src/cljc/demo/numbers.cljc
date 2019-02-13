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

(def MIN-VALUE 1)
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
  {1 "ten"
   2 "twenty"
   3 "thirty"
   4 "fourty"
   5 "fifty"
   6 "sixty"
   7 "seventy"
   8 "eighty"
   9 "ninety"})

(def hundreds-suffix "hundred")

(defn number->word
  [num]
  (when-not (<= MIN-VALUE num MAX-VALUE)
    (throw (ex-info "number out of range" (t/vals->map MIN-VALUE MAX-VALUE num))))
  (cond
    (zero? num) zero-word

    )
  )
