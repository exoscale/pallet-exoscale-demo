(ns demo.crate.shorten
  (:require [pallet.crate         :refer [defplan assoc-settings get-settings]]
            [pallet.crate.service :refer [service-supervisor-config service]]
            [demo.crate.utils     :refer [format-first-node]]
            [clojure.string       :refer [join] :as s]
            [pallet.actions       :as actions]
            [pallet.api           :as api]
            [pallet.crate.upstart :as upstart]
            [demo.crate.base      :as base]))

(def default-settings
  {:riemann_host "127.0.0.1"
   :riemann_port 5555
   :log_file_path "/var/log/shorten-web.log"
   :log_level "INFO"
   :gunicorn_path "/usr/local/bin/gunicorn"
   :gunicorn_log_file "/var/log/shorten-gunicorn.log"
   :gunicorn_worker_count 2
   :listen_host "0.0.0.0"
   :listen_port 8080
   :redis_host "127.0.0.1"
   :redis_port 6379})

(defn shorten-env
  [settings]
  (map (comp (partial apply format "%s='%s'")
             (juxt (comp s/upper-case name key) val))
       settings))

(defplan shorten-settings
  [settings]
  (let [settings (-> (merge default-settings settings)
                     (assoc :redis_host (format-first-node :redis)))
        jobfmt   "%s -b '%s:%s' -w %s --log-file='%s' --log-level='%s' %s:app"]
    (assoc-settings :shorten settings)
    (upstart/job "url-shortener"
                 {:exec (format jobfmt
                                (:gunicorn_path settings)
                                (:listen_host settings)
                                (:listen_port settings)
                                (:gunicorn_worker_count settings)
                                (:gunicorn_log_file settings)
                                (:log_level settings)
                                "url_shortener")
                  :respawn true
                  :env (shorten-env settings)}
                 {})))

(defplan shorten-configure
  []
  (let [settings (get-settings :shorten)]
    (actions/exec-checked-script "install_pip_module"
                                 ("pip" "install" "url-shortener"))
    (service {:service-name "url-shortener"
              :supervisor :upstart}
             {:action :restart})))

(defn server-spec
  ([]
     (server-spec {}))
  ([settings]
     (api/server-spec
      :roles [:shorten]
      :extends [(base/server-spec) (upstart/server-spec {})]
      :phases {:settings (api/plan-fn
                          (shorten-settings settings))
               :install   shorten-configure
               :configure shorten-configure
               :deploy    shorten-configure})))