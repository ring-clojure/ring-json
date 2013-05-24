(ns ring.middleware.json
  (:use ring.util.response)
  (:require [cheshire.core :as json]
            [cheshire.parse :as parse])
  (:import (com.fasterxml.jackson.core JsonParseException)))

(defn- json-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn- read-json [request & [keywords?]]
  (if (json-request? request)
    (if-let [body (:body request)]
      (json/parse-string (slurp body) keywords?))))

(defn- json-body
  [request]
  (if (json-request? request)
    (if-let [body (:body request)]
      body)))

(defn- wrap-malformed-json
  [handler & [{:keys [keywords? handle-malformed]}]]
  (fn [request]
    (try
      (if-let [body (json-body request)]
        (assoc request :body (json/parse-string (slurp body) keywords?)))
      (catch JsonParseException e
        (let [message (.getMessage e)]
          (if handle-malformed
            (handle-malformed {:message message})
            {:status 400
             :content-type "application/json"
             :body (json/generate-string
                     {:error message})})))
      (finally
        (handler request)))))

(defn wrap-json-body
  "Middleware that parses the :body of JSON requests into a Clojure data
  structure."
  [handler & [{:keys [keywords? bigdecimals? handle-malformed]}]]
  (fn [request]
    (binding [parse/*use-bigdecimals?* bigdecimals?]
      (if-let [body (json-body request)]
        (let [h (-> handler
                  (wrap-malformed-json {:keywords? keywords?
                                        :handle-malformed handle-malformed}))]
          (h (assoc request :body body)))
        (handler request)))))

(defn wrap-json-params
  "Middleware that converts request bodies in JSON format to a map of
  parameters, which is added to the request map on the :json-params and
  :params keys."
  [handler & [{:keys [bigdecimals?]}]]
  (fn [request]
    (binding [parse/*use-bigdecimals?* bigdecimals?]
      (let [json (read-json request)]
        (if (and json (map? json))
          (handler (-> request
                       (assoc :json-params json)
                       (update-in [:params] merge json)))
          (handler request))))))

(defn wrap-json-response
  "Middleware that converts responses with a map or a vector for a body into a
  JSON response. Accepts the following options:
    :pretty            - when true, pretty-print the JSON
    :escape-non-ascii  - when true, non-ASCII characters are escaped with \\u"
  [handler & [{:as options}]]
  (fn [request]
    (let [response (handler request)]
      (if (coll? (:body response))
        (let [json-response (update-in response [:body] json/generate-string options)]
          (if (contains? (:headers response) "Content-Type")
            json-response
            (content-type json-response "application/json; charset=utf-8")))
        response))))
