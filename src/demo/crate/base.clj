(ns demo.crate.base
  (:require [pallet.actions                    :as actions]
            [pallet.api                        :as api]
            [pallet.crate.automated-admin-user :as aau]
            [pallet.crate.upstart              :as upstart]
            [pallet.crate                      :refer [defplan]]))

(defplan base-install
  []
  (actions/package "tmux")
  (actions/package "htop")
  (actions/package "sysstat")
  (actions/package "tcpdump")
  (actions/package "htop")
  (actions/package "python-pip"))

(defplan base-bootstrap
  []
  (aau/automated-admin-user)
  (actions/package-manager :update))

(defn server-spec
  ([]
     (server-spec {}))
  ([settings]
     (api/server-spec
      :phases {:install   (api/plan-fn
                           (base-install)
                           (upstart/configure {}))
               :bootstrap base-bootstrap})))