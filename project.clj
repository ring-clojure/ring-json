(defproject ring/ring-json "0.4.0"
  :description "Ring middleware for handling JSON"
  :url "https://github.com/ring-clojure/ring-json"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.6.3"]
                 [ring/ring-core "1.6.0-beta6"]]
  :plugins [[codox "0.8.13"]]
  :aliases {"test-all" ["with-profile" "default:+1.6:+1.7:+1.8" "test"]}
  :profiles
  {:1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}})
