(ns webapp.core
  (:gen-class)
  (:require [ring.adapter.jetty :as ring]
            [webapp.handlers.core :refer [app]]
            [webapp.settings :refer [settings]]
            [mount.core :as mount]))

(declare server)
(mount/defstate server
  :start (ring/run-jetty app {:port (get settings :port) :join? false})
  :stop (.stop server))

(defn reset []
  (mount/stop)
  (mount/start))

(defn -main
  ;;[& args]
  [& _]
    (mount/start))