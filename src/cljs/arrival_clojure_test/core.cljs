(ns arrival-clojure-test.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [ajax.core :refer [GET POST]]
   [re-frame.core :as rf]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]))

(defn get-csrf-token []
  (.getAttribute (.querySelector js/document "meta[csrf-token]") "csrf-token"))

(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:tickets []
     :csrf-token (get-csrf-token)}))

(rf/reg-event-db
  :load-tickets
  (fn [db _]
    (GET
      "/api/tickets"
      {:headers {:X-CSRF-Token (:csrf-token db)
                 :Content-Type "application/json"}
       :handler #(rf/dispatch [:tickets-loaded %1])
       :response-format :json
       :keywords? true
       :error-handler #(println %1)})
    db))

(rf/reg-event-db
  :create-new-ticket
  (fn [db [e new-ticket]]
    (POST
      "/api/ticket"
      {:headers {:X-CSRF-Token (:csrf-token db)
                 :Content-Type "application/json"}
       :body (.stringify js/JSON (clj->js new-ticket))
       :handler #(rf/dispatch [:ticket-created %1])
       :response-format :json
       :keywords? true
       :error-handler #(println %1)})
    db))

(rf/reg-event-db
  :ticket-created
  (fn [db [_ ticket]]
    (assoc db :tickets (conj (:tickets db) ticket))))

(rf/reg-event-db
  :tickets-loaded
  (fn [db [_ tickets]]
    (assoc db :tickets tickets)))

(rf/reg-sub
  :get-tickets
  (fn [db _]
    (reverse (:tickets db))))

(rf/reg-sub
  :get-ticket
  (fn [db [_ id]]
    (first (filter #(= (:id %) id) (:tickets db)))))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/tickets"
     ["/:ticket-id" :ticket]]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components


(defn simple-input [value type title field]
  (let [id (gensym)]
    [:div
     [:label {:for id} title]
     [:input {:id id :type type :value (field @value)
              :on-change #(swap! value assoc field (-> % .-target .-value))}]]))

(defn simple-textarea [value title field]
  (let [id (gensym)]
    [:div
     [:label {:for id} title]
     [:textarea {:id id :value (field @value)
                 :on-change #(swap! value assoc field (-> % .-target .-value))}]]))

(def new-ticket (atom {:title "" :description "" :creator "" :assignee "" :deadline ""}))

(defn create-new-ticket-button []
  [:div {:id "new-ticket-form"}
   (simple-input new-ticket "text" "title" :title)
   (simple-textarea new-ticket "description" :description)
   (simple-input new-ticket "text" "creator" :creator)
   (simple-input new-ticket "text" "assignee" :assignee)
   (simple-input new-ticket "date" "deadline" :deadline)
   [:input {:type "button" :value "New Ticket"
            :on-click #(rf/dispatch [:create-new-ticket @new-ticket])}]])


(defn tickets-page []
  (fn []
    [:span.main
     [:h1 "All tickets"]
     [create-new-ticket-button]
     [:ul (map (fn [ticket]
                 [:li {:name (str "Ticket id:" (:id ticket)) :key (:id ticket)}
                  [:a {:href (path-for :ticket {:ticket-id (:id ticket)})}
                   (:id ticket)
                    " "
                   (or (:title ticket) "no-title")]])
               @(rf/subscribe [:get-tickets]))]]))


(defn ticket-page []
  (fn []
    (let [routing-data (session/get :route)
          id (long (get-in routing-data [:route-params :ticket-id]))
          ticket @(rf/subscribe [:get-ticket id])]
      [:span.main
       [:h1 (:id ticket) " " (:title ticket)]
       [:div (:description ticket)]
       [:div  "Assignee: " (:assignee ticket)]
       [:div  "Creator: " (:creator ticket)]
       [:div  "Deadline: " (:deadline ticket)]
       [:p [:a {:href (path-for :index)} "Back to the list of tickets"]]])))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'tickets-page
    :ticket #'ticket-page))

;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [page]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (rf/dispatch-sync [:initialize])
  (rf/dispatch-sync [:load-tickets])
  (mount-root))
