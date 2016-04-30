(ns ops-panel.core
  (:require [cemerick.url :refer [url url-encode]]
            [clojure.data.json :as json]
            [clojure.pprint :as pp]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [files not-found resources]]
            [environ.core :refer [env]]
            [hiccup.core :refer [html]]
            [org.httpkit.client :as http]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as http-kit-adapter]
            [taoensso.sente.server-adapters.nginx-clojure :as nginx-adapter]))

(def in-development (= (env :in-development) "indeed"))

(def github-client-id "dbcf71904287bde31110")

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
  (if (get-in req [:session :user])
      {:status 200
       :headers {"content-type" "text/html"}
       :body (html [:head [:title "ops-panel (WIP)"]]
                   [:body
                    [:h2 "Ops Panel (WIP)"]
                    [:div "An amazing ops panel will be here Soon&trade;!"]
                    [:div#app_container
                     [:script {:type "text/javascript" :src "js/main.js"}]
                     [:script {:type "text/javascript"} "ops_panel.core.main();"]]
                    [:h2 "Your request"]
                    [:div [:pre (with-out-str (pp/pprint req))]]])}
      (let [redirect-url (str (assoc (url "https://github.com/login/oauth/authorize")
                                     :query
                                     {:client_id github-client-id}))]
        {:status 303
         :headers {"content-type" "text/html"
                   "Location" redirect-url}
         :session (assoc (get req :session {})
                         :github-login-original-path "/")
         :body (html [:head [:title "Please log in"]]
                     [:body
                      [:h2 "Please log in"]
                      [:p "Redirecting you to " redirect-url " to log in."]])})))

(defn github-auth-cb [code req]
  (let [token-resp @(http/post "https://github.com/login/oauth/access_token"
                               {:headers {"Accept" "application/json"}
                                :form-params {:client_id github-client-id
                                              :client_secret (env :github-client-secret)
                                              :code code}})
        access-token (get (json/read-str (:body token-resp))
                          "access_token")
        user-resp @(http/get "https://api.github.com/user"
                             {:headers {"Accept" "application/json"
                                        "Authorization" (str "token " access-token)}})
        user (get (json/read-str (:body user-resp))
                  "login")
        original-path (get-in req [:session :github-login-original-path] "/")]
    (println "client_secret" (env :github-client-secret))
    (println "code" code)
    (println "req" req)
    (println "token-resp" token-resp)
    (println "body" (:body token-resp))
    (println "body-at" (get (:body token-resp) "access_token"))
    (println "loaded-body" (json/read-str (:body token-resp)))
    (println "loaded-body-at" (get (json/read-str (:body token-resp)) "access_token"))
    (println "access-token" access-token)
    (println "user-resp" user-resp)
    (println "user" user)
    {:status 303
     :headers {"content-type" "text/html"
               "location" original-path}
     :session (assoc (get req :session {}) :user (str user))
     :body (html [:head [:title "Login successful"]]
                 [:body [:h1 "Login successful"]
                  [:p "Redirecting you to "
                   [:a {:href original-path} original-path]]])}))

(comment  [{{:keys [code state]} :params
            {session-state :state} :session
            :as req}])

(defroutes handler

  (GET "/" req (root req))
  (GET "/github-auth-cb" req
    (github-auth-cb (get-in req [:params :code]) req))
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
