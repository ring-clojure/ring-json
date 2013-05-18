(defproject ring/ring-json "0.2.1"
  :description "Ring middleware for handling JSON"
  :url "https://github.com/ring-clojure/ring-json"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [cheshire "5.0.2"]
                 [ring/ring-core "1.1.8"]]
  :profiles
  {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.0-RC16"]]}})
