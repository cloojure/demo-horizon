(ns tst.demo.numbers
  (:require
    #?@(:clj [[demo.numbers :as num]
              [tupelo.test :as tst :refer [is= dotest dotest-focus throws? ]]])
    #?@(:cljs [[demo.numbers :as num :include-macros true]
               [tupelo.test-cljs :as tst]]))
  )

(dotest
  (println "test 1")
  (is= 2 (+ 1 1)))

(dotest
  (println "test 2")
  (is= 5 (num/add2 2 3)) ; this works
  (is= 3 (num/logr-numbers
           (inc 0)
           (inc 1)
           (inc 2))))

(dotest
  (throws? (num/number->word -1))
  (is= "zero"  (num/number->word 0))
  (is= "one"  (num/number->word 1))
  (is= "seven"  (num/number->word 7))
  (is= "ten"  (num/number->word 10))
  (is= "twelve"  (num/number->word 12))
  (is= "nineteen"  (num/number->word 19))

  (is= ""  (num/hundreds-frag 99))
  (is= "one-hundred"  (num/hundreds-frag 123))
  (is= "nine-hundred"  (num/hundreds-frag 923))
  )