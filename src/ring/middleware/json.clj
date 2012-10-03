(ns ring.middleware.json
  (:use ring.util.response)
  (:require [cheshire.core :as json]))

(defn- json-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/(vnd.+)?json" type)))))

(defn- read-json [request & [keywords?]]
  (if (json-request? request)
    (if-let [body (:body request)]
      (json/parse-string (slurp body) keywords?))))

(defn wrap-json-body
  "Middleware that parses the :body of JSON requests into a Clojure data
  structure."
  [handler & [{:keys [keywords?]}]]
  (fn [request]
    (if-let [json (read-json request keywords?)]
      (handler (assoc request :body json))
      (handler request))))

(defn wrap-json-params
  "Middleware that converts request bodies in JSON format to a map of
  parameters, which is added to the request map on the :json-params and
  :params keys."
  [handler & [{:keys [keywords?]}]]
  (fn [request]
    (if-let [json (read-json request keywords?)]
      (handler (-> request
                   (assoc :json-params json)
                   (update-in [:params] merge json)))
      (handler request))))

(defn wrap-json-response
  "Middleware that converts responses with a map or a vector for a body into a
  JSON response."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (or (map? (:body response))
              (vector? (:body response)))
        (-> response
            (content-type "application/json")
            (update-in [:body] json/generate-string))
        response))))