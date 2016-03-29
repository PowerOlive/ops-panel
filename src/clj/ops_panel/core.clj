(ns ops-panel.core
  (:require [clojure.pprint :as pp]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [files not-found resources]]
            [hiccup.core :refer [html]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.sente :as sente]
            [org.httpkit.server :as http-kit]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)])
  (:gen-class))

;; sente setup

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defmulti sente-handler :id)

(defmethod sente-handler :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (println "Unhandled event:" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

;; test handler
(defmethod sente-handler :test/inc
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (?reply-fn (inc ?data)))

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk sente-handler)))

(defroutes handler

  (GET "/" req
    {:status 200
     :headers {"content-type" "text/html"}
     :body (html [:head [:title "ops-panel (WIP)"]]
                 [:body
                  [:h2 "Ops Panel (WIP)"]
                  [:div "An amazing ops panel will be here Soon&trade;!"]
                  [:div#app_container
                   [:script {:type "text/javascript" :src "main.js"}]
                   [:script {:type "text/javascript"} "ops_panel.core.main();"]]])})

  ;; sente
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))

  (files "/" {:root "target"})
  (resources "/" {:root "target"})
  (not-found "Page not found."))

(def app
  (wrap-defaults handler site-defaults))

(defn start-web-server! [ring-handler port]
    (println "Starting http-kit...")
    (http-kit/run-server ring-handler {:port port}))

(defn start! [web-port]
  (start-router!)
  (start-web-server! app web-port))

;; XXX: do this only on development.
;(start-router!)

(defn -main []
  (start! 62000))
