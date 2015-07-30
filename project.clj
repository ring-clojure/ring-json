(defproject ring/ring-json "0.4.0"
  :description "Ring middleware for handling JSON"
  :url "https://github.com/ring-clojure/ring-json"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.5.0"]
                 [ring/ring-core "1.4.0"]]
  :plugins [[codox "0.8.13"]]
  :profiles
  {:1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}})
