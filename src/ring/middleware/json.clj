(ns ring.middleware.json
  (:use ring.util.response)
  (:require [cheshire.core :as json]))

(defn wrap-json-response
  "Middleware that converts responses with a map for a body into a JSON
  response."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (map? (:body response))
        (-> response
            (content-type "application/json")
            (update-in [:body] json/generate-string))
        response))))