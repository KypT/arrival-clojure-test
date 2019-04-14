(ns arrival-clojure-test.handler
  (:require
   [compojure.core :refer :all]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [clojure.data.json :as json]
   [hiccup.page :refer [include-js include-css html5]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.middleware.json :refer [wrap-json-body]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [prone.middleware :refer [wrap-exceptions]]
   [ring.middleware.reload :refer [wrap-reload]]
   [config.core :refer [env]]
   [arrival-clojure-test.tickets :as tickets]))

(def mount-target
  [:div#app
   [:h2 "Welcome to arrival-clojure-test"]
   [:p "Loading ..."]])

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

(defroutes app-routes
           (route/resources "/" {:root "/public"})
           (GET "/" request (index-handler request))
           (context "/tickets" []
             (GET "/" request (index-handler request))
             (GET "/:id" [id :as request] (index-handler request)))
           (context "/api" []
             (GET "/tickets" request (api-all-tickets-handler request))
             (POST "/ticket" request (api-create-ticket-handler request)))
           (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> (handler/site app-routes)
      (wrap-defaults site-defaults)
      (wrap-reload)
      (wrap-json-body {:keywords? true})
      (wrap-exceptions)))
