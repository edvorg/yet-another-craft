(ns yet-another-craft.repl
  (:use yet-another-craft.handler
        [org.httpkit.server :refer [run-server]]
        [ring.middleware file-info file]
        [mount.core :refer [start]]))

(defonce server (atom nil))

(defn get-handler []
  ;; #'app expands to (var app) so that when we reload our code,
  ;; the server is forced to re-resolve the symbol in the var
  ;; rather than having its own copy. When the root binding
  ;; changes, the server picks it up without having to restart.
  (-> #'app
                                        ; Makes static assets in $PROJECT_DIR/resources/public/ available.
      (wrap-file "resources")
                                        ; Content-Type, Content-Length, and Last Modified headers for files in body
      (wrap-file-info)))

(defn start-server
  "used for starting the server in development mode from REPL"
  [& [port]]
  (start)
  (let [port (if port (Integer/parseInt port) 3000)]
    (reset! server
            (run-server (get-handler)
                        {:port port
                         :auto-reload? true
                         :join? false}))
    (println (str "You can view the site at http://localhost:" port))))

(defn stop-server []
  (@server)
  (reset! server nil))
