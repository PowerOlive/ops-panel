(ns ops-panel.core
  (:require [clj-ssh.ssh :as ssh]
            [clojure.data.json :as json]
            [clojure.pprint :as pp]
            [com.climate.claypoole :as pool]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [files not-found resources]]
            [digitalocean.v2.core :as do]
            [environ.core :refer [env]]
            [hiccup.core :refer [html]]
            [org.httpkit.client :as http]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn ssh [host cmd]
  (let [agent (ssh/ssh-agent {})
        session (ssh/session agent host {:username "lantern" :strict-host-key-checking :no})]
    (ssh/with-connection session (ssh/ssh session {:cmd cmd}))))

(defn pssh [hosts cmd]
  ;; XXX: reuse pool in concurrent requests, maybe cache it for some time or
  ;; even for the web server's lifetime
  (pool/pmap (min (count hosts) 50) #(ssh % cmd) hosts))

(def do-link-re #"https://api.digitalocean.com/v2/droplets/\?page=(\d+)")

(defn all-do-vpss []
  (let [first-page (do/droplets (env :do-token))
        last-index (->> first-page
                        :links
                        :pages
                        :last
                        (re-find do-link-re)
                        last
                        java.lang.Integer/parseInt)]
    (reduce into
            (:droplets first-page)
            (pool/pmap (min last-index 50)
                       #(:droplets (do/droplets (env :do-token) "" {:page %}))
                       (range 2 (+ last-index 1))))))

(defn all-vultr-vpss []
  (-> "https://api.vultr.com/v1/server/list"
      (http/get {:headers {"API-Key" (env :vultr-apikey)}})
      deref
      :body
      json/read-str))

(defroutes handler
  (GET "/" req
    {:status 200
     :headers {"content-type" "text/html"}
     :body (html [:head [:title "ops-panel (WIP)"]]
                 [:body
                  [:h2 "Ops Panel (WIP)"]
                  [:div "An amazing ops panel will be here Soon&trade;!"]
                  [:div "This is your request:"]
                  [:pre (with-out-str (pp/pprint req))]
                  [:div "The stuff below brought to you by Clojurescript magic:"]
                  [:div#app_container
                   [:script {:type "text/javascript" :src "main.js"}]
                   [:script {:type "text/javascript"} "ops_panel.core.main();"]]])})
  (files "/" {:root "target"})
  (resources "/" {:root "target"})
  (not-found "Page not found."))

(def app
  (wrap-defaults handler site-defaults))
