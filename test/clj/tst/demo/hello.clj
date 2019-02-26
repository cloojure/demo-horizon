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
    [tupelo.schema :as tsk]
    ))

;---------------------------------------------------------------------------------------------------
; v1: Hello, World (http://pedestal.io/guides/hello-world)

(def tst-service-map
  {::http/host   "localhost" ; critical to make it listen on ALL IP addrs not just "localhost"
   ::http/join?  false ; true => block the starting thread (want this for supervisord in prod); false => don't block
   ::http/port   8890
   ::http/routes server-routes })

;---------------------------------------------------------------------------------------------------
(dotest
  ;(with-debug-tag :dbg-01)
  ;(with-system-err-str) ; capture jetty logging from System/err
  (let [ctx-out (tupelo.pedestal/invoke-interceptors {} [respond-hello-intc])]
    (is= 200 (fetch-in ctx-out [:response :status]))
    (is-nonblank= "Hello, World!" (fetch-in ctx-out [:response :body])))

  (is (submap? {:path        "/greet"
                :method      :get
                :route-name  :greet
                :path-params {}}
        (route/try-routing-for server-routes :prefix-tree "/greet" :get))))

(dotest
  ;(with-debug-tag :dbg-02)
  ;(with-system-err-str) ; capture jetty logging from System/err
  (tupelo.pedestal/with-service tst-service-map ; mock testing w/o actually starting jetty
    (let [resp (tupelo.pedestal/service-get "/greet")]
      (is= (grab :status resp) 200)
      (is-nonblank= "Hello, World!" (grab :body resp))

      (let [headers-expected {tp/strict-transport-security         "max-age=31536000; includeSubdomains",
                              tp/x-frame-options                   "DENY",
                              tp/x-content-type-options            "nosniff",
                              tp/x-xss-protection                  "1; mode=block",
                              tp/x-download-options                "noopen",
                              tp/x-permitted-cross-domain-policies "none",
                              tp/content-security-policy           "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;",
                              tp/content-type                      "text/plain"}
            headers-actual   (grab :headers resp)]
        (is= headers-actual headers-expected))))

  (dotest
    ;(with-debug-tag :dbg-03)
    (let [sys-err-str (with-system-err-str ; capture jetty logging from System/err
                        (tupelo.pedestal/with-server tst-service-map ; test over http using jetty server
                          (let [resp @(http-client/get "http://localhost:8890/greet")]
                            (is (ts/contains-str? (grab :body resp) "Hello, World!")))))]
      ; eg '[qtp1379526008-32] INFO io.pedestal.http - {:msg "GET /greet", :line 80}'
      (is (not-empty? (ts/fgrep "GET /greet" sys-err-str))))))

;---------------------------------------------------------------------------------------------------
; v2: Hello World, With Parameters (http://pedestal.io/guides/hello-world-query-parameters)

(dotest
  (with-debug-tag :dbg-04
    (with-system-err-str ; capture jetty logging from System/err
      (tupelo.pedestal/with-service tst-service-map ; mock testing w/o actually starting jetty

        (let [resp (tupelo.pedestal/service-get "/greet?name=Michael")]
          (destruct [resp {:status ? :body ?}]

            (is= (spyx status) 200)
            (is-nonblank= body "Hello, Michael!")))

        (with-debug-tag :dbg-04-a
          (let [resp (tupelo.pedestal/service-get "/greet?name=")]
            (destruct [resp {:status ? :body ?}]
              (spyx status)
              (is= status 200)
              (is-nonblank= body "Hello, World!"))))

        (let [resp (tupelo.pedestal/service-get "/greet")]
          (destruct [resp {:status ? :body ?}]
            (is= status 200)
            (is-nonblank= body "Hello, World!")))

        ; test the unmentionables
        (let [resp (tupelo.pedestal/service-get "/greet?name=YHWH")]
          (destruct [resp {:status ? :body ?}]
            (is= status 404)
            (is-nonblank= body "Not found")))

        (let [resp (tupelo.pedestal/service-get "/greet?name=Voldemort")]
          (destruct [resp {:status ? :body ?}]
            (is= status 404)
            (is-nonblank= body "Not found")))

        (let [resp (tupelo.pedestal/service-get "/greet?name=voldemort")]
          (destruct [resp {:status ? :body ?}]
            (is= status 200)
            (is-nonblank= body "Hello, voldemort!"))))))) ; case-sensitive test

;---------------------------------------------------------------------------------------------------
; v3: Hello World, With Content-Types (http://pedestal.io/guides/hello-world-content-types)

(dotest
  ;(with-debug-tag :dbg-05)
  ;(with-system-err-str) ; capture jetty logging from System/err
  (tupelo.pedestal/with-service tst-service-map ; mock testing w/o actually starting jetty
    (let [resp (tupelo.pedestal/service-get "/greet")]
      (destruct [resp {:status ? :headers ? :body ?}]
        (is= status 200)
        (is-nonblank= body "  Hello,    World!   ")
        (is= headers
          {tp/strict-transport-security         "max-age=31536000; includeSubdomains",
           tp/x-frame-options                   "DENY",
           tp/x-content-type-options            "nosniff",
           tp/x-xss-protection                  "1; mode=block",
           tp/x-download-options                "noopen",
           tp/x-permitted-cross-domain-policies "none",
           tp/content-security-policy           "object-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:;",
           tp/content-type                      tp/text-plain})))

    (let [resp (tupelo.pedestal/service-get "/greet" :headers {tp/accept tp/application-edn})]
      (is= (grab :status resp) 200)
      (is= tp/text-plain (fetch-in resp [:headers tp/content-type])))

    (let [resp (tupelo.pedestal/service-get "/greet"
                 :headers {tp/accept (str/join ", " [tp/application-xml tp/application-json])})]
      (is= (grab :status resp) 200)
      (is= tp/text-plain (fetch-in resp [:headers tp/content-type])))))


