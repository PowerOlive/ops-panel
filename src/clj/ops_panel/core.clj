(ns ops-panel.core
  (:require [clojure.pprint :as pp]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [files not-found resources]]
            [environ.core :refer [env]]
            [hiccup.core :refer [html]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as http-kit-adapter]
            [taoensso.sente.server-adapters.nginx-clojure :as nginx-adapter]))

(def in-development (= (env :in-development) "indeed"))

;; sente setup

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! (if in-development
                                    http-kit-adapter/sente-web-server-adapter
                                    nginx-adapter/sente-web-server-adapter)
                                  {})]
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

(defn root [req]
  (if (get-in req [:session :authorized])
      {:status 200
       :headers {"content-type" "text/html"}
       :body (html [:head [:title "ops-panel (WIP)"]]
                   [:body
                    [:h2 "Ops Panel (WIP)"]
                    [:div "An amazing ops panel will be here Soon&trade;!"]
                    [:div#app_container
                     [:script {:type "text/javascript" :src "js/main.js"}]
                     [:script {:type "text/javascript"} "ops_panel.core.main();"]]])}
      {:status 200
       :headers {"content-type" "text/html"}
       :body (html [:head [:title "Please log in"]]
                   [:body
                    [:h2 "Please log in"]
                    [:form {:action "/login"}
                     [:input {:type "submit" :value "Login"}]]])}))

(defn login [req]
  {:status 303
   :headers {"Location" "/"}
   :session (assoc (get req :session {}) :authorized true)
   :body (html [:head [:title "Successfully logged in"]]
               [:body "Successfully logged in; redirecting to"
                [:a {:href "/"} "/"]])})

(defroutes handler

  (GET "/" req (root req))
  (GET "/login" req (login req))

  ;; sente
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))

  (resources "/")
  (files "/")
  (not-found "Page not found."))

(def app
  (wrap-defaults handler site-defaults))

(if in-development
  (start-router!))

;; This is set in nginx.conf as jvm_init_handler_name, so it will get called on
;; startup.
(defn nginx-init! [_]
  (start-router!))
