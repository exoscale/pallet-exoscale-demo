(ns demo.crate.nginx
  (:require [pallet.actions       :as actions]
            [pallet.node          :as node]
            [pallet.api           :as api]
            [demo.crate.base      :as base]
            [pallet.core.session  :refer [session]]
            [clojure.string       :refer [join]]
            [pallet.crate.service :refer [service]]
            [pallet.crate         :refer [defplan get-settings assoc-settings]]
            [demo.crate.utils     :refer [format-nodes]]))

(defn vhost-config
  [vhost listen upstream port]
  (join "\n"
        ["server {"
         (format "\tlisten %s;" listen)
         (format "\tserver_name %s;" (name vhost))
         "\tlocation / {"
         (format  "\t\tproxy_pass http://%s;" (name upstream))
         "\t}"
         "}"]))

(defplan role-upstream
  [upstream role port]
  (let [upstream  (name upstream)
        serverfmt (format "\tserver %%s:%d;\n" port)
        servers   (->> (format-nodes serverfmt role)
                       (apply str))]
    (actions/remote-file (format "/etc/nginx/conf.d/upstream-%s.conf" upstream)
                         :content (format "upstream %s {\n%s}\n"
                                          upstream servers))))

(defplan upstream-vhost
  [vhost listen upstream port]
  (actions/remote-file (format "/etc/nginx/sites-enabled/%s.conf"
                               (name vhost))
                       :content (vhost-config vhost listen upstream port)))

(defplan nginx-settings
  [settings]
  (assoc-settings :nginx settings))

(defplan nginx-configure
  []
  (let [{:keys [vhosts upstreams] :as settings} (get-settings :nginx)]
    (actions/remote-file "/etc/nginx/sites-enabled/default"
                         :content "")
    (doseq [[upstream {:keys [role port]}] upstreams
            :let [role     (keyword role)
                  upstream (keyword upstream)]]
      (role-upstream upstream role port))
    (doseq [[vhost {:keys [upstream port]}] vhosts
            :let [upstream      (keyword upstream)
                  upstream-port (get-in upstreams [upstream :port])]]
      (upstream-vhost vhost port upstream upstream-port))
    (actions/service "nginx" :action :restart)))

(defplan nginx-install
  []
  (actions/package "nginx")
  (nginx-configure))

(defn server-spec
  ([]
     (server-spec {}))
  ([settings]
     (api/server-spec
      :roles [:nginx]
      :extends [(base/server-spec)]
      :phases {:settings  (api/plan-fn (nginx-settings settings))
               :install   nginx-install
               :configure nginx-configure})))