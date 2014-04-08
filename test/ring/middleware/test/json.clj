(ns ring.middleware.test.json
  (:use ring.middleware.json
        clojure.test
        ring.util.io))

(deftest test-json-body
  (let [handler (wrap-json-body identity)]
    (testing "xml body"
      (let [request  {:content-type "application/xml"
                      :body (string-input-stream "<xml></xml>")}
            response (handler request)]
        (is (= "<xml></xml>") (slurp (:body response)))))
    
    (testing "json body"
      (let [request  {:content-type "application/json; charset=UTF-8"
                      :body (string-input-stream "{\"foo\": \"bar\"}")}
            response (handler request)]
        (is (= {"foo" "bar"} (:body response)))))

    (testing "custom json body"
      (let [request  {:content-type "application/vnd.foobar+json; charset=UTF-8"
                      :body (string-input-stream "{\"foo\": \"bar\"}")}
            response (handler request)]
        (is (= {"foo" "bar"} (:body response)))))

    (testing "json patch body"
      (let [json-string "[{\"op\": \"add\",\"path\":\"/foo\",\"value\": \"bar\"}]"
            request  {:content-type "application/json-patch+json; charset=UTF-8"
                      :body (string-input-stream json-string)}
            response (handler request)]
        (is (= [{"op" "add" "path" "/foo" "value" "bar"}] (:body response))))))

    (testing "with keywords option"
      (let [handler (wrap-json-body identity {:keywords? true})
            request  {:content-type "application/json"
                      :body (string-input-stream "{\"foo\": \"bar\"}")}
            response (handler request)]
        (is (= {:foo "bar"} (:body response)))))

    (testing "invalid json body"
      (testing "with custom malformed handler"
        (let [malformed-handler (fn [{:keys [message]}]
                                  {:status 400
                                   :body
                                   {:error "The JSON provided is either malformed or invalid."}})
              handler (wrap-json-body identity {:handle-malformed malformed-handler})
              request  {:content-type "application/json; charset=UTF-8"
                        :body (string-input-stream "{:foo \"bar}")
                        :params {"id" 3}}
              response (handler request)]
          (is (= 400 (:status response)))
          (is (= "The JSON provided is either malformed or invalid." (get-in response [:body :error])))))))

(deftest test-json-params
  (let [handler  (wrap-json-params identity)]
    (testing "xml body"
      (let [request  {:content-type "application/xml"
                      :body (string-input-stream "<xml></xml>")
                      :params {"id" 3}}
            response (handler request)]
        (is (= "<xml></xml>") (slurp (:body response)))
        (is (= {"id" 3} (:params response)))
        (is (nil? (:json-params response)))))

    (testing "json body"
      (let [request  {:content-type "application/json; charset=UTF-8"
                      :body (string-input-stream "{\"foo\": \"bar\"}")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3, "foo" "bar"} (:params response)))
        (is (= {"foo" "bar"} (:json-params response)))))

    (testing "json body with bigdecimals"
      (let [handler (wrap-json-params identity {:bigdecimals? true})
            request  {:content-type "application/json; charset=UTF-8"
                      :body (string-input-stream "{\"foo\": 5.5}")
                      :params {"id" 3}}
            response (handler request)]
        (is (decimal? (get-in response [:params "foo"])))
        (is (decimal? (get-in response [:json-params "foo"])))
        (is (= {"id" 3, "foo" 5.5M} (:params response)))
        (is (= {"foo" 5.5M} (:json-params response)))))

    (testing "custom json body"
      (let [request  {:content-type "application/vnd.foobar+json; charset=UTF-8"
                      :body (string-input-stream "{\"foo\": \"bar\"}")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3, "foo" "bar"} (:params response)))
        (is (= {"foo" "bar"} (:json-params response)))))

    (testing "json schema body"
      (let [request  {:content-type "application/schema+json; charset=UTF-8"
                      :body (string-input-stream "{\"type\": \"schema\",\"properties\":{}}")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3, "type" "schema", "properties" {}} (:params response)))
        (is (= {"type" "schema", "properties" {}} (:json-params response)))))

    (testing "array json body"
      (let [request  {:content-type "application/vnd.foobar+json; charset=UTF-8"
                      :body (string-input-stream "[\"foo\"]")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3} (:params response)))))))

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
      (is (= (:body response) "[\"foo\",\"bar\"]"))))

  (testing "JSON options"
    (let [handler  (constantly {:status 200 :headers {} :body {:foo "bar" :baz "quz"}})
          response ((wrap-json-response handler {:pretty true}) {})]
      (is (= (:body response)
             "{\n  \"foo\" : \"bar\",\n  \"baz\" : \"quz\"\n}"))))

  (testing "don’t overwrite Content-Type if already set"
    (let [handler  (constantly {:status 200 :headers {"Content-Type" "application/json; some-param=some-value"} :body {:foo "bar"}})
          response ((wrap-json-response handler) {})]
      (is (= (get-in response [:headers "Content-Type"]) "application/json; some-param=some-value"))
      (is (= (:body response) "{\"foo\":\"bar\"}")))))
