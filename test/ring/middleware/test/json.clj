(ns ring.middleware.test.json
  (:use ring.middleware.json
        clojure.test
        ring.util.io))

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

    (testing "custom json body"
      (let [request  {:content-type "application/vnd.foobar+json; charset=UTF-8"
                      :body (string-input-stream "{\"foo\": \"bar\"}")
                      :params {"id" 3}}
            response (handler request)]
        (is (= {"id" 3, "foo" "bar"} (:params response)))
        (is (= {"foo" "bar"} (:json-params response)))))))

(deftest test-json-response
  (let [handler  (constantly {:status 200 :headers {} :body {:foo "bar"}})
        response ((wrap-json-response handler) {})]
    (is (= (get-in response [:headers "Content-Type"]) "application/json"))
    (is (= (:body response) "{\"foo\":\"bar\"}"))))