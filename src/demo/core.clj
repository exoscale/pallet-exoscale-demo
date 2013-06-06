(ns demo.core
  (:gen-class)
  (:require [ring.adapter.jetty       :refer [run-jetty]]
            [ring.util.response       :refer [response header redirect]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json     :refer [wrap-json-response
                                              wrap-json-body]]
            [compojure.core           :refer [GET POST DELETE PUT routes]]
            [compojure.handler        :refer [api]]
            [clojure.java.browse      :refer [browse-url]]
            [pallet.configure         :as configure]
            [pallet.compute           :as compute]
            [pallet.api               :as api]
            [pallet.actions           :as actions]
            [pallet.crate             :as crate]
            [pallet.node              :as node]
            [demo.crate.redis         :as redis]
            [demo.crate.nginx         :as nginx]
            [demo.crate.shorten       :as shorten]))

(def node-spec
  (api/node-spec
   :network  {:inbound-ports [22 80 6379 8080]}
   :image    {:os-family :ubuntu
              :os-version-matches "12.04"}
   :hardware {:min-cores 1
              :min-disk 10
              :min-ram 512}))

(def role-specs
  {:nginx   #(nginx/server-spec   {:vhosts    {:shorten {:port     80
                                                         :upstream :shorten
                                                         :role     :shorten}}
                                   :upstreams {:shorten {:role     :shorten
                                                         :port     8080}}})
   :redis   #(redis/server-spec   {:bind "0.0.0.0"})
   :shorten #(shorten/server-spec {:listen_host "0.0.0.0"})})

(def split-topology
  {:lb     {:roles [:nginx]
            :size 1}
   :db     {:roles [:redis]
            :size 1}
   :web    {:roles [:shorten]
            :size  3}})

(def service
  (delay
   (-> (configure/pallet-config)
       :services
       :exoscale
       (configure/compute-service-from-map))))

(defn cluster-topology
  [topology-config]
  (->> (for [[group-name {:keys [size roles] :or {size 1}}] topology-config]
         [(api/group-spec
           group-name
           :extends (for [role roles] ((get role-specs (keyword role))))
           :node-spec node-spec)
          size])
       (reduce merge {})))

(defn cluster
  [topology-config phases]
  (let [topology (cluster-topology topology-config)]
    (api/converge topology :compute @service :phase phases)))


;; HTTP Service

(defn format-phase-details
  [result]
  (let [succeeded? (comp (partial = 0) :exit)
        failed?    (complement succeeded?)]
    {:total   (count result)
     :failed  (count (filter failed? result))
     :success (count (filter succeeded? result))
     :results (for [r result]
                (select-keys r [:exit :out :context :action-symbol]))}))

(defn format-details
  [res]
  (let [phases (for [{:keys [phase result target]} (:results res)
                     :let [details (format-phase-details result)]]
                 {:phase   phase
                  :hostname (-> target :node node/hostname)
                  :details details
                  :success (= (:total details) (:success details))})]
    {:phases  (vec phases)
     :success (every? :success phases)}))

(defn sanitize
  [topology]
  (->>
   (for [[group-name {:keys [size roles]}] topology]
     [(keyword group-name)
      (hash-map
       :size (if (string? size) (Integer/parseInt size) size)
       :roles (map keyword roles))])
   (reduce merge {})))

(defn handler
  []
  (routes
   (GET "/"
        request
        (redirect "/index.html"))
   (GET "/api/nodes"
        {{:keys [service-name]} :params}
        (response
         (->> (compute/nodes @service)
              (map node/node-map))))
   (GET "/api/topology"
        request
        (response
         split-topology))
   (PUT "/api/topology"
        {{:keys [service-name]} :params
         {:keys [topology phases] :as body} :body}
        (response (-> (cluster (sanitize topology) (map keyword phases))
                      (format-details))))))

(defn -main
  [& args]
  (println "Welcome to exoscale deployer!")
  (println "Please visit http://localhost:8080/")
  (let [app (-> (api (handler))
                (wrap-json-response)
                (wrap-json-body {:keywords? true})
                (wrap-resource "assets"))]
    (run-jetty app {:host "127.0.0.1" :port 8080})
    (browse-url "http://localhost:8080/")))