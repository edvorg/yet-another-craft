(ns yet-another-craft.server
  (:require [yet-another-craft.handler :refer [app]]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [mount.core :refer [start]])
  (:gen-class))

(defn -main [& args]
  (start)
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (run-server app {:port port :join? false})))
