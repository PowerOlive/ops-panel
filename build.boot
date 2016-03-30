(set-env!
 :source-paths #{"src/clj" "src/cljs" "src/cljc"}
 :resource-paths #{"res"}
 :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                 [adzerk/boot-cljs-repl "0.3.0" :scope "test"]
                 [adzerk/boot-reload "0.4.5" :scope "test"]
                 [pandeiro/boot-http "0.7.2" :scope "test"]
                 [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]
                 [boot-environ "1.0.2"]
                 [com.climate/claypoole "1.1.2"]
                 [clj-ssh "0.5.14"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [compojure "1.4.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.json "0.2.6"]
                 [datascript "0.15.0"]
                 [digitalocean "1.2"]
                 [environ "1.0.2"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]  ;; same as used by boot-http
                 [com.cemerick/piggieback "0.2.1" :scope "test"]
                 [posh "0.3.5"]
                 [reagent "0.6.0-alpha"]
                 [ring/ring-defaults "0.1.5"]
                 [com.taoensso/sente "1.8.1"]
                 [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                 [weasel "0.7.0" :scope "test"]])

(require
  '[adzerk.boot-cljs      :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-reload    :refer [reload]]
  '[environ.boot :refer [environ]]
  '[crisptrutski.boot-cljs-test  :refer [test-cljs]]
  '[pandeiro.boot-http    :refer [serve]])

(deftask auto-test []
  (merge-env! :resource-paths #{"test"})
  (comp (watch)
     (speak)
     (test-cljs)))

(deftask dev []
  (comp (environ :env {:in-development "indeed"})
     (serve :handler 'ops-panel.core/app
            :resource-root "target"
            :httpkit true
            :reload true)
     (watch)
     (speak)
     (reload :on-jsload 'ops-panel.core/main
             ;; XXX: make this configurable
             :open-file "emacsclient -n +%s:%s %s")
     (cljs-repl)
     (cljs :source-map true :optimizations :none)
     (target :dir #{"target"})))

(deftask build []
  (comp
   (cljs :optimizations :advanced)
   (aot :namespace '#{ops-panel.core})
   (pom :project 'ops-panel
        :version "0.1.0-SNAPSHOT")
   (uber)
   (jar :main 'ops-panel.core)
   (target :dir #{"target"})))
