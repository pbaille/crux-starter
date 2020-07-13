(ns crux-primer.core
  (:require [crux.api :as crux]
            [clojure.java.io :as io]))

;; setup ---------------------------------------------------------------------------------------------------------------

;; standalone config

(defn start-standalone-node ^crux.api.ICruxAPI [storage-dir]
  (crux/start-node {:crux.node/topology '[crux.standalone/topology]
                    :crux.kv/db-dir (str (io/file storage-dir "db"))}))

;; node

(def node (start-standalone-node "crux-store"))

;; operations ----------------------------------------------------------------------------------------------------------

;; put ------------

(def put-exemple1
  [:crux.tx/put
   {:crux.db/id :one :data 1} ;; the data we will put
   #inst "2017" ;; valid time starts (optional)
   #inst "2019-12-31" ;; valid time ends (optional)
   ])

;; usage

(crux/submit-tx node
                [put-exemple1])

(crux/entity (crux/db node) :one)
;; -> nil ???

;; this is because valid time is over

(crux/entity (crux/db node #inst "2018-06") ;; we specify a valid time
             :one)

;; -> {:crux.db/id :one, :data 1}

;; match -------------

(crux/submit-tx node
                [[:crux.tx/match
                  :one ;; we will check this entity against next provided arg
                  {:crux.db/id :one :data 1} ;; the document we will match the entity against
                  #inst "2018-06"] ;; at a this given valid time

                 ;; if the match expression succeed we will transact the following forms
                 [:crux.tx/put
                  {:crux.db/id :one
                   :data 1
                   :extra 2}]])

;; delete -------------

;; this form will delete our :one entity from 2018 to 2019
(crux/submit-tx node
                [[:crux.tx/delete :one
                  #inst "2018"
                  #inst "2019"]])

(crux/entity (crux/db node #inst "2017") :one) ;; still exists in 2017
(crux/entity (crux/db node #inst "2018") :one) ;; no longer exists in 2018
(crux/entity (crux/db node #inst "2019") :one) ;; still exists in 2019

;; evict --------------

;; remove all historical versions of a document
(crux/submit-tx node
                [[:crux.tx/evict :one]])

;; data ----------------------------------------------------------------------------------------------------------------

;; adding-data

(defn puts
  "transact several puts on node"
  [& xs]
  (crux/submit-tx node
                  (mapv (fn [x]
                          (let [id (or (:crux.db/id x) (:id x) (throw (Exception. "no id in " x)))]
                            [:crux.tx/put (assoc (dissoc x :id) :crux.db/id id)]))
                        xs)))

(defn topic
  "creates a topic map"
  [id & kvs]
  (merge {:id id :type :topic}
         (apply hash-map kvs)))

(puts
  ;; myself
  {:id :me
   :name "Pierre"
   :last-name "Baille"
   :address {:zip "75018"
             :town "Paris"
             :street "31 rue Letort"
             :floor 2}}

  ;; some topics

  (topic :god)

  (topic :programming)
  (topic :prolog :belongs-to [:programming])

  (topic :music)
  (topic :guitar :belongs-to [:music])
  (topic :phrasing :belongs-to [:guitar]))

;; queries -------------------------------------------------------------------------------------------------------------

;; query shortcut

(defn q [query]
  (crux/q (crux/db node)
          query))

;; list topics

(q {:find ['id]
    :where [['id :type :topic]]})

;; with full documents

(q {:find ['id]
    :where [['id :type :topic]]
    :full-results? true})

(defn subtopics
  "find all existent subtopics for a given topic"
  [topic]
  (q {:find ['sub]
      :where [['sub :type :topic]
              '(belongs-to sub topic)]
      :rules '[[(belongs-to ?e1 ?e2) [?e1 :belongs-to ?e2]]
               [(belongs-to ?e1 ?e2) [?e1 :belongs-to ?t] (belongs-to ?t ?e2)]]
      :args [{'topic topic}]}))

;; list all :music subtopics

(subtopics :music)

;; lets put all our rules in one place, we will then be able to share them between queries

(def rules
  '[[(is-topic ?e) [?e :type :topic]]
    [(belongs-to ?e1 ?e2) [?e1 :belongs-to ?e2]]
    [(belongs-to ?e1 ?e2) [?e1 :belongs-to ?t] (belongs-to ?t ?e2)]
    [(owned-by ?e1 ?e2) (belongs-to ?e2 ?e1)]])

(defn subtopics
  "find all existent subtopics for a given topic"
  [topic]
  (q {:find ['sub]
      :where '[(is-topic sub) (belongs-to sub topic)]
      :rules rules
      :args [{'topic topic}]}))

(defn suptopics
  "find all existent parent topics for a given topic"
  [topic]
  (q {:find ['sup]
      :where '[(is-topic sup) (owned-by sup topic)]
      :rules rules ;; use our global rules here
      :args [{'topic topic}]}))

(subtopics :music) ;; #{[:guitar] [:phrasing]}

(suptopics :phrasing) ;; #{[:god] [:guitar] [:music]}

;; transaction functions

(puts
  {:id :topic/add-parent
   :crux.db/fn
   '(fn [ctx eid parent]
      (let [db (crux.api/db ctx)
            entity (crux.api/entity db eid)]
        [[:crux.tx/put (update entity :belongs-to (fnil conj []) parent)]]))})

(crux/submit-tx node
                [[:crux.tx/fn :topic/add-parent :music :god]])

(defn get-by-id [id]
  (ffirst
    (q {:find ['e]
        :where [['e :crux.db/id id]]
        :full-results? true})))

(get-by-id :music)

;; populate db with simple number records (for testing purposes)

(let [num (fn [n]
            {:id (keyword (str "num_" n))
             :type :num
             :val n
             :odd (odd? n)})]

  (apply puts (map num (range 10))))

;; 'or
(q '{:find [v]
     :where [(or [e :odd true] [e :val 0]) ; <--
             [e :val v]]})

;; not
(q '{:find [e]
     :where [(not [e :crux.db/id :god])
             [e :type :topic]]})

;; set literals can be useful
(q '{:find [e]
     :where [[e :val #{0 5 8}] ; <--
             [e :val v]]
     :full-results? true})

;; queries also supports vector syntax
(q '[:find e v
     :where (or [e :odd true] [e :val 0]) [e :val v]])

;; tuples given to the :where key can also be duplets to test for the existence of an attribute
(q '[:find e
     :where [e :val]])

;; predicates
(q '{:find [v]
     :where [[e :val v] [(>= v 5)]]})

