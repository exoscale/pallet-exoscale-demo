 (ns demo.http
  (:gen-class)
  (:require [ring.adapter.jetty       :refer [run-jetty]]
            [ring.util.response       :refer [response header redirect]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.json     :refer [wrap-json-response
                                              wrap-json-body]]
            [demo.core                :refer [services service-configs cluster]]
            [compojure.core           :refer [GET POST DELETE PUT routes]]
            [compojure.handler        :refer [api]]
            [clojure.java.browse      :refer [browse-url]]            
            [pallet.configure         :as configure]
            [pallet.compute           :as compute]
            [pallet.api               :as api]
            [pallet.actions           :as actions]
            [pallet.crate             :as crate]
            [pallet.node              :as node]))


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

(defn routes
  []
  (routes
   (GET "/"
        request
        (redirect "/index.html"))
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
                               topology
                               (map keyword phases))
                      (format-details))))))

(defn -main
  [& _]
  (println "Welcome to exoscale deployer!")
  (println "Please visit http://localhost:8080/")
  (let [app (-> (api handler)
                (wrap-json-response)
                (wrap-json-body {:keywords? true})
                (wrap-resource "assets")
                (wrap-riemann))]
    (run-jetty app {:host "127.0.0.1" :port 8080})
    (browse-url "http://localhost:8080/")))