(ns ring.middleware.test.json
  (:use ring.middleware.json
        clojure.test))

(deftest test-json-response
  (let [handler  (constantly {:status 200 :headers {} :body {:foo "bar"}})
        response ((wrap-json-response handler) {})]
    (is (= (get-in response [:headers "Content-Type"]) "application/json"))
    (is (= (:body response) "{\"foo\":\"bar\"}"))))