(ns ops-panel.ip-whitelist
  (:require [datomic.api :as d]))

;; XXX: unit tests.

;; XXX: we'll probably need to move this elsewhere as more subsystems use the DB.
(def datomic-uri "datomic:free://localhost:4334/ops")

(def dconn (atom nil))

(defn start! []
  (swap! dconn (d/connect datomic-uri)))

(defn whitelist-ip [github-name ip-address]
  (d/transact @dconn [{:db/id #db/id[:db.part/user]
                       :user/github-name github-name
                       :user/whitelisted-ip ip-address}]))

(defn whitelisted-ips [github-name]
  (d/q '[:find (?ip ...)
       :in $ ?n
       :where [?u :user/github-name ?n]
              [?u :user/whitelisted-ip ?ip]]
     (d/db @dconn)
     github-name))
