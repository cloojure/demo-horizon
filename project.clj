(defproject demo-horizon "0.1.0-SNAPSHOT"
  :min-lein-version "2.7.1"
  :dependencies [[bidi "2.1.5"]
                 [binaryage/devtools "0.9.10"]
                 [binaryage/oops "0.7.0"]
                 [cljs-ajax "0.8.0"]
                 [hiccup "1.0.5"]
                 [http-kit "2.3.0"]
                 [io.pedestal/pedestal.jetty "0.5.5"]
                 [io.pedestal/pedestal.route "0.5.5"]
                 [io.pedestal/pedestal.service "0.5.5"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                ;[org.clojure/clojurescript "1.10.516"]  ; ***** WARNING - FAILS IN COMPILE!!! *****
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/test.check "0.9.0"]
                 [org.slf4j/slf4j-simple "1.7.26"]
                 [prismatic/schema "1.1.10"]
                 [re-frame "0.10.6"]
                 [reagent "0.8.1"]
                 [reagent-utils "0.3.2"]
                 [secretary "1.2.3"]
                 [tupelo "0.9.133"] ]
  :plugins [[com.jakemccrary/lein-test-refresh   "0.23.0"]
            [lein-ancient "0.6.15"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-codox "0.10.6"]
            [lein-doo "0.1.11"]
            [lein-figwheel "0.5.18"] ]

  :doo {:karma {:config {"plugins"       ["karma-junit-reporter"]
                         "reporters"     ["progress" "junit"]
                         "junitReporter" {"outputDir" "target/test-results"}}}
        :paths {:karma   "node_modules/karma/bin/karma"
                :phantom "node_modules/phantomjs/bin/phantomjs"}}

  :source-paths   [ "src/cljc" "src/clj" ]
  :test-paths     [ "test/cljc" "test/clj" ]
  :global-vars    {*warn-on-reflection* false}
  :main  demo.hello ; ^:skip-aot   <=  when use this????

  ; need to add the compliled assets to the :clean-targets
  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "out"
                                    :target-path]
  :auto-clean false ; ***** VERY IMPORTANT **** else 'lein uberjar' does 'clean' first & deletes demo.js !!!
  :profiles {:uberjar {:aot :all}}

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths [ "src/cljc" "src/cljs" ]
                ; The presence of a :figwheel configuration here will cause figwheel to inject the
                ; figwheel client into your build
                :figwheel     {:on-jsload "demo.core/figwheel-reload"
                               ; :open-urls will pop open your application in the default browser once
                               ; Figwheel has started and compiled your application.  Comment this out
                               ; once it no longer serves you.
                               :open-urls ["http://localhost:3449/demo.html"]}
                :compiler     {:main                 demo.core
                               :optimizations        :none
                               :libs                 ["resources/public/libs"] ; recursive includes all children

                               ; figwheel server has implicit path `resources/public`, leave off here
                               :foreign-libs         [{:file     "dino.js"
                                                       :provides ["dinoPhony"]}]
                               :externs              ["dino-externs.js"]

                               :output-to            "resources/public/js/compiled/demo.js"
                               :output-dir           "resources/public/js/compiled/demo-dev"
                               :asset-path           "js/compiled/demo-dev" ; rel to figwheel default of `resources/public`
                               ;                       ^^^^^ must match :output-dir

                               :source-map           true
                               :source-map-timestamp true}}
               {:id           "test"
                :source-paths [ "src/cljc" "test/cljc"
                                "src/cljs" "test/cljs" ] ; #todo  :test-paths ???
                :compiler     {:main                 tst.demo.doorunner
                               :optimizations        :none ; :advanced
                               :libs                 ["resources/public/libs"] ; recursively includes all children

                               ; tests run w/o figwheel server, so need to explicitely add path `resources/public` here
                               :foreign-libs         [{:file     "resources/public/dino.js"
                                                       :provides ["dinoPhony"]}]
                               :externs              ["resources/public/dino-externs.js"]

                               :output-to            "resources/public/js/compiled/bedrock.js"
                               :output-dir           "resources/public/js/compiled/bedrock-tst"
                               ; :asset-path           "js/compiled/bedrock-tst"  ; not used for testing
                               ; ^^^ rel to figwheel default of `resources/public`

                               :source-map           true
                               :source-map-timestamp true}}]}

  ; automatically handle `--add-modules` stuff req'd for Java 9 & Java 10
  :jvm-opts ["-Xmx1g"]
  )
