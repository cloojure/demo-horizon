(ns demo.test-cljs  ; *.cljs file makes macros available to normal CLJS code
  (:require-macros [demo.test-cljs]))

;*****************************************************************************
;
; IMPORTANT:  Need an empty `demo/test.cljs` file with (:require-macros ...) as a 
; "hook" so the compiler can find the `demo/test.clj[c]` file containing the macros.  
; This also allows user code to ignore difference between fn/macro in (:require ...) expression.
;
;*****************************************************************************
