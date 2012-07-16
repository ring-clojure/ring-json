# Ring-JSON

Standard Ring middleware functions for handling JSON requests and responses.

## Usage

You can use middleware to convert any response with a map as a body
into JSON:

```clojure
(use 'ring.middleware.json-response
     'ring.util.response)

(defn handler [request]
  (response {:foo "bar"}))

(def app
  (wrap-json-response handler))
```

## License

Copyright © 2012 James Reeves

Distributed under the Eclipse Public License, the same as Clojure.
