# Ring-JSON

Standard Ring middleware functions for handling JSON requests and
responses.

## Installation

To install, add the following to your project `:dependencies`:

    [ring/ring-json "0.1.0"]

## Usage

The `wrap-json-response` middleware will convert any response with a
map as a body into JSON:

```clojure
(use 'ring.middleware.json
     'ring.util.response)

(defn handler [request]
  (response {:foo "bar"}))

(def app
  (wrap-json-response handler))
```

The `wrap-json-params` middleware will parse any request with a JSON
content-type and body and merge the resulting parameters into a params
map:

```clojure
(use 'ring.middleware.json)

(defn handler [request]
  (prn (get-in request [:params "user"]))
  (response "Uploaded user."))

(def app
  (wrap-json-params handler))
```

## License

Copyright © 2012 James Reeves

Distributed under the MIT License, the same as Ring.
