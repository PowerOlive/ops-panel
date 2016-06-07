(ns ops-panel.core
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [ajax.core :refer (GET)]
            [datascript.core :as d]
            [posh.core :as p]
            [reagent.core :as r]))

(defonce conn (let [schema {:user/whitelisted-ip {:db/cardinality :db.cardinality/many}}
                    conn (d/create-conn schema)]
                (d/transact! conn [{:db/id 0
                                    :counter/value 0
                                    :user/whitelisted-ip []}])
                (p/posh! conn)
                conn))

(defn refresh-whitelist! []
  (js/setTimeout refresh-whitelist! 1000)
  (GET "/whitelist-ip"
      :format :text
      :handler (fn [resp]
                 (let [v (cljs.reader/read-string resp)]
                   (p/transact!
                    conn
                    [{:db/id 0
                      :user/whitelisted-ip (if (vector? v) v [])}])))
      :error-handler (fn [{:keys [status status-text]}]
                       (.log js/console (str "Error trying to whitelist IP: " status " " status-text)))))

(defn whitelist-display []
  (let [whitelisted-ips @(p/q conn
                              '[:find (?ip ...)
                                :where [0 :user/whitelisted-ip ?ip]])]
    [:div (if (= (count whitelisted-ips) 0)
            "You have no IPs whitelisted yet..."
            [:div "You have the following IPs whitelisted:"
             [:ul
              (for [ip whitelisted-ips]
                ^{:key ip} [:li ip])]])]))

(defn app []
  [whitelist-display])

(defn ^:export main []
  (refresh-whitelist!)
  (r/render [app]
            (js/document.getElementById "app_container")))
