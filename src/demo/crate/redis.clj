(ns demo.crate.redis
  (:require [pallet.crate         :refer [defplan assoc-settings get-settings]]
            [pallet.crate.service :refer [service]]
            [clojure.string       :refer [join]]
            [pallet.actions       :as actions]
            [pallet.api           :as api]
            [pallet.crate.upstart :as upstart]
            [demo.crate.base      :as base]))

(def default-settings
  {:daemonize "no"
   :pidfile "/var/run/redis/redis-server.pid"
   :port 6379
   :bind "0.0.0.0"
   :timeout 300
   :loglevel "notice"
   :logfile "/var/log/redis/redis-server.log"
   :databases 1
   :rdbcompression "yes"
   :dbfilename "dump.rdb"
   :dir "/var/lib/redis"
   :slave-serve-stale-data "yes"
   :appendonly "no"
   :appendfsync "everysec"
   :no-appendfsync-on-rewrite "no"
   :vm-enabled "no"
   :vm-swap-file "/var/lib/redis/redis.swap"
   :vm-max-memory 0
   :vm-page-size 32
   :vm-pages 134217728
   :vm-max-threads 4
   :hash-max-zipmap-entries 512
   :hash-max-zipmap-value 64
   :list-max-ziplist-entries 512
   :list-max-ziplist-value 64
   :set-max-intset-entries 512
   :activerehashing "yes"})

(defn redis-config
  [config]
  (join "\n" (for [[directive value] config]
               (format "%s %s" (name directive) value))))

(defplan redis-settings
  [settings]
  (assoc-settings :redis (merge default-settings settings))
  (upstart/job "redis"
               {:exec "/usr/bin/redis-server /etc/redis/redis.conf"
                :respawn true}
               {}))

(defplan redis-configure
  []
  (let [settings (get-settings :redis)]
    (actions/remote-file "/etc/redis/redis.conf"
                         :content (redis-config settings))
    (service {:service-name "redis"
              :supervisor   :upstart}
             {:action :restart})))

(defplan redis-install
  []
  (actions/package "redis-server")
  (actions/service "redis-server" :action "stop")
  (redis-configure))

(defn server-spec
  ([]
     (server-spec {}))
  ([settings]
     (api/server-spec
      :roles [:redis]
      :extends [(base/server-spec) (upstart/server-spec {})]
      :phases {:settings  (api/plan-fn (redis-settings settings))
               :install   redis-install
               :configure redis-configure})))