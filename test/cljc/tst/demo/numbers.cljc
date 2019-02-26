(ns tst.demo.numbers
  (:require
    [tupelo.core :as t]
    [demo.enflame :as flame]
    #?@(:clj [[demo.numbers :as num]
              [tupelo.test :as tst :refer [is is= dotest dotest-focus throws? ]]])
    #?@(:cljs [[demo.numbers :as num :include-macros true]
               [tupelo.test-cljs :as tst   :refer [is= dotest throws? ]]]))
)

(dotest
  (is= (num/two-digit-frag 1) "one")
  (is= (num/two-digit-frag 7) "seven")
  (is= (num/two-digit-frag 10) "ten")
  (is= (num/two-digit-frag 12) "twelve")
  (is= (num/two-digit-frag 19) "nineteen")

  (is= (num/two-digit-frag 20) "twenty")
  (is= (num/two-digit-frag 21) "twenty-one")
  (is= (num/two-digit-frag 30) "thirty")
  (is= (num/two-digit-frag 39) "thirty-nine")
  (is= (num/two-digit-frag 99) "ninety-nine")

  (is= (num/hundreds-frag 99) "")
  (is= (num/hundreds-frag 123) "one-hundred")
  (is= (num/hundreds-frag 923) "nine-hundred"))

(dotest
  (throws? (num/number->text -1))
  (is= (num/number->text 0) "zero")

  (is= (num/number->text 1) "one")
  (is= (num/number->text 7) "seven")
  (is= (num/number->text 10) "ten")
  (is= (num/number->text 12) "twelve")
  (is= (num/number->text 19) "nineteen")

  (is= (num/number->text 20) "twenty")
  (is= (num/number->text 21) "twenty-one")
  (is= (num/number->text 30) "thirty")
  (is= (num/number->text 39) "thirty-nine")
  (is= (num/number->text 99) "ninety-nine")

  (is= (num/number->text 99) "ninety-nine")
  (is= (num/number->text 123) "one-hundred twenty-three")
  (is= (num/number->text 923) "nine-hundred twenty-three")

  (is= (num/number->text 123) "one-hundred twenty-three")
  (is= (num/number->text 420) "four-hundred twenty")
  (is= (num/number->text 711) "seven-hundred eleven")
  (is= (num/number->text 818) "eight-hundred eighteen")
  (is= (num/number->text 947) "nine-hundred forty-seven"))

(dotest
  (is= (num/number->text-squash -1) "")

  (is= (num/number->text-squash 0) "zero")

  (is= (num/number->text-squash 1) "one")
  (is= (num/number->text-squash 7) "seven")
  (is= (num/number->text-squash 10) "ten")
  (is= (num/number->text-squash 12) "twelve")
  (is= (num/number->text-squash 19) "nineteen")

  (is= (num/number->text-squash 20) "twenty")
  (is= (num/number->text-squash 21) "twentyone")
  (is= (num/number->text-squash 30) "thirty")
  (is= (num/number->text-squash 39) "thirtynine")
  (is= (num/number->text-squash 99) "ninetynine")

  (is= (num/number->text-squash 99) "ninetynine")
  (is= (num/number->text-squash 123) "onehundredtwentythree")
  (is= (num/number->text-squash 923) "ninehundredtwentythree")

  (is= (num/number->text-squash 123) "onehundredtwentythree")
  (is= (num/number->text-squash 420) "fourhundredtwenty")
  (is= (num/number->text-squash 711) "sevenhundredeleven")
  (is= (num/number->text-squash 818) "eighthundredeighteen")
  (is= (num/number->text-squash 947) "ninehundredfortyseven"))

(dotest
  (throws? (num/number-words-squash 3 2))

  (is= (num/number-words-squash 2 4) "twothreefour")
  (is= (num/number-words-squash 52 54) "fiftytwofiftythreefiftyfour")
  (is= (num/number-words-squash 152 154) "onehundredfiftytwoonehundredfiftythreeonehundredfiftyfour"))

(dotest
  (let [freqs-2-4   {\t 2, \w 1, \o 2, \h 1, \r 2, \e 2, \f 1, \u 1}
        num-letters (apply + (vals freqs-2-4))]
    (is= (num/letter-freqs-num-words 2 4) freqs-2-4)

    (is= 12 num-letters)
    (is (t/rel= (num/letter-prob-num-words 2 4 \e) (/ 2.0 12.0) :digits 8))
    (is (t/rel= (num/letter-prob-num-words 2 4 \r) (/ 2.0 12.0) :digits 8))
    (is (t/rel= (num/letter-prob-num-words 2 4 \w) (/ 1.0 12.0) :digits 8))

    (is= (num/letter-stats-num-words 2 4 \r)
      {:num-total-letters 12, :num-tgt-letter 2, :prob (/ 1.0 6.0)}) ))

(dotest
  (t/spyx (flame/parse-int "123"))
  (t/spyx (flame/parse-float "1.23"))
  )















