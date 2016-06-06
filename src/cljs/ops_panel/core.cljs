(ns ops-panel.core
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [datascript.core :as d]
            [posh.core :as p]
            [reagent.core :as r]
            [taoensso.sente  :as sente :refer (cb-success?)]))

;; sente
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defonce conn (let [schema {:user/whitelisted-ip {:db/cardinality :db.cardinality/many}}
                    conn (d/create-conn schema)]
                (d/transact! conn [{:db/id 0
                                    :counter/value 0
                                    :user/whitelisted-ip []}])
                (p/posh! conn)
                conn))

(defn refresh-whitelist! []
  (js/setTimeout refresh-whitelist! 1000)
  (chsk-send! [:ops/whitelist-ip nil]
              5000
              (fn [resp]
                (p/transact! conn
                             [{:db/id 0
                               :user/whitelisted-ip (if (vector? resp) resp [])}]))))

(defn whitelist-display []
  (let [whitelisted-ips @(p/q conn
                              '[:find (?ip ...)
                                :where [0 :user/whitelisted-ip ?ip]])]
    [:div (if (= (count whitelisted-ips) 0)
            "You have no IPs whitelisted yet!"
            [:div "You have the following IPs whitelisted:"
             [:ul
              (for [ip whitelisted-ips]
                ^{:key ip} [:li ip])]])]))

(defn sente-test []
  (let [counter-value @(p/q conn
                            '[:find ?c .
                              :where [0 :counter/value ?c]])]
    [:div
      [:h3 "Sente test"]
      (str "Current value is " counter-value)
      [:div
       [:input {:type "button"
                :value "Click me NOW"
                :on-click #(chsk-send! [:test/inc counter-value]
                                       5000
                                       (fn [resp]
                                         (p/transact! conn [{:db/id 0
                                                             :counter/value resp}])))}]]]))
(defn app []
  [:div
   [whitelist-display]
   [sente-test]])

(defn ^:export main []
  (refresh-whitelist!)
  (r/render [app]
            (js/document.getElementById "app_container")))
