(defproject lambdacd-cron "0.1.2-SNAPSHOT"
  :description "A cron for your lambdacd"
  :url "https://github.com/felixb/lambdacd-cron"
  :license {:name "Apache License, version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [lambdacd "0.8.0"]]
  :test-paths ["test" "example"]
  :profiles {:dev {:main         lambdacd-cron.example.pipeline
                   :dependencies [[compojure "1.1.8"]
                                  [ring-server "0.3.1"]
                                  [ring/ring-mock "0.2.0"]]}})
