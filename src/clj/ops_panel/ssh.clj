(ns ops-panel.ssh
  (:require [clj-ssh.ssh :as ssh]
            [com.climate.claypoole :as pool]))

(defn ssh [host cmd]
  (let [agent (ssh/ssh-agent {})
        session (ssh/session agent host {:username "lantern" :strict-host-key-checking :no})]
    (ssh/with-connection session (ssh/ssh session {:cmd cmd}))))

(defn pssh [hosts cmd]
  ;; XXX: reuse pool in concurrent requests, maybe cache it for some time or
  ;; even for the web server's lifetime
  (pool/pmap (min (count hosts) 50) #(ssh % cmd) hosts))
