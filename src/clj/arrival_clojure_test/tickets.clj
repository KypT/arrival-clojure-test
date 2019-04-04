(ns arrival-clojure-test.tickets)

(def all-tickets (atom []))
(def last-ticket-id (atom 0))

(defrecord Ticket [id title description creator assignee deadline])

(defn create-ticket [title description creator assignee deadline]
  (last (swap! all-tickets conj
               (Ticket. (swap! last-ticket-id inc)
                        title
                        description
                        creator
                        assignee
                        deadline))))


(defn get-tickets []
  @all-tickets)
