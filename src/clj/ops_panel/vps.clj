(ns ops-panel.vps
  (:require [clojure.data.json :as json]
            [com.climate.claypoole :as pool]
            [digitalocean.v2.core :as do]
            [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [clojure.string :as str]))

(defn do-ip [version m]
  (first (for [submap (get-in m [:networks version])
               :when (= (:type submap) "public")]
           (:ip_address submap))))

(defn do-vps [do-map]
  {:v4ip (do-ip :v4 do-map)
   :v6ip (do-ip :v6 do-map)
   :name (:name do-map)
   :ram (:memory do-map)
   :monthly-cost (get-in do-map [:size :price_monthly])
   :provider :do
   :etc do-map})

(def do-link-re #"https://api.digitalocean.com/v2/droplets/\?page=(\d+)")

(defn all-do-vpss []
  (let [first-page (do/droplets (env :do-token))
        last-index (->> first-page
                        :links
                        :pages
                        :last
                        (re-find do-link-re)
                        last
                        Integer/parseInt)]
    (map do-vps
         (reduce into
                 (:droplets first-page)
                 (pool/pmap (min last-index 50)
                            #(:droplets (do/droplets (env :do-token) "" {:page %}))
                            (range 2 (+ last-index 1)))))))

(defn vl-vps [vl-map]
  {:v4ip (vl-map "main_ip")
   :v6ip (vl-map "v6_main_ip")
   :name (vl-map "label")
   :ram (let [ramstr (vl-map "ram")]
          (Integer/parseInt
           (subs ramstr 0 (- (count ramstr)
                             (count " MB")))))
   :monthly-cost (Double/parseDouble (vl-map "cost_per_month"))
   :provider :vl
   :etc vl-map})

(defn all-vl-vpss []
  (->> @(http/get "https://api.vultr.com/v1/server/list"
                  {:headers {"API-Key" (env :vultr-apikey)}})
      :body
      json/read-str
      vals
      (map vl-vps)))
