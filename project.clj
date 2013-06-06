(defproject pallet-exoscale-demo "0.1.0"
  :description "quickstart for pallet-exoscale"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.palletops/pallet "0.8.0-beta.10"]
                 [ch.qos.logback/logback-classic "1.0.0"]
                 [ch.exoscale/pallet-exoscale "0.1.6"]
                 [com.palletops/pallet-vmfest "0.3.0-alpha.5"]
                 [com.palletops/riemann-crate "0.8.0-SNAPSHOT"]
                 [com.palletops/upstart-crate "0.8.0-alpha.2"]
                 [org.clojure/data.json       "0.2.2"]
                 [compojure                   "1.1.5"]
                 [ring/ring-core              "1.2.0-beta2"]
                 [ring/ring-jetty-adapter     "1.2.0-beta2"]
                 [ring/ring-json              "0.2.0"]]
  :main demo.core
  :profiles {:dev
             {:dependencies
              [[com.palletops/pallet "0.8.0-beta.10" :classifier "tests"]]}}
  :local-repo-classpath true)
