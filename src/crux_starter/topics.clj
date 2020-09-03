(ns crux-starter.topics
  (:require [crux-starter.setup :refer [node]]
            [crux-starter.sugar :refer [puts q]]
            [crux.api :as crux]))

(defn topic
  "creates a topic map"
  [id & kvs]
  (merge {:id id :type :topic}
         (apply hash-map kvs)))

(puts

  ;; some topics

  (topic :god)

  (topic :programming)
  (topic :prolog :belongs-to [:programming])

  (topic :music)
  (topic :guitar :belongs-to [:music])
  (topic :phrasing :belongs-to [:guitar]))

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



