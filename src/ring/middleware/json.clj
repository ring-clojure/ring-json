(ns ring.middleware.json
  "Ring middleware for parsing JSON requests and generating JSON responses."
  (:require [cheshire.core :as json]
            [cheshire.parse :as parse]
            [clojure.java.io :as io]
            [ring.core.protocols :as ring-protocols]
            [ring.util.io :as ring-io]
            [ring.util.response :refer [content-type]]
            [ring.util.request :refer [character-encoding]])
  (:import [java.io InputStream]))

(defn- json-request? [request]
  (if-let [type (get-in request [:headers "content-type"])]
    (not (empty? (re-find #"^application/(.+\+)?json" type)))))

(defn- read-json [request & [{:keys [keywords? bigdecimals? key-fn]}]]
  (if (json-request? request)
    (if-let [^InputStream body (:body request)]
      (let [^String encoding (or (character-encoding request)
                                 "UTF-8")
            body-reader (java.io.InputStreamReader. body encoding)]
        (binding [parse/*use-bigdecimals?* bigdecimals?]
          (try
            [true (json/parse-stream body-reader (or key-fn keywords?))]
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
  [request options]
  (if-let [[valid? json] (read-json request options)]
    (if valid? (assoc request :body json))
    request))

(defn wrap-json-body
  "Middleware that parses the body of JSON request maps, and replaces the :body
  key with the parsed data structure. Requests without a JSON content type are
  unaffected.

  Accepts the following options:

  :key-fn             - function that will be applied to each key
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
  (if (map? json)
    (-> request
        (assoc :json-params json)
        (update-in [:params] merge json))
    request))

(defn json-params-request
  "Parse the body of JSON requests into a map of parameters, which are added
  to the request map on the :json-params and :params keys. Returns nil if the
  JSON is malformed. See: wrap-json-params."
  [request options]
  (if-let [[valid? json] (read-json request options)]
    (if valid? (assoc-json-params request json))
    request))

(defn wrap-json-params
  "Middleware that parses the body of JSON requests into a map of parameters,
  which are added to the request map on the :json-params and :params keys.

  Accepts the following options:

  :key-fn             - function that will be applied to each key
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

(defrecord JsonStreamingResponseBody [body options]
  ring-protocols/StreamableResponseBody
  (write-body-to-stream [_ _ output-stream]
    (json/generate-stream body (io/writer output-stream) options)))

(defn json-response
  "Converts responses with a map or a vector for a body into a JSON response.
  See: wrap-json-response."
  [response options]
  (if (coll? (:body response))
    (let [generator (if (:stream? options)
                      ->JsonStreamingResponseBody
                      json/generate-string)
          options (dissoc options :stream?)
          json-resp (update-in response [:body] generator options)]
      (if (contains? (:headers response) "Content-Type")
        json-resp
        (content-type json-resp "application/json; charset=utf-8")))
    response))

(defn wrap-json-response
  "Middleware that converts responses with a map or a vector for a body into a
  JSON response.

  Accepts the following options:

  :key-fn            - function that will be applied to each key
  :pretty            - true if the JSON should be pretty-printed
  :escape-non-ascii  - true if non-ASCII characters should be escaped with \\u
  :stream?           - true to create JSON body as stream rather than string"
  {:arglists '([handler] [handler options])}
  [handler & [{:as options}]]
  (fn
    ([request]
     (json-response (handler request) options))
    ([request respond raise]
     (handler request (fn [response] (respond (json-response response options))) raise))))
