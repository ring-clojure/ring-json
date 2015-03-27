(defproject ring/ring-json "0.3.1"
  :description "Ring middleware for handling JSON"
  :url "https://github.com/ring-clojure/ring-json"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [cheshire "5.3.1"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-defaults "0.1.4"]]
  :plugins [[codox "0.8.0"]]
  :profiles
  {:dev {:dependencies [[ring/ring-mock "0.2.0"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}})
