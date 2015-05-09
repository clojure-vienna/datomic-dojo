(ns datomic-dojo.core
  (:require [datomic.api :as d]))

(def db-uri "datomic:mem://db-name")
(defn create-db [uri schema]
  (let [created (d/create-database uri)
        conn (d/connect uri)]
    (when created
      (deref (d/transact conn schema)))
    conn))

(def schema
  [{:db/id (d/tempid :db.part/db)
    :db/ident :todo.task/text
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id (d/tempid :db.part/db)
    :db/ident :todo.task/done
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :todo.task/ordinal
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])

(def conn (create-db db-uri schema))

(create-ns 'todo.task)
(alias 'tt 'todo.task)

(defn test-data []
  [[:db/add (d/tempid :db.part/user) ::tt/text "First Todo"]
   {:db/id (d/tempid :db.part/user)
    ::tt/text "Second Todo"}])

(defn insert-test-data [conn]
  @(d/transact conn (test-data)))

(defn reify-entity
  ([e]
   (into {:db/id (:db/id e)} e))
  ([db e]
   (reify-entity (d/entity db e))))

(defn list-todos [db]
  (for [id (d/q '[:find [?id ...] :where
                  [?id ::tt/text]]
                db)]
    (-> (d/entity db id)
        reify-entity)))

;; server connector
(comment
  (def server (jetty (wrap-datomic handler uri)))

  (defn wrap-datomic [h uri]
    (let [conn (d/connect uri)]
      (fn [req]
        (h (assoc req
                  :datomic/conn conn
                  :datomic/db (d/db conn))))))

  (defn handler [{:as req
                  conn :datomic/conn
                  db :datomic/db}]
    {:status 200 :body (pr-str (list-todos db))}))
