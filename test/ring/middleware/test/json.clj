(ns ring.middleware.test.json
  (:use ring.middleware.json
        clojure.test
        ring.util.io)
  (:require [ring.mock.request :refer [request content-type body]]
            [ring.util.response :refer [response header]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]))

(deftest test-json-body
  (let [handler (wrap-json-body identity)]
    (testing "xml body"
      (let [resp (handler (-> (request :get "/")
                              (content-type "application/xml")
                              (body "<xml></xml>")))]
        (is (= "<xml></xml>") (:body resp))))
    
    (testing "json body"
      (let [resp (handler (-> (request :get "/")
                              (content-type "application/json; charset=UTF-8")
                              (body "{\"foo\": \"bar\"}")))]
        (is (= {"foo" "bar"} (:body resp)))))

    (testing "custom json body"
      (let [resp (handler (-> (request :get "/")
                              (content-type "application/vnd.foobar+json; charset=UTF-8")
                              (body "{\"foo\": \"bar\"}")))]
        (is (= {"foo" "bar"} (:body resp)))))

    (testing "json patch body"
      (let [resp (handler (-> (request :get "/")
                              (content-type "application/json-patch+json; charset=UTF-8")
                              (body "[{\"op\": \"add\",\"path\":\"/foo\",\"value\": \"bar\"}]")))]
        (is (= [{"op" "add" "path" "/foo" "value" "bar"}] (:body resp)))))

    (testing "malformed json"
      (let [resp (handler (-> (request :get "/")
                              (content-type "application/json; charset=UTF-8")
                              (body "{\"foo\": \"bar\"")))]
        (is (= resp 
               {:status  400
                :headers {"Content-Type" "text/plain"}
                :body    "Malformed JSON in request body."})))))

  (let [handler (wrap-json-body identity {:keywords? true})]
    (testing "keyword keys"
      (let [resp (handler (-> (request :get "/")
                              (content-type "application/json")
                              (body "{\"foo\": \"bar\"}")))]
        (is (= {:foo "bar"} (:body resp))))))

  (let [handler (wrap-json-body identity {:keywords? true :bigdecimals? true})]
    (testing "bigdecimal floats"
      (let [resp (handler (-> (request :get "/")
                              (content-type "application/json")
                              (body "{\"foo\": 5.5}")))]
        (is (decimal? (-> resp :body :foo)))
        (is (= {:foo 5.5M} (:body resp))))))

  (testing "custom malformed json"
    (let [malformed {:status 400
                     :headers {"Content-Type" "text/html"}
                     :body "<b>Your JSON is wrong!</b>"}
          handler (wrap-json-body identity {:malformed-response malformed})]
      (is (= (handler (-> (request :get "/") 
                          (content-type "application/json")
                          (body "{\"foo\": \"bar\"")))
             malformed)))))

