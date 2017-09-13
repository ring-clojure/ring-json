(ns ring.middleware.test.json
  (:require [clojure.test :refer :all]
            [ring.middleware.json :refer :all]
            [ring.util.io :refer [string-input-stream]]))

(deftest test-json-body
  (let [handler (wrap-json-body identity)]
    (testing "xml body"
      (let [request  {:headers {"content-type" "application/xml"}
                      :body (string-input-stream "<xml></xml>")}
            response (handler request)]
        (is (= "<xml></xml>" (slurp (:body response))))))

    (testing "json body"
      (let [request  {:headers {"content-type" "application/json; charset=UTF-8"}
                      :body (string-input-stream "{\"foo\": \"bar\"}")}
            response (handler request)]
        (is (= {"foo" "bar"} (:body response)))))

    (testing "custom json body"
      (let [request  {:headers {"content-type" "application/vnd.foobar+json; charset=UTF-8"}
                      :body (string-input-stream "{\"foo\": \"bar\"}")}
            response (handler request)]
        (is (= {"foo" "bar"} (:body response)))))

    (testing "json patch body"
      (let [json-string "[{\"op\": \"add\",\"path\":\"/foo\",\"value\": \"bar\"}]"
            request  {:headers {"content-type" "application/json-patch+json; charset=UTF-8"}
                      :body (string-input-stream json-string)}
            response (handler request)]
        (is (= [{"op" "add" "path" "/foo" "value" "bar"}] (:body response)))))

    (testing "malformed json"
      (let [request {:headers {"content-type" "application/json; charset=UTF-8"}
                     :body (string-input-stream "{\"foo\": \"bar\"")}]
        (is (= (handler request)
               {:status  400
                :headers {"Content-Type" "text/plain"}
                :body    "Malformed JSON in request body."})))))

  (let [handler (wrap-json-body identity {:keywords? true})]
    (testing "keyword keys"
      (let [request  {:headers {"content-type" "application/json"}
                      :body (string-input-stream "{\"foo\": \"bar\"}")}
            response (handler request)]
        (is (= {:foo "bar"} (:body response))))))

  (let [handler (wrap-json-body identity {:keywords? true :bigdecimals? true})]
    (testing "bigdecimal floats"
      (let [request  {:headers {"content-type" "application/json"}
                      :body (string-input-stream "{\"foo\": 5.5}")}
            response (handler request)]
        (is (decimal? (-> response :body :foo)))
        (is (= {:foo 5.5M} (:body response))))))

  (testing "custom malformed json"
    (let [malformed {:status 400
                     :headers {"Content-Type" "text/html"}
                     :body "<b>Your JSON is wrong!</b>"}
          handler (wrap-json-body identity {:malformed-response malformed})
          request {:headers {"content-type" "application/json"}
                   :body (string-input-stream "{\"foo\": \"bar\"")}]
      (is (= (handler request) malformed))))

  (let [handler  (fn [_] {:status 200 :headers {} :body {:bigdecimals cheshire.parse/*use-bigdecimals?*}})]
    (testing "don't overwrite bigdecimal binding"
      (binding [cheshire.parse/*use-bigdecimals?* false]
        (let [response ((wrap-json-body handler {:bigdecimals? true}) {})]
          (is (= (get-in response [:body :bigdecimals]) false))))
      (binding [cheshire.parse/*use-bigdecimals?* true]
        (let [response ((wrap-json-body handler {:bigdecimals? false}) {})]
          (is (= (get-in response [:body :bigdecimals]) true)))))))

(deftest test-json-body-cps
  (let [identity (fn [request respond _] (respond request))]
    (let [handler (wrap-json-body identity)]
      (testing "xml body"
        (let [request  {:headers {"content-type" "application/xml"}
                        :body (string-input-stream "<xml></xml>")}
              response (promise)
              exception    (promise)]
          (handler request response exception)
          (is (= "<xml></xml>" (slurp (:body @response))))
          (is (not (realized? exception)))))

      (testing "json body"
        (let [request  {:headers {"content-type" "application/json; charset=UTF-8"}
                        :body (string-input-stream "{\"foo\": \"bar\"}")}
              response (promise)
              exception (promise)]
          (handler request response exception)
          (is (= {"foo" "bar"} (:body @response)))
          (is (not (realized? exception)))))

      (testing "malformed json"
        (let [request {:headers {"content-type" "application/json; charset=UTF-8"}
                       :body (string-input-stream "{\"foo\": \"bar\"")}
              response (promise)
              exception (promise)]
          (handler request response exception)
          (is (= @response
                 {:status  400
                  :headers {"Content-Type" "text/plain"}
                  :body    "Malformed JSON in request body."}))
          (is (not (realized? exception))))))

    (let [handler (wrap-json-body identity {:keywords? true})]
      (testing "keyword keys"
        (let [request  {:headers {"content-type" "application/json"}
                        :body (string-input-stream "{\"foo\": \"bar\"}")}
              response (promise)
              exception    (promise)]
          (handler request response exception)
          (is (= {:foo "bar"} (:body @response)))
          (is (not (realized? exception))))))

    (testing "custom malformed json"
      (let [malformed {:status 400
                       :headers {"Content-Type" "text/html"}
                       :body "<b>Your JSON is wrong!</b>"}
            handler (wrap-json-body identity {:malformed-response malformed})
            request {:headers {"content-type" "application/json"}
                     :body (string-input-stream "{\"foo\": \"bar\"")}
            response (promise)
            exception (promise)]
        (handler request response exception)
        (is (= @response malformed))
        (is (not (realized? exception))))))

  (testing "don't overwrite bigdecimal binding"
    (let [handler  (fn [_ respond _] (respond {:status 200 :headers {} :body {:bigdecimals cheshire.parse/*use-bigdecimals?*}}) )]
      (binding [cheshire.parse/*use-bigdecimals?* false]
        (let [response (promise)]
          ((wrap-json-body handler {:bigdecimals? true}) {} response (promise))
          (is (= (get-in @response [:body :bigdecimals]) false))))
      (binding [cheshire.parse/*use-bigdecimals?* true]
        (let [response (promise)]
          ((wrap-json-body handler {:bigdecimals? false}) {} response (promise))
          (is (= (get-in @response [:body :bigdecimals]) true)))))))

(deftest test-json-params
  (let [handler  (wrap-json-params identity)]
    (testing "xml body"
      (let [request  {:headers {"content-type" "application/xml"}
                      :body (string-input-stream "<xml></xml>")
                      :params {"id" 3}}
            response (handler request)]
        (is (= "<xml></xml>" (slurp (:body response))))
        (is (= {"id" 3} (:params response)))
        (is (nil? (:json-params response)))))

    (testing "json body"
      (let [request  {:headers {"content-type" "application/json; charset=UTF-8"}
                      :body (string-input-stream "{\"foo\": \"bar\"}")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3, "foo" "bar"} (:params response)))
        (is (= {"foo" "bar"} (:json-params response)))))

    (testing "json body with bigdecimals"
      (let [handler (wrap-json-params identity {:bigdecimals? true})
            request  {:headers {"content-type" "application/json; charset=UTF-8"}
                      :body (string-input-stream "{\"foo\": 5.5}")
                      :params {"id" 3}}
            response (handler request)]
        (is (decimal? (get-in response [:params "foo"])))
        (is (decimal? (get-in response [:json-params "foo"])))
        (is (= {"id" 3, "foo" 5.5M} (:params response)))
        (is (= {"foo" 5.5M} (:json-params response)))))

    (testing "custom json body"
      (let [request  {:headers {"content-type" "application/vnd.foobar+json; charset=UTF-8"}
                      :body (string-input-stream "{\"foo\": \"bar\"}")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3, "foo" "bar"} (:params response)))
        (is (= {"foo" "bar"} (:json-params response)))))

    (testing "json schema body"
      (let [request  {:headers {"content-type" "application/schema+json; charset=UTF-8"}
                      :body (string-input-stream "{\"type\": \"schema\",\"properties\":{}}")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3, "type" "schema", "properties" {}} (:params response)))
        (is (= {"type" "schema", "properties" {}} (:json-params response)))))

    (testing "array json body"
      (let [request  {:headers {"content-type" "application/vnd.foobar+json; charset=UTF-8"}
                      :body (string-input-stream "[\"foo\"]")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3} (:params response)))
        (is (= ["foo"] (:json-params response)))))

    (testing "malformed json"
      (let [request {:headers {"content-type" "application/json; charset=UTF-8"}
                     :body (string-input-stream "{\"foo\": \"bar\"")}]
        (is (= (handler request)
               {:status  400
                :headers {"Content-Type" "text/plain"}
                :body    "Malformed JSON in request body."})))))

  (testing "custom malformed json"
    (let [malformed {:status 400
                     :headers {"Content-Type" "text/html"}
                     :body "<b>Your JSON is wrong!</b>"}
          handler (wrap-json-params identity {:malformed-response malformed})
          request {:headers {"content-type" "application/json"}
                   :body (string-input-stream "{\"foo\": \"bar\"")}]
      (is (= (handler request) malformed))))

  (testing "don't overwrite bigdecimal binding"
    (let [handler  (fn [_] {:status 200 :headers {} :body {:bigdecimals cheshire.parse/*use-bigdecimals?*}})]
      (binding [cheshire.parse/*use-bigdecimals?* false]
        (let [response ((wrap-json-params handler {:bigdecimals? true}) {})]
          (is (= (get-in response [:body :bigdecimals]) false))))
      (binding [cheshire.parse/*use-bigdecimals?* true]
        (let [response ((wrap-json-params handler {:bigdecimals? false}) {})]
          (is (= (get-in response [:body :bigdecimals]) true)))))))

(deftest test-json-params-cps
  (let [identity (fn [request respond _] (respond request))]
    (let [handler  (wrap-json-params identity)]
      (testing "xml body"
        (let [request  {:headers {"content-type" "application/xml"}
                        :body (string-input-stream "<xml></xml>")
                        :params {"id" 3}}
              response (promise)
              exception (promise)]
          (handler request response exception)
          (is (= "<xml></xml>" (slurp (:body @response))))
          (is (= {"id" 3} (:params @response)))
          (is (nil? (:json-params @response)))
          (is (not (realized? exception)))))

      (testing "json body"
        (let [request  {:headers {"content-type" "application/json; charset=UTF-8"}
                        :body (string-input-stream "{\"foo\": \"bar\"}")
                        :params {"id" 3}}
              response (promise)
              exception (promise)]
          (handler request response exception)
          (is (= {"id" 3, "foo" "bar"} (:params @response)))
          (is (= {"foo" "bar"} (:json-params @response)))
          (is (not (realized? exception)))))

      (testing "malformed json"
        (let [request {:headers {"content-type" "application/json; charset=UTF-8"}
                       :body (string-input-stream "{\"foo\": \"bar\"")}
              response (promise)
              exception (promise)]
          (handler request response exception)
          (is (= @response
                 {:status  400
                  :headers {"Content-Type" "text/plain"}
                  :body    "Malformed JSON in request body."}))
          (is (not (realized? exception))))))

    (testing "custom malformed json"
      (let [malformed {:status 400
                       :headers {"Content-Type" "text/html"}
                       :body "<b>Your JSON is wrong!</b>"}
            handler (wrap-json-params identity {:malformed-response malformed})
            request {:headers {"content-type" "application/json"}
                     :body (string-input-stream "{\"foo\": \"bar\"")}
            response (promise)
            exception (promise)]
        (handler request response exception)
        (is (= @response malformed))
        (is (not (realized? exception)))))

    (testing "don't overwrite bigdecimal binding"
      (let [handler  (fn [_ respond _] (respond {:status 200 :headers {} :body {:bigdecimals cheshire.parse/*use-bigdecimals?*}}) )]
        (binding [cheshire.parse/*use-bigdecimals?* false]
          (let [response (promise)]
            ((wrap-json-params handler {:bigdecimals? true}) {} response (promise))
            (is (= (get-in @response [:body :bigdecimals]) false))))
        (binding [cheshire.parse/*use-bigdecimals?* true]
          (let [response (promise)]
            ((wrap-json-params handler {:bigdecimals? false}) {} response (promise))
            (is (= (get-in @response [:body :bigdecimals]) true))))))))

(deftest test-json-response
  (testing "map body"
    (let [handler  (constantly {:status 200 :headers {} :body {:foo "bar"}})
          response ((wrap-json-response handler) {})]
      (is (= (get-in response [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (:body response) "{\"foo\":\"bar\"}"))))

  (testing "string body"
    (let [handler  (constantly {:status 200 :headers {} :body "foobar"})
          response ((wrap-json-response handler) {})]
      (is (= (:headers response) {}))
      (is (= (:body response) "foobar"))))

  (testing "vector body"
    (let [handler  (constantly {:status 200 :headers {} :body [:foo :bar]})
          response ((wrap-json-response handler) {})]
      (is (= (get-in response [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (:body response) "[\"foo\",\"bar\"]"))))

  (testing "list body"
    (let [handler  (constantly {:status 200 :headers {} :body '(:foo :bar)})
          response ((wrap-json-response handler) {})]
      (is (= (get-in response [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (:body response) "[\"foo\",\"bar\"]"))))

  (testing "set body"
    (let [handler  (constantly {:status 200 :headers {} :body #{:foo :bar}})
          response ((wrap-json-response handler) {})]
      (is (= (get-in response [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (or (= (:body response) "[\"foo\",\"bar\"]")
              (= (:body response) "[\"bar\",\"foo\"]")))))

  (testing "JSON options"
    (let [handler  (constantly {:status 200 :headers {} :body {:foo "bar" :baz "quz"}})
          response ((wrap-json-response handler {:pretty true}) {})]
      (is (or (= (:body response) "{\n  \"foo\" : \"bar\",\n  \"baz\" : \"quz\"\n}")
              (= (:body response) "{\n  \"baz\" : \"quz\",\n  \"foo\" : \"bar\"\n}")))))

  (testing "don’t overwrite Content-Type if already set"
    (let [handler  (constantly {:status 200 :headers {"Content-Type" "application/json; some-param=some-value"} :body {:foo "bar"}})
          response ((wrap-json-response handler) {})]
      (is (= (get-in response [:headers "Content-Type"]) "application/json; some-param=some-value"))
      (is (= (:body response) "{\"foo\":\"bar\"}")))))

(deftest test-json-response-cps
  (testing "map body"
    (let [handler  (fn [_ respond _] (respond {:status 200 :headers {} :body {:foo "bar"}}))
          response (promise)
          exception (promise)]
      ((wrap-json-response handler) {} response exception)
      (is (= (get-in @response [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (:body @response) "{\"foo\":\"bar\"}"))
      (is (not (realized? exception)))))

  (testing "string body"
    (let [handler  (fn [_ respond _] (respond {:status 200 :headers {} :body "foobar"}))
          response (promise)
          exception (promise)]
      ((wrap-json-response handler) {} response exception)
      (is (= (:headers @response) {}))
      (is (= (:body @response) "foobar"))
      (is (not (realized? exception)))))

  (testing "JSON options"
    (let [handler  (fn [_ respond _] (respond {:status 200 :headers {} :body {:foo "bar" :baz "quz"}}))
          response (promise)
          exception (promise)]
      ((wrap-json-response handler {:pretty true}) {} response exception)
      (is (or (= (:body @response) "{\n  \"foo\" : \"bar\",\n  \"baz\" : \"quz\"\n}")
              (= (:body @response) "{\n  \"baz\" : \"quz\",\n  \"foo\" : \"bar\"\n}")))
      (is (not (realized? exception)))))

  (testing "don’t overwrite Content-Type if already set"
    (let [handler  (fn [_ respond _] (respond {:status 200 :headers {"Content-Type" "application/json; some-param=some-value"} :body {:foo "bar"}}))
          response (promise)
          exception (promise)]
      ((wrap-json-response handler) {} response exception)
      (is (= (get-in @response [:headers "Content-Type"]) "application/json; some-param=some-value"))
      (is (= (:body @response) "{\"foo\":\"bar\"}"))
      (is (not (realized? exception))))))