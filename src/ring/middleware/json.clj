(ns ring.middleware.json
  "Ring middleware for parsing JSON requests and generating JSON responses."
  (:require [cheshire.core :as json]
            [cheshire.parse :as parse]
            [ring.util.response :refer [content-type]]))

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn- read-json [request & [{:keys [keywords? bigdecimals?]}]]
  (if (json-request? request)
    (if-let [body (:body request)]
      (let [body-string (slurp body)]
        (binding [parse/*use-bigdecimals?* bigdecimals?]
          (try
            [true (json/parse-string body-string keywords?)]
            (catch com.fasterxml.jackson.core.JsonParseException ex
              [false nil])))))))

(def ^{:doc "The default response to return when a JSON request is malformed."}
  default-malformed-response
  {:status  400
   :headers {"Content-Type" "text/plain"}
   :body    "Malformed JSON in request body."})

(defn json-body-request
  "Parse a JSON request body and assoc it back into the :body key. Returns nil
  if the JSON is malformed. See: wrap-json-body."
  [request {:keys [keywords? bigdecimals?]}]
  (if-let [[valid? json] (read-json request {:keywords? keywords? :bigdecimals? bigdecimals?})]
    (if valid? (assoc request :body json))
    request))

(defn wrap-json-body
  "Middleware that parses the body of JSON request maps, and replaces the :body
  key with the parsed data structure. Requests without a JSON content type are
  unaffected.

  Accepts the following options:

  :keywords?          - true if the keys of maps should be turned into keywords
  :bigdecimals?       - true if BigDecimals should be used instead of Doubles
  :malformed-response - a response map to return when the JSON is malformed"
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [malformed-response]
               :or {malformed-response default-malformed-response}
               :as options}]]
  (fn
    ([request]
     (if-let [request (json-body-request request options)]
       (handler request)
       malformed-response))
    ([request respond raise]
     (if-let [request (json-body-request request options)]
       (handler request respond raise)
       (respond malformed-response)))))

(defn- assoc-json-params [request json]
  (cond-> request
    (map? json)
    (update-in [:params] merge json)

    json
    (assoc :json-params json)

    :else
    identity))

(defn json-params-request
  "Parse the body of JSON requests into a map of parameters, which are added
  to the request map on the :json-params and :params keys. Returns nil if the
  JSON is malformed. See: wrap-json-params."
  [request {:keys [bigdecimals?]}]
  (if-let [[valid? json] (read-json request {:bigdecimals? bigdecimals?})]
    (if valid? (assoc-json-params request json))
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
  [handler & [{:keys [malformed-response]
               :or {malformed-response default-malformed-response}
               :as options}]]
  (fn
    ([request]
     (if-let [request (json-params-request request options)]
       (handler request)
       malformed-response))
    ([request respond raise]
     (if-let [request (json-params-request request options)]
       (handler request respond raise)
       (respond malformed-response)))))

(defn json-response
  "Converts responses with a map or a vector for a body into a JSON response.
  See: wrap-json-response."
  [response options]
  (if (coll? (:body response))
    (let [json-resp (update-in response [:body] json/generate-string options)]
      (if (contains? (:headers response) "Content-Type")
        json-resp
        (content-type json-resp "application/json; charset=utf-8")))
    response))

(defn wrap-json-response
  "Middleware that converts responses with a map or a vector for a body into a
  JSON response.

  Accepts the following options:

  :pretty            - true if the JSON should be pretty-printed
  :escape-non-ascii  - true if non-ASCII characters should be escaped with \\u"
  {:arglists '([handler] [handler options])}
  [handler & [{:as options}]]
  (fn
    ([request]
     (json-response (handler request) options))
    ([request respond raise]
     (handler request (fn [response] (respond (json-response response options))) raise))))
