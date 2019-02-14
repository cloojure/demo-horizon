(ns tst.demo.hello
  (:use demo.hello tupelo.core tupelo.test)
  (:require
    [clojure.string :as str]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [io.pedestal.interceptor :as interceptor]
    [io.pedestal.interceptor.chain :as chain]
    [io.pedestal.test :as pedtst]
    [org.httpkit.client :as http-client]
    [schema.core :as s]
    [tupelo.pedestal :as tp]
    [tupelo.string :as ts]
    [tupelo.schema :as tsk]))

;---------------------------------------------------------------------------------------------------
; v1: Hello, World (http://pedestal.io/guides/hello-world)

(def tst-service-map
  {::http/port   8890
   ::http/host   "localhost" ; critical to make it listen on ALL IP addrs not just "localhost"
   ::http/join?  false ; true => block the starting thread (want this for supervisord in prod); false => don't block
   })

(defmacro with-server ; #todo => tupelo.pedestal ???
  "Start & stop the server, even if exception occurs."
  [& forms]
  `(try
     (server-config! tst-service-map)
     (server-start!) ; sends log output to stdout
     ~@forms
     (finally
       (server-stop!))))

(s/defn invoke-interceptors ; #todo => tupelo.pedestal
  "Given a context and a vector of interceptor-maps, invokes the interceptor chain
  and returns the resulting output context."
  [ctx :- tsk/KeyMap
   interceptors :- [tsk/KeyMap]] ; #todo => tupelo.pedestal & specialize to interceptor maps
  (let [pedestal-interceptors (mapv interceptor/map->Interceptor interceptors)]
    (chain/execute ctx pedestal-interceptors)))

(dotest
  (let [ctx-out (invoke-interceptors {} [respond-hello-intc])]
    (is= 200 (fetch-in ctx-out [:response :status]))
    (nonblank= "Hello, World!" (fetch-in ctx-out [:response :body])))

  (is (submap? {:path        "/greet"
                :method      :get
                :route-name  :greet
                :path-params {}}
        (route/try-routing-for server-routes :prefix-tree "/greet" :get))))

(dotest
  (server-config! tst-service-map)   ; mock testing w/o actually starting jetty
  (let [resp (pedtst/response-for (service-fn) :get "/greet")]
    (is= (grab :status resp) 200)
    (nonblank= "Hello, World!" (grab :body resp))

    (let [headers-expected {tp/strict-transport-security          "max-age=31536000; includeSubdomains",
                            tp/x-frame-options                    "DENY",
                            tp/x-content-type-options             "nosniff",
                            tp/x-xss-protection                   "1; mode=block",
                            tp/x-download-options                 "noopen",
                            tp/x-permitted-cross-domain-policies  "none",
                            tp/content-security-policy            "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;",
                            tp/content-type                       "text/plain"}
          headers-actual   (grab :headers resp)]
      (is= headers-actual headers-expected))))

(dotest
  (try
    (let [sys-err-str (with-system-err-str ; capture jetty logging from System/err
                        (with-server ; test over http using jetty server
                          (let [resp @(http-client/get "http://localhost:8890/greet")]
                            (is (ts/contains-str? (grab :body resp) "Hello, World!")))))]
      (is (not-empty? (ts/fgrep "GET /greet" sys-err-str)))))) ; eg '[qtp1379526008-32] INFO io.pedestal.http - {:msg "GET /greet", :line 80}'

;---------------------------------------------------------------------------------------------------
; v2: Hello World, With Parameters (http://pedestal.io/guides/hello-world-query-parameters)

(dotest
  (server-config! tst-service-map) ; mock testing w/o actually starting jetty

  (let [resp (pedtst/response-for (service-fn) :get "/greet?name=Michael")]
    (is= (grab :status resp) 200)
    (nonblank= (grab :body resp) "Hello, Michael!"))

  (let [resp (pedtst/response-for (service-fn) :get "/greet?name=")]
    (is= (grab :status resp) 200)
    (nonblank= (grab :body resp) "Hello, World!"))

  (let [resp (pedtst/response-for (service-fn) :get "/greet")]
    (is= (grab :status resp) 200)
    (nonblank= (grab :body resp) "Hello, World!"))

  ; test the unmentionables
  (let [resp (pedtst/response-for (service-fn) :get "/greet?name=YHWH")]
    (is= (grab :status resp) 404)
    (nonblank= (grab :body resp) "Not found"))

  (let [resp (pedtst/response-for (service-fn) :get "/greet?name=Voldemort")]
    (is= (grab :status resp) 404)
    (nonblank= (grab :body resp) "Not found"))

  (let [resp (pedtst/response-for (service-fn) :get "/greet?name=voldemort")]
    (is= (grab :status resp) 200)
    (nonblank= (grab :body resp) "Hello, voldemort!"))) ; case-sensitive test

;---------------------------------------------------------------------------------------------------
; v3: Hello World, With Content-Types (http://pedestal.io/guides/hello-world-content-types)

(dotest
  (server-config! tst-service-map) ; mock testing w/o actually starting jetty

  (let [resp (pedtst/response-for (service-fn) :get "/greet")]
    (is= (grab :status resp) 200)
    (is= (grab :headers resp)
      {tp/strict-transport-security         "max-age=31536000; includeSubdomains",
       tp/x-frame-options                   "DENY",
       tp/x-content-type-options            "nosniff",
       tp/x-xss-protection                  "1; mode=block",
       tp/x-download-options                "noopen",
       tp/x-permitted-cross-domain-policies "none",
       tp/content-security-policy           "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;",
       tp/content-type                      tp/text-plain})
    (is= tp/text-plain (fetch-in resp [:headers tp/content-type])))

  (let [resp (pedtst/response-for (service-fn) :get "/greet" :headers {tp/accept tp/application-edn})]
    (is= (grab :status resp) 200)
    (is= tp/text-plain (fetch-in resp [:headers tp/content-type])))

  (let [resp (pedtst/response-for (service-fn) :get "/greet"
               :headers {tp/accept (str/join ", " [tp/application-xml tp/application-json])})]
    (is= (grab :status resp) 200)
    (is= tp/text-plain (fetch-in resp [:headers tp/content-type]))))

