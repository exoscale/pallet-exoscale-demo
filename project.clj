(defproject pallet-exoscale-demo "0.1.0"
  :description "quickstart for pallet-exoscale"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.palletops/pallet "0.8.0-beta.10"]
                 [ch.exoscale/pallet-exoscale "0.1.7"]
                 [com.palletops/upstart-crate "0.8.0-alpha.2"]
                 [org.clojure/data.json       "0.2.2"]
                 [compojure                   "1.1.5"]
                 [ring/ring-core              "1.2.0-beta2"]
                 [ring/ring-jetty-adapter     "1.2.0-beta2"]
                 [ring/ring-json              "0.2.0"]
                 [org.slf4j/slf4j-log4j12     "1.6.4"]
                 [org.slf4j/slf4j-api         "1.6.4"]
                 [log4j/log4j                 "1.2.16"
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jdmk/jmxtools
                               com.sun.jmx/jmxri]]]
  :main demo.core
  :profiles {:dev
             {:dependencies
              [[com.palletops/pallet "0.8.0-beta.10" :classifier "tests"]]}}
  :local-repo-classpath true)