(deftest test-json-params
  (let [handler (-> identity 
                    (wrap-defaults api-defaults)
                    (wrap-json-params {:bigdecimals? true}))]
    (testing "xml body"
      (let [resp (handler (-> (request :get "/" {"id" 3})
                              (content-type "application/xml")
                              (body "<xml></xml>")))]
        (is (= "<xml></xml>") (:body resp))
        (is (= {:id "3"} (:params resp)))
        (is (nil? (:json-params resp)))))

    (testing "json body"
      (let [resp (handler (-> (request :get "/" {"id" 3})
                              (content-type "application/json; charset=UTF-8")
                              (body "{\"foo\": \"bar\"}")))]
        (is (= {:id "3", :foo "bar"} (:params resp)))
        (is (= {"foo" "bar"} (:json-params resp)))))

    (testing "json body with bigdecimals"
      (let [resp (handler (-> (request :get "/" {"id" 3})
                              (content-type "application/json; charset=UTF-8")
                              (body "{\"foo\": 5.5}")))]
        (is (decimal? (get-in resp [:params :foo])))
        (is (decimal? (get-in resp [:json-params "foo"])))
        (is (= {:id "3" :foo 5.5M} (:params resp)))
        (is (= {"foo" 5.5M} (:json-params resp)))))

    (testing "custom json body"
      (let [resp (handler (-> (request :get "/" {"id" 3})
                              (content-type "application/vnd.foobar+json; charset=UTF-8")
                              (body "{\"foo\": \"bar\"}")))]
        (is (= {:id "3", :foo "bar"} (:params resp)))
        (is (= {"foo" "bar"} (:json-params resp)))))

    (testing "json schema body"
      (let [resp (handler (-> (request :get "/" {"id" 3})
                              (content-type "application/schema+json; charset=UTF-8")
                              (body "{\"type\": \"schema\",\"properties\":{}}")))]
        (is (= {:id "3", :type "schema", :properties {}} (:params resp)))
        (is (= {"type" "schema", "properties" {}} (:json-params resp)))))

    (testing "array json body"
      (let [resp (handler (-> (request :get "/" {"id" 3})
                              (content-type "application/vnd.foobar+json; charset=UTF-8")
                              (body "[\"foo\"]")))]
        (is (= {:id "3"} (:params resp)))))

    (testing "malformed json"
      (let [resp (handler (-> (request :get "/" {"id" 3})
                              (content-type "application/vnd.foobar+json; charset=UTF-8")
                              (body "{\"foo\": \"bar\"")))]
        (is (= resp 
               {:status  400
                :headers {"Content-Type" "text/plain"}
                :body    "Malformed JSON in request body."})))))

  (testing "custom malformed json"
    (let [malformed {:status 400
                     :headers {"Content-Type" "text/html"}
                     :body "<b>Your JSON is wrong!</b>"}
          handler (wrap-json-params identity {:malformed-response malformed})
          resp (handler (-> (request :get "/" {"id" 3})
                            (content-type "application/vnd.foobar+json; charset=UTF-8")
                            (body "{\"foo\": \"bar\"")))]
      (is (= resp malformed)))))

(deftest test-json-response
  (testing "map body"
    (let [handler (constantly (response {"foo" "bar"})) 
          resp    ((wrap-json-response handler) {})]
      (is (= (get-in resp [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (:body resp) "{\"foo\":\"bar\"}"))))

  (testing "string body"
    (let [handler (constantly (response "foobar"))
          resp    ((wrap-json-response handler) {})]
      (is (= (:headers resp) {}))
      (is (= (:body resp) "foobar"))))

  (testing "vector body"
    (let [handler (constantly (response [:foo :bar]))
          resp    ((wrap-json-response handler) {})]
      (is (= (get-in resp [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (:body resp) "[\"foo\",\"bar\"]"))))
  
  (testing "list body"
    (let [handler (constantly (response '(:foo :bar)))
          resp    ((wrap-json-response handler) {})]
      (is (= (get-in resp [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (:body resp) "[\"foo\",\"bar\"]"))))
  
  (testing "set body"
    (let [handler (constantly (response #{:foo :bar}))
          resp    ((wrap-json-response handler) {})]
      (is (= (get-in resp [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (or (= (:body resp) "[\"foo\",\"bar\"]")
              (= (:body resp) "[\"bar\",\"foo\"]")))))

  (testing "JSON options"
    (let [handler (constantly (response {:foo "bar" :baz "quz"}))
          resp    ((wrap-json-response handler {:pretty true}) {})]
      (is (or (= (:body resp) "{\n  \"foo\" : \"bar\",\n  \"baz\" : \"quz\"\n}")
              (= (:body resp) "{\n  \"baz\" : \"quz\",\n  \"foo\" : \"bar\"\n}")))))

  (testing "don’t overwrite Content-Type if already set"
    (let [handler (constantly (header (response {:foo "bar"}) "Content-Type" "application/json; some-param=some-value"))
          resp    ((wrap-json-response handler) {})]
      (is (= (get-in resp [:headers "Content-Type"]) "application/json; some-param=some-value"))
      (is (= (:body resp) "{\"foo\":\"bar\"}")))))