(ns ring.middleware.json
  (:use ring.util.response)
  (:require [cheshire.core :as json]))

(defn- json-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/(vnd.+)?json" type)))))

(defn wrap-json-params
  "Middleware that converts request bodies in JSON format to a map of
  parameters, which is added to the request map on the :json-params and
  :params keys."
  [handler]
  (fn [request]
    (if-let [body (and (json-request? request) (:body request))]
      (let [params (json/parse-string (slurp body))]
        (handler
         (-> request
             (assoc :json-params params)
             (update-in [:params] merge params))))
      (handler request))))

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