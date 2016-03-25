(ns ops-panel.core
  (:require [clojure.pprint :as pp]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [hiccup.core :refer [html]]))
(comment 
  )

(comment )

(defroutes handler

  (GET "/" req {:status 200
                :headers {"content-type" "text/html"}
                :body (html [:head [:title "Yup it works!"]]
                            [:body
                             [:h2 "Hello from the server"]
                             [:div "This is your request"]
                             [:pre (with-out-str (pp/pprint req))]])})

  (not-found "Page not found."))

(def app
  (wrap-defaults handler site-defaults))
