(ns tst.demo.numbers
  (:require
    #?@(:clj [[demo.numbers :as numbers]
              [tupelo.test :as tst]])
    #?@(:cljs [[demo.numbers :as numbers :include-macros true]
               [tupelo.test-cljs :as tst]]))
  )

(tst/dotest
  (println "test 1")
  (tst/is= 2 (+ 1 1)))

(tst/dotest
  (println "test 2")
  (tst/is= 95 (numbers/add2 2 3)) ; this works
  (tst/is= 3 (numbers/logr-numbers
           (inc 0)
           (inc 1)
           (inc 2))))

(tst/dotest
  (tst/throws? (numbers/number->word -1))
  )