# Ring-JSON

[![Build Status](https://secure.travis-ci.org/ring-clojure/ring-json.png)](http://travis-ci.org/ring-clojure/ring-json)

Standard Ring middleware functions for handling JSON requests and
responses.

## Installation

To install, add the following to your project `:dependencies`:

    [ring/ring-json "0.3.1"]

## Usage

The `wrap-json-response` middleware will convert any response with a
collection as a body (e.g. map, vector, set, seq, etc) into JSON:

```clojure
(use '[ring.middleware.json :only [wrap-json-response]]
     '[ring.util.response :only [response]])

(defn handler [request]
  (response {:foo "bar"}))

(def app
  (wrap-json-response handler))
```

The `wrap-json-body` middleware will parse the body of any request
with a JSON content-type into a Clojure data structure:

```clojure
(use '[ring.middleware.json :only [wrap-json-body]]
     '[ring.util.response :only [response]])

(defn handler [request]
  (prn (get-in request [:body "user"]))
  (response "Uploaded user."))

(def app
  (wrap-json-body handler {:keywords? true :bigdecimals? true}))
```


The `wrap-json-params` middleware will parse any request with a JSON
content-type and body and merge the resulting parameters into a params
map:

```clojure
(use '[ring.middleware.json :only [wrap-json-params]]
     '[ring.util.response :only [response]])

(defn handler [request]
  (prn (get-in request [:params "user"]))
  (response "Uploaded user."))

(def app
  (wrap-json-params handler))
```

## License

Copyright © 2014 James Reeves

Distributed under the MIT License, the same as Ring.
