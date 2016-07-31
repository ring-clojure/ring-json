# Ring-JSON

[![Build Status](https://travis-ci.org/ring-clojure/ring-json.svg?branch=master)](https://travis-ci.org/ring-clojure/ring-json)

Standard Ring middleware functions for handling JSON requests and
responses.

## Installation

To install, add the following to your project `:dependencies`:

    [ring/ring-json "0.4.0"]


## Usage

#### wrap-json-response

The `wrap-json-response` middleware will convert any response with a
collection as a body (e.g. map, vector, set, seq, etc) into JSON:

```clojure
(require '[ring.middleware.json :refer [wrap-json-response]]
         '[ring.util.response :refer [response]])

(defn handler [request]
  (response {:foo "bar"}))

(def app
  (wrap-json-response handler))
```


#### wrap-json-body

The `wrap-json-body` middleware will parse the body of any request
with a JSON content-type into a Clojure data structure, and assign it
to the `:body` key.

This is the preferred way of handling JSON requests.

```clojure
(use '[ring.middleware.json :only [wrap-json-body]]
     '[ring.util.response :only [response]])

(defn handler [request]
  (prn (get-in request [:body "user"]))
  (response "Uploaded user."))

(def app
  (wrap-json-body handler {:keywords? true :bigdecimals? true}))
```


##### Transforming JSON map keys

Both `wrap-json-response` and `wrap-json-body` support the `:key-fn` option
which takes a transform function in order to customize the JSON map keys:

```clojure
(use '[ring.middleware.json :only [wrap-json-body]]
     '[ring.util.response :only [response]])

(defn handler [request]
  (prn (get-in request [:body "user"]))
  (response "Uploaded user."))

(def app
  (wrap-json-body handler {:key-fn clojure.string/lower-case}))
```

Note: for `wrap-json-body`, both `:key-fn` and `:keywords?` can be supplied at the same time. 


#### wrap-json-params

The `wrap-json-params` middleware is an alternative to
`wrap-json-body` for when it's convenient to treat a JSON request as a
map of parameters. Rather than replace the `:body` key, the parsed
data structure will be assigned to the `:json-params`. The parameters
will also be merged into the standard `:params` map.

```clojure
(require '[ring.middleware.json :refer [wrap-json-params]]
         '[ring.util.response :refer [response]])

(defn handler [request]
  (prn (get-in request [:params "user"]))
  (response "Uploaded user."))

(def app
  (wrap-json-params handler))
```

Note that Ring parameter maps use strings for keys. For consistency,
this means that `wrap-json-params` does not have a `:keywords?`
option. Instead, use the standard Ring `wrap-keyword-params` function:

```clojure
(require '[ring.middleware.keyword-params :refer [wrap-keyword-params]])

(def app
  (-> handler
      wrap-keyword-params
      wrap-json-params))
```


## License

Copyright © 2015 James Reeves

Distributed under the MIT License, the same as Ring.
