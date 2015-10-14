(ns reagent-example.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [org.httpkit.server :refer [with-channel on-close on-receive send!]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [prone.middleware :refer [wrap-exceptions]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [environ.core :refer [env]]
            [reagent-example.util :as util]
            [clojure.tools.reader :as reader]))

(defonce entities (atom {}))
(defonce users (atom {}))
(defonce minerals (atom {}))

(defn reset []
  (reset! entities {})
  (reset! users {})
  (reset! minerals {}))

(comment
  (reset)
  )

(def current-time (atom 0))

(def home-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]]
     (include-js "js/app.js")]]))

(defn get-channel-state [channel]
  {:entities @entities
   :minerals (@minerals (@users channel))})

(defn get-channel-state-initial [channel]
  (merge (get-channel-state channel) {:map (util/make-map)}))

(defn add-channel [channel username]
  (swap! entities #(merge % (util/make-initial-units username)))
  (swap! users #(assoc % channel username))
  (swap! minerals #(assoc % username 100)))

(defn remove-channel [channel]
  (swap! users #(dissoc channel %))
  (println "socket closed"))

(defn harvest [username value]
  (println "harvest")
  (swap! minerals (fn [m] (update-in m [username] (partial + value)))))

(defn repair [username entity]
  (println "repair")
  (let [{:keys [hp max-hp]} (@entities entity)
        resources (@minerals username)]
    (if (and (< hp max-hp) (> resources 0))
      (do
        (swap! minerals (fn [m] (update-in m [username] dec)))
        (swap! entities (fn [e] (update-in e [entity :hp] inc)))))))

(defn marine [username entity]
  (println "marine"))

(defn attack [username entity target]
  (println "attack"))

(defn move [username entity x y]
  (println "move"))

(defn handle-commands [username {:keys [command entity x y target] :as msg}]
  (cond
    (= command :harvest) (harvest username util/harvest-power)
    (= command :repair) (repair username entity)
    (= command :marine) (marine username entity)
    (= command :attack) (attack username entity target)
    (= command :move) (move username entity x y)))

(defn web-socket-handler [req]
  (with-channel req channel
    (let [username (get-in req [:params :name])]
      (println "socket opened for user ")
      (println username)
      (add-channel channel username)
      (send! channel (pr-str (get-channel-state-initial channel)))
      (on-close channel (fn [status] (remove-channel channel)))
      (on-receive channel (fn [msg]
                            (handle-commands (@users channel) (read-string msg))
                            (send! channel (pr-str (get-channel-state channel))))))))

(defroutes routes
  (GET "/" [] home-page)
  (GET "/ws/:name" [] web-socket-handler)
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
