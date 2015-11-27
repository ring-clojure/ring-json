(ns ring.middleware.json
  "Ring middleware for parsing JSON requests and generating JSON responses."
  (:require [cheshire.core :as json]
            [cheshire.parse :as parse]
            [ring.util.response :refer [content-type]]))

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn- ->key-fn [key-fn keywords?]
  (if (fn? key-fn)
    (if keywords?
      #(-> % key-fn keyword)
      key-fn)
    keywords?))

(defn- read-json [request & [{:keys [key-fn keywords? bigdecimals?]}]]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (binding [parse/*use-bigdecimals?* bigdecimals?]
          (try
            [true (json/parse-string body-string (->key-fn key-fn keywords?))]
            (catch com.fasterxml.jackson.core.JsonParseException ex
              [false nil])))))))

(def ^{:doc "The default response to return when a JSON request is malformed."}
  default-malformed-response
  {:status  400
   :headers {"Content-Type" "text/plain"}
   :body    "Malformed JSON in request body."})

(defn wrap-json-body
  "Middleware that parses the body of JSON request maps, and replaces the :body
  key with the parsed data structure. Requests without a JSON content type are
  unaffected.

  Accepts the following options:

  :key-fn             - a function to transform the keys of maps
  :keywords?          - true if the keys of maps should be turned into keywords
  :bigdecimals?       - true if BigDecimals should be used instead of Doubles
  :malformed-response - a response map to return when the JSON is malformed"
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [key-fn keywords? bigdecimals? malformed-response]
               :or {malformed-response default-malformed-response}}]]
  (fn [request]
    (if-let [[valid? json]
             (read-json request {:key-fn key-fn :keywords? keywords? :bigdecimals? bigdecimals?})]
      (if valid?
        (handler (assoc request :body json))
        malformed-response)
      (handler request))))

(defn- assoc-json-params [request json]
  (if (map? json)
    (-> request
        (assoc :json-params json)
        (update-in [:params] merge json))
    request))

(defn wrap-json-params
  "Middleware that parses the body of JSON requests into a map of parameters,
  which are added to the request map on the :json-params and :params keys.

  Accepts the following options:

  :bigdecimals?       - true if BigDecimals should be used instead of Doubles
  :malformed-response - a response map to return when the JSON is malformed

  Use the standard Ring middleware, ring.middleware.keyword-params, to
  convert the parameters into keywords."
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [bigdecimals? malformed-response]
               :or {malformed-response default-malformed-response}}]]
  (fn [request]
    (if-let [[valid? json] (read-json request {:bigdecimals? bigdecimals?})]
      (if valid?
        (handler (assoc-json-params request json))
        malformed-response)
      (handler request))))

(defn wrap-json-response
  "Middleware that converts responses with a map or a vector for a body into a
  JSON response.

  Accepts the following options:

  :pretty            - true if the JSON should be pretty-printed
  :escape-non-ascii  - true if non-ASCII characters should be escaped with \\u
  :key-fn            - a function to transform the keys of maps in the JSON"
  {:arglists '([handler] [handler options])}
  [handler & [{:as options}]]
  (fn [request]
    (let [response (handler request)]
      (if (coll? (:body response))
        (let [json-response (update-in response [:body] json/generate-string options)]
          (if (contains? (:headers response) "Content-Type")
            json-response
            (content-type json-response "application/json; charset=utf-8")))
        response))))
