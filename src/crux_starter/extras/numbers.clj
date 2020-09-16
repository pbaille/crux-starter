(ns crux-starter.extras.numbers
  (:require [crux-starter.sugar [puts q]]))

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