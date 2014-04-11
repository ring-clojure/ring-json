(ns ring.middleware.json
  (:use ring.util.response)
  (:require [cheshire.core :as json]
            [cheshire.parse :as parse]))

(defn- json-request? [request]
  (if-let [type (:content-type request)]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn- read-json [request & [keywords?]]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (try
          [true (json/parse-string body-string keywords?)]
          (catch com.fasterxml.jackson.core.JsonParseException ex
            [false nil]))))))

(def default-malformed-response
  {:status  400
   :headers {"Content-Type" "text/plain"}
   :body    "Malformed JSON in request body."})

(defn wrap-json-body
  "Middleware that parses the :body of JSON requests into a Clojure data
  structure."
  [handler & [{:keys [keywords? bigdecimals? malformed-response]
               :or {malformed-response default-malformed-response}}]]
  (fn [request]
    (binding [parse/*use-bigdecimals?* bigdecimals?]
      (if-let [[valid? json] (read-json request keywords?)]
        (if valid?
          (handler (assoc request :body json))
          malformed-response)
        (handler request)))))

(defn- assoc-json-params [request json]
  (if (map? json)
    (-> request
        (assoc :json-params json)
        (update-in [:params] merge json))
    request))

(defn wrap-json-params
  "Middleware that converts request bodies in JSON format to a map of
  parameters, which is added to the request map on the :json-params and
  :params keys."
  [handler & [{:keys [bigdecimals? malformed-response]
               :or {malformed-response default-malformed-response}}]]
  (fn [request]
    (binding [parse/*use-bigdecimals?* bigdecimals?]
      (if-let [[valid? json] (read-json request)]
        (if valid?
          (handler (assoc-json-params request json))
          malformed-response)
        (handler request)))))

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
