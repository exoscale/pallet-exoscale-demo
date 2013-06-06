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

;; works on both vmfest and exoscale
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

(def bundle-topology
  {:bundle {:roles [:redis :shorten :nginx]
            :size  1}})

(def split-topology
  {:lb     {:roles [:nginx]
            :size 1}
   :db     {:roles [:redis]
            :size 1}
   :web    {:roles [:shorten]
            :size  3}})

(def service-configs
  (delay (-> (configure/pallet-config) :services)))

(def services
  (delay
   (->> (for [[service config] (:services (configure/pallet-config))]
          [service (configure/compute-service-from-map config)])
        (reduce merge {}))))

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
  [service-name topology-config phases]
  (let [service (service-name @services)
        topology (cluster-topology topology-config)]
    (when-not service
      (throw (ex-info "need a service configuration for: " service-name)))
    (api/converge topology :compute service :phase phases)))


;; HTTP Service

(defn format-phase-details
  [result]
  (let [succeeded? (comp (partial = 0) :exit)
        failed?    (complement succeeded?)]
    {:total   (count result)
     :failed  (count (filter failed? result))
     :success (count (filter succeeded? result))
     :results (for [r result]
                (select-keys r [:exit :out]))}))

(defn format-details
  [res]
  (let [phases (for [{:keys [phase result]} (:results res)
                     :let [details (format-phase-details result)]]
                 {:phase   phase
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
   (GET "/api/topologies"
        request
        (response
         {:vmfest bundle-topology
          :exoscale  split-topology}))
   (GET "/api/services"
        request
        (response @service-configs))
   (GET "/api/services/:service-name/nodes"
        {{:keys [service-name]} :params}
        (response
         (->> (compute/nodes (get @services (keyword service-name)))
              (map node/node-map))))
   (PUT "/api/services/:service-name/topology"
        {{:keys [service-name]} :params
         {:keys [topology phases] :as body} :body}
        (response (-> (cluster (keyword service-name)
                               (sanitize topology)
                               (map keyword phases))
                      (format-details))))))

(defn -main
  [& _]
  (println "Welcome to exoscale deployer!")
  (println "Please visit http://localhost:8080/")
  (let [app (-> (api (handler))
                (wrap-json-response)
                (wrap-json-body {:keywords? true})
                (wrap-resource "assets"))]
    (run-jetty app {:host "127.0.0.1" :port 8080})
    (browse-url "http://localhost:8080/")))