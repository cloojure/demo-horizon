(ns demo.hello
  (:use tupelo.core)
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [hiccup.core :as hiccup]
    [io.pedestal.http :as http]
    [io.pedestal.http.content-negotiation :as conneg]
    [io.pedestal.http.route :as route]
    [io.pedestal.interceptor :as interceptor]
    [io.pedestal.interceptor.chain :as chain]
    [io.pedestal.test :as pedtst]
    [schema.core :as s]
    [tupelo.parse :as tpar]
    [tupelo.pedestal :as tp]
    [tupelo.schema :as tsk]
    [tupelo.string :as ts]
   )
  (:gen-class))

; Prismatic Schema type definitions
(s/set-fn-validation! true) ; enforce fn schemas

(defn ok [body]
  {:status 200 :body body})

(defn not-found []
  {:status 404 :body "Not found\n"})

(tp/definterceptor echo-intc
  {:leave (fn [context]
            (assoc context :response (ok (pretty-str (tpar/edn-parsible context)))))})

(tp/definterceptor music-intc
  {:leave (fn [context]
            (assoc context :response (ok (slurp (io/resource "music.txt")))))})

(tp/definterceptor demo-intc
  {:leave (fn [ctx]
            (assoc ctx :response
                       (glue (ok (slurp (io/resource "public/demo.html")))
                         {:headers {"Content-Security-Policy" "script-src 'self' 'unsafe-inline' "
                                    "Content-Type" "text/html"}})))})

(def unmentionables #{"YHWH" "Voldemort" "Mxyzptlk" "Rumplestiltskin" "曹操"})

(def supported-types [tp/text-html tp/application-edn tp/application-json tp/text-plain] )

(def content-negotiation-intc
  (validate tp/interceptor?
    (conneg/negotiate-content supported-types)))

(defn negotiated-type
  "Returns the Content-Type determined by the content negotiation interceptor"
  [ctx]
  (get-in ctx [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (condp = content-type
    tp/text-html         body
    tp/text-plain        body
    tp/application-edn   (pr-str body)
    tp/application-json  (json/write-str body)))

(defn coerce-to
  [response content-type]
  (-> response
    (update :body transform-content content-type)
    (assoc-in [:headers "Content-Type"] content-type)))

(tp/definterceptor coerce-body-intc
  {:leave (fn [ctx]
            ; Check if previous interceptor has already set content-type
            (if (get-in ctx [:response :headers tp/content-type])
              ctx ; content-type already set; don't override
              (do
                ; Content-Type undefined so far. Coerce response body to negotiated
                ; type & record result
                (update-in ctx [:response] coerce-to (negotiated-type ctx)))))})

(defn greeting-for [user-name]
  "Returns a greeting for `user-name` if present, else a generic greeting. Returns `nil`
  for unmentionable values of `user-name`. "
  (cond
    (contains? unmentionables user-name) nil    ; warning:  case-sensitive match!
    (or (nil? user-name)
      (empty? user-name))   "Hello, World! \n"
    :else  (format "Hello, %s!" user-name)))

(tp/definterceptor respond-hello-intc
  {:leave (fn [ctx]
            (let [user-name (get-in ctx [:request :query-params :name])
                  body-str  (greeting-for user-name)
                  response  (if body-str
                              (ok body-str)
                              (not-found))]
              (assoc ctx :response response)))})

; NOTE!  a handler fn consumes a REQUEST (not a CONTEXT) !!!
; NOTE!  a handler fn produces a RESPONSE (not a :response in the CONTEXT) !!!
(defn index-handler
  [request]
  (validate tp/request? request)
  (let [html-txt (hiccup/html
                   [:div
                    [:div "Try the following:"]
                    [:ul
                     [:li [:a {:href "http://alanwthompson.com/greet"} "Basic Greeting"]]
                     [:li [:a {:href "http://alanwthompson.com/greet?name=Fred"} "Personal Greeting"]]]])]
    (let [response (ok html-txt)]
      (coerce-to response tp/text-html))))

(def server-routes
  (route/expand-routes
    #{
      (tp/table-route {:verb :get :path "/echo"  :route-name :echo  :interceptors echo-intc})
      (tp/table-route {:verb :get :path "/music" :route-name :music :interceptors music-intc})
      (tp/table-route {:verb         :get :path "/demo" :route-name :demo
                       :interceptors [coerce-body-intc content-negotiation-intc demo-intc]})
      (tp/table-route {:verb :get :path "/"      :route-name :index :interceptors [index-handler]})
      (tp/table-route {:verb :get :path "/greet" :route-name :greet
                       :interceptors [ ; coerce-body-intc
                                       ; content-negotiation-intc
                                      respond-hello-intc]})}))

; #todo => tupelo.pedestal ???
;---------------------------------------------------------------------------------------------------

(def service-map
  {::http/routes server-routes})

(defn -main [& args]
  (println "main - enter")
  (let [svc-map (cond-it-> service-map
                  (not-empty? args) (let [port-str (first args)
                                          port     (tpar/parse-int port-str)]
                                      (glue it {::http/port port})))]
    (spyx-pretty svc-map)
    (tp/with-server svc-map
      (println "main - server started (never print this)")
      ))
  (println "main - exit"))

