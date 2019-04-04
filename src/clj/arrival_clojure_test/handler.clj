(ns arrival-clojure-test.handler
  (:require
   [reitit.ring :as reitit-ring]
   [arrival-clojure-test.middleware :refer [middleware]]
   [clojure.data.json :as json]
   [hiccup.page :refer [include-js include-css html5]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.middleware.json :refer [wrap-json-body]]
   [config.core :refer [env]]
   [arrival-clojure-test.tickets :as tickets]))

(def mount-target
  [:div#app
   [:h2 "Welcome to arrival-clojure-test"]
   [:p "please wait while Figwheel is waking up ..."]
   [:p "(Check the js console for hints if nothing exciting happens.)"]])

(defn head []
  [:head
   [:meta {:csrf-token *anti-forgery-token*}]
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js "/js/app.js")]))

(defn index-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})

(defn api-all-tickets-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str (tickets/get-tickets))})

(defn api-create-ticket-handler
  [{ticket-params :body}]
  (let [ticket (tickets/create-ticket
                 (:title ticket-params)
                 (:description ticket-params)
                 (:creator ticket-params)
                 (:assignee ticket-params)
                 (:deadline ticket-params))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str ticket)}))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/items"
      ["" {:get {:handler index-handler}}]
      ["/:item-id" {:get {:handler index-handler
                          :parameters {:path {:item-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]
     ["/api"
      ["/tickets" {:get {:handler api-all-tickets-handler}}]
      ["/ticket" {:post {:handler api-create-ticket-handler}}]]])
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware (conj middleware #(wrap-json-body % {:keywords? true}))}))
