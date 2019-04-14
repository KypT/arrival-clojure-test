(ns arrival-clojure-test.tickets
  (:require [datomic.api :as d]))

(def connection-string "datomic:sql://arrival?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic")

(def get-connection
  (memoize
    (fn []
      (d/connect connection-string))))

(defrecord Ticket [id title description creator assignee deadline])

(defn transact-ticket [title description creator assignee deadline]
  (d/transact (get-connection)
              [{:ticket/title title
                :ticket/description description
                :ticket/creator   creator
                :ticket/assignee  assignee
                :ticket/deadline deadline}]))

(defn create-ticket [title description creator assignee deadline]
  (let [transaction (transact-ticket title description creator assignee deadline)
        ticket-id (first (vals (:tempids @transaction)))]
    (Ticket. ticket-id title description creator assignee deadline)))

(defn get-tickets []
  (map (fn [[id title description creator assignee deadline]]
         (Ticket. id title description creator assignee deadline))
       (d/q
         `[:find ?e ?title ?description ?creator ?assignee ?deadline
           :where
           [?e :ticket/title ?title]
           [?e :ticket/description ?description]
           [?e :ticket/creator ?creator]
           [?e :ticket/assignee ?assignee]
           [?e :ticket/deadline ?deadline]]
         (d/db (get-connection)))))

(def tickets-schema [{:db/ident :ticket/title
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :ticket/description
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :ticket/creator
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}

                     {:db/ident :ticket/assignee
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :ticket/deadline
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}])


;(d/create-database connection-string)
;(d/transact conn tickets-schema)
