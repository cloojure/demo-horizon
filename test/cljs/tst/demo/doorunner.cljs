;*************************************************************
;*************************************************************
;*****     REMEMBER TO ENTER EACH NAMESPACE TWICE!!!     *****
;*************************************************************
;*************************************************************

(ns tst.demo.doorunner
  (:require
    [doo.runner :refer-macros [doo-tests]]

    [tst.demo.numbers]
  ))

(enable-console-print!)
(println "doorunner - beginning")

(doo-tests
  'tst.demo.numbers

)
(println "doorunner - end")
