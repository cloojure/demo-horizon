;*************************************************************
;*************************************************************
;*****     REMEMBER TO ENTER EACH NAMESPACE TWICE!!!     *****
;*************************************************************
;*************************************************************

(ns tst.flintstones.doorunner
  (:require
    [doo.runner :refer-macros [doo-tests]]

    [tst.flintstones.bambam]
  ))

(enable-console-print!)
(println "doorunner - beginning")

(doo-tests
  'tst.flintstones.bambam

)
(println "doorunner - end")
