(ns crux-starter.transactions
  (:require [crux.api :as crux]
            [crux-starter.setup :refer [node]]))

;; putting data into the database
;; ---------------------------------------------------------------------------------------------------------------------

;; crux valid documents are arbitrary nested edn maps
;; the only requirement is the presence of a :crux.db/id key pointing to either a keyword or a map

;; let's say that we have a clojure map that fulfill this requirement

(def data1 {:crux.db/id :data1
            :data 1})

;; we can transact it to the database like this

(crux/submit-tx node
                [[:crux.tx/put data1]])

;; the simplest way to retrieve it is to use crux/entity

(crux/entity (crux/db node) :data1)
;;=> {:crux.db/id :data1, :data 1}

;; the crux/db call is returning the current value of our database
;; if we are interested in retrieving its value at a given time we can feed it a second argument

(crux/db node #inst "2000") ;; returns the value of the database as in the beginning of the year 2000

;; as we can check our previously trasacted :data1 document does not yet exists in 2000

(crux/entity (crux/db node #inst "2000") :data1) ;;=> nil

;; crux/submit-tx can take several transactions

(crux/submit-tx node
                [[:crux.tx/put {:crux.db/id :data2 :data 2}]
                 [:crux.tx/put {:crux.db/id :data3 :data 3}]])

;; the :crux.tx/put operation is letting you specify the valid time frame of the given document

(crux/submit-tx node
                [;; a document that is valid forever starting at the beginning of the year 2019
                 [:crux.tx/put {:crux.db/id :timed1 :value 10}
                  #inst "2019"]

                 ;; a document that is valid between 2017 and 2018
                 [:crux.tx/put {:crux.db/id :timed2 :value 10}
                  #inst "2017"
                  #inst "2018"]])

;; timed1 is not yet valid in 2000
(crux/entity (crux/db node #inst "2000") :timed1)
;;=> nil

;; but is in 2019
(crux/entity (crux/db node #inst "2019") :timed1)
;;=> {:crux.db/id :timed1, :value 10}

;; timed2 not yet valid in 2000
(crux/entity (crux/db node #inst "2019") :timed2)
;;=> nil

;; but is on june 2017
(crux/entity (crux/db node #inst "2017-06") :timed2)
;;=> {:crux.db/id :timed2, :value 10}

;; but is no longer valid in 2019
(crux/entity (crux/db node #inst "2019") :timed2)
;;=> nil

;; deleting (invalidating) documents
;; ---------------------------------------------------------------------------------------------------------------------

;; this form will delete (invalidate) our :timed2 entity  (that is valid in 2017 only) from august to october 2017
(crux/submit-tx node
                [[:crux.tx/delete :timed2
                  #inst "2017-08"
                  #inst "2017-10"]])

;; still exists in january
(crux/entity (crux/db node #inst "2017-01") :timed2)
;;=> {:crux.db/id :timed2, :value 10}

;; no longer exists in september
(crux/entity (crux/db node #inst "2017-09") :timed2)
;;=> nil

;; still exists in december
(crux/entity (crux/db node #inst "2017-11") :timed2)
;;=> {:crux.db/id :timed2, :value 10}

;; like :crux.tx.put, :curx.tx/delete do not have to take valid-time starts and ends
;; if not the data will be deleted (invalidated) from now

(crux/submit-tx node
                [[:crux.tx/delete :timed1]])

;; :timed1 is no longer valid
(crux/entity (crux/db node) :timed1)
;;=> nil

;; but is still valid in 2019
(crux/entity (crux/db node #inst "2019") :timed1)
;;=> {:crux.db/id :timed1, :value 10}

;; eviction
;;----------------------------------------------------------------------------------------------------------------------

;; remove all historical versions of a document
(crux/submit-tx node
                [[:crux.tx/evict :one]])

;; conditional transactions
;; ---------------------------------------------------------------------------------------------------------------------

;; one way to issue transaction only if certain condition is met is to use the :crux.tx/match operation
;; it let you verify the value of a database document against a given value
;; and issue some transactions only if those are equals

(crux/submit-tx node
                [[:crux.tx/match
                  :data1 ;; we will check this entity against next provided arg
                  {:crux.db/id :data1 :data 1} ;; the value we check the corresponding document against
                  ]

                 ;; if the match expression succeed we will transact the following forms
                 [:crux.tx/put
                  {:crux.db/id :data1
                   :data 1
                   :foo :bar}]]) ;; <- we had an entry to our document

(crux/entity (crux/db node) :data1)
;;=> {:crux.db/id :data1, :data 1, :foo :bar}

;; like previously seen operations, crux.db/match can take a time at which to issue the matching

(crux/submit-tx node
                [[:crux.tx/match
                  :data2
                  {:crux.db/id :data2 :data 2}
                  #inst "2019"] ;; the point in time where we do the check

                 ;; since in 2019, :data2 does not still exists, the belowing transaction is not executed
                 [:crux.tx/put
                  {:crux.db/id :data2
                   :never :occurs}]])

(crux/entity (crux/db node) :data2)
;;=> {:crux.db/id :data2, :data 2}



;; modelling a simplistic bank account
;; ---------------------------------------------------------------------------------------------------------------------

;; initiating it with 2018 with 20 dollars on it
(crux/submit-tx node
                [[:crux.tx/put
                  {:crux.db/id :bank-account
                   :dollars 20}
                  #inst "2018"]])

;; for chrismass 2018 grandma gave us 20 dollars
(crux/submit-tx node
                [[:crux.tx/put
                  {:crux.db/id :bank-account
                   :dollars 40}
                  #inst "2018-12-25"]])

;; on the january first 2019, we've bought a cookie for 1 dollar
(crux/submit-tx node
                [[:crux.tx/put
                  {:crux.db/id :bank-account
                   :dollars 39}
                  #inst "2019-01"]])

;; in june 2018 we've gt 20 dollars
(crux/entity (crux/db node #inst "2018-06") :bank-account)

;; on the december 28th of 2018, we are at our peak with the amount of 40 dollars
(crux/entity (crux/db node #inst "2018-12-28") :bank-account)

;; sadly grandma is dead before christmass 2019 and we still have 39 dollars
(crux/entity (crux/db node) :bank-account)
;=> {:crux.db/id :bank-account, :dollars 20}

;; transaction functions
;; ---------------------------------------------------------------------------------------------------------------------

;; Transaction functions are user-supplied functions that run on the individual Crux nodes when a transaction is being ingested.
;; They can take any number of parameters, and return normal transaction operations which are then indexed as above.
;; If they return false or throw an exception, the whole transaction will roll back.


;; the first exemple is a transaction function that add (or substract) a given amount on our fancy :bank-account document

;; transaction functions are defined with our old friend crux.tx/put
;; the given document has to have a :crux.db/fn key pointing to the function code (quoted)

(crux/submit-tx node
                [[:crux.tx/put {:crux.db/id :update-bank-account
                                :crux.db/fn
                                ;; note that the function body is quoted.
                                '(fn [ctx delta]
                                   ;; the first argument (ctx) is holding our node
                                   ;; we can use it as we've done so far
                                   (let [db (crux.api/db ctx) ;; we taking the present value of the database
                                         entity (crux.api/entity db :bank-account)] ;; using it to retrieve our bank-account document
                                     ;; then we are returning a vector of transaction (containing only one in this case)
                                     [[:crux.tx/put (update entity :dollars + delta)]]))}]])

(crux/submit-tx node
                [[:crux.tx/fn :update-bank-account 5]])

(crux/entity (crux/db node) :bank-account)

;; exemple 2

;; a transaction function that can create a new document by merging existing/given ones

(crux/submit-tx node
                [[:crux.tx/put {:crux.db/id :merge
                                :crux.db/fn
                                '(fn [ctx id & xs]
                                   (let [db (crux.api/db ctx)]
                                     [[:crux.tx/put
                                       (reduce merge
                                               {:crux.db/id id}
                                               (map (fn [e]
                                                      (cond
                                                        (keyword? e) (dissoc (crux.api/entity db e) :crux.db/id)
                                                        (map? e) e))
                                                    xs))]]))}]])

(crux/submit-tx node
                [ ;; putting 2 dummy records into the db
                 [:crux.tx/put {:crux.db/id :m1 :a 1 :b 2}]
                 [:crux.tx/put {:crux.db/id :m2 :a 4 :c 3}]
                 ;; use them to built another dummy record via our freshly defined merge transaction function
                 [:crux.tx/fn :merge :m3 :m1 :m2 {:d 5}]])

(crux/entity (crux/db node) :m3)
;;=> {:crux.db/id :m3, :a 4, :b 2, :c 3, :d 5}

;; exemple 3

;; a transaction function that let you extend your document with new key (semantically similar to clojure's assoc)

(crux/submit-tx node
                [[:crux.tx/put {:crux.db/id :assoc
                                :crux.db/fn
                                ;; note that the function body is quoted.
                                '(fn [ctx eid & kvs]
                                   (let [db (crux.api/db ctx)
                                         entity (crux.api/entity db eid)]
                                     [[:crux.tx/put (apply assoc entity kvs)]]))}]])

(crux/submit-tx node
                [[:crux.tx/put {:crux.db/id :ivan, :age 40}]])

(crux/submit-tx node
                [[:crux.tx/fn :assoc :ivan :genre :M]])

(crux/entity (crux/db node) :ivan)
;;=> {:crux.db/id :ivan, :age 40, :genre :M}

;; speculative transactions
;; ---------------------------------------------------------------------------------------------------------------------

;; with the crux/with-tx function, we are creating an enriched database value without persisting anything to the system
(def speculative-db
  (crux/with-tx (crux/db node)
                [[:crux.tx/put {:crux.db/id :speculative-doc1 :value 42}]]))

;; we can chack that the added document does not exist in our real database

(crux/entity (crux/db node)
             :speculative-doc1)
;=> nil

;; and that it exists in our speculative db
(crux/entity speculative-db
             :speculative-doc1)
;=> {:crux.db/id :speculative-doc1, :value 42}

;; we can issue queries over our speculative db
(crux/q speculative-db
        '{:find [x]
          :where [[x :value 42]]})
;=> #{[:speculative-doc1]}

(+ 1 2)

(+ 3 4)

