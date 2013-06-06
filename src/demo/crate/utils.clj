(ns demo.crate.utils
  (:require [pallet.node    :refer [primary-ip]]
            [pallet.crate   :refer [defplan nodes-with-role]]
            [clojure.string :refer [join]]))

(defn format-first-node
  ([fmt role]
     (if-let [node (first (nodes-with-role role))]
       (->> node
            :node
            primary-ip
            (format fmt))
       ""))
  ([role]
     (format-first-node "%s" role)))

(defn format-nodes
  [fmt role]
  (->> (nodes-with-role role)
       (map :node)
       (map primary-ip)
       (map (partial format fmt))))