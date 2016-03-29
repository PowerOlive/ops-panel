(ns ops-panel.core
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [reagent.core :as r]
            [taoensso.sente  :as sente :refer (cb-success?)]))

;; sente
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defonce app-state (r/atom 0))

(defn app []
  [:div (str "Current value is " @app-state)
   [:input {:type "button"
            :value "Click me NOW"
            :on-click (fn [] (chsk-send! [:test/inc @app-state]
                                        5000
                                        (fn [resp]
                                          (reset! app-state resp))))}]])

(defn ^:export main []
  (r/render [app]
            (js/document.getElementById "app_container")))
