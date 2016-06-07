;;; Compojure and Sente routing.

(ns ops-panel.core
  (:require [clojure.pprint :as pp]
            [compojure.core :refer (defroutes GET POST)]
            [compojure.route :refer (files not-found resources)]
            [environ.core :refer (env)]
            [hiccup.core :refer (html)]
            [ops-panel.github-login :as github-login]
            [ops-panel.ip-whitelist :as ip-wl]
            [ring.middleware.defaults :refer (wrap-defaults site-defaults)]))

(def in-development (= (env :in-development) "indeed"))

(def not-authorized {:status 401
                     :headers {"content-type" "text/plain"}
                     :body "Not authorized"})

(defn whitelist-ip [req]
  (if-let [user (get-in req [:session :user])]
    (do
      (ip-wl/whitelist-ip user (:remote-addr req))
      {:status 200
       :headers {"content-type" "application/edn"}
       :body (pr-str (ip-wl/whitelisted-ips user))})
    not-authorized))

(defn is-ip-whitelisted? [ip req]
  (println ip (get-in req [:headers "whitelist-query-token"] "no token provided"))
  (if (= (get-in req [:headers "whitelist-query-token"] "no token provided")
             (env :whitelist-query-token))
    {:status 200
     :headers {"content-type" "text/plain"}
     :body (if (ip-wl/is-ip-whitelisted? ip) "yes" "no")}
    not-authorized))

(defn root [req]
  (if-let [user (get-in req [:session :user])]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (html [:head [:title "ops-panel (WIP)"]]
                 [:body
                  [:h2 "Ops Panel (WIP)"]
                  [:div (str "Hello, " user "!")]
                  [:div#app_container
                   ;; DRY: pre-rendering what the Clojurescript side will produce.
                   [:div "You have the following IPs whitelisted:"
                    [:ul
                     (for [ip (ip-wl/whitelisted-ips user)]
                       ^{:key ip} [:li ip])]]
                   [:script {:type "text/javascript" :src "js/main.js"}]
                   [:script {:type "text/javascript"} "ops_panel.core.main();"]]])}
    (github-login/login req)))

(defroutes handler
  (GET "/" req (root req))
  (GET "/github-auth-cb" [code state :as req]
    (github-login/github-auth-cb code state (get req :session {})))
  ;;XXX make this a POST
  (GET "/whitelist-ip" req (whitelist-ip req))
  (GET "/is-ip-whitelisted" [ip :as req] (is-ip-whitelisted? ip req))
  ;; XXX: this is what seems to work; figure out why!
  (resources (if in-development "/public" "/"))
  (files "/public")
  (not-found "Page not found."))

(def app
  (wrap-defaults handler site-defaults))

(defn start! []
  nil)

(when in-development
  (start!))

;; This is set in nginx.conf as jvm_init_handler_name, so it will get called on
;; startup.
(defn nginx-init! [_]
  (start!))
