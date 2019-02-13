;*************************************************************
;*************************************************************
;*****     REMEMBER TO ENTER EACH NAMESPACE TWICE!!!     *****
;*************************************************************
;*************************************************************

(ns tst.demo.doorunner
  (:require
    [doo.runner :refer-macros [doo-tests]]

    [tst.demo.bambam]
  ))

(enable-console-print!)
(println "doorunner - beginning")

(doo-tests
  'tst.demo.bambam

)
(println "doorunner - end")
