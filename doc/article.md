# Crux 

Crux is a database developped by **[Juxt](https://juxt.pro/)**. It has been available as a public alpha since april 2019.

At first glance it looks a bit like an open sourced [Datomic](https://www.datomic.com/), but without schemas and with a slightly different temporal model.

## Bitemporality

While Datomic is indexing datums along a single time axis based on **transaction-time** (the point in time where data was transacted into the database), Crux uses a bitemporal approach, indexing datums along two axis:

- transaction time
- valid time

This extra time axis (valid-time) let the user populate the DB with past and future information regardless of the order in which the information arrives, and make corrections to past recordings to build an ever-improving temporal model of a given domain.

This kind of modeling takes into account the fact that our understanding of the past grows along the way. We do not know the exact state of the domain at each moment.

As an exemple, we can think of a criminal investigation

[Crux - Open Time Store](https://opencrux.com/docs#bitemp-crime)

## Schemaless

Crux does not enforce any schema for the documents it stores. One reason for this is that data might come from many different places, and may not ultimately be owned by the service using Crux to query the data. This design enables schema-on-write and/or schema-on-read to be achieved outside of the core of Crux, to meet the exact application requirements.

The only requirement for a crux document is to have a `:crux.db/id` key

## Datalog queries

Like Datomic, Crux uses datalog as a query language. in both systems Datalog queries are represented as [EDN](https://opencrux.com/tutorials/essential-edn.html) datastructures, but are not totally compatible.

Datalog is a non turing-complete subset of prolog

Unlike in Prolog,

- statements of a Datalog program can be **stated in any order**.
- Datalog queries on finite sets **are guaranteed to terminate**
- Datalog disallows complex terms as arguments of predicates, e.g., `p (1, 2)` is admissible but not `p(f (1), 2)`. It also is more restrictive about negation and recursion usage. [(wiki)](https://en.wikipedia.org/wiki/Datalog)

## Setup

In order to begin to play with Crux you only have to clone [this project](https://github.com/pbaille/crux-starter)

```shell
git clone git@github.com:pbaille/crux-starter.git
```

If you are not familiar with clojure you will find some instructions to setup an IDE in the [readme file](https://github.com/pbaille/crux-starter/blob/master/README.md)





## Transactions

``` clojure 
(ns crux-starter.p01_transactions
  (:require [crux.api :as crux]
            [crux-starter.p00_setup :refer [node]]))
```
### putting data into the database
crux valid documents are arbitrary nested edn maps
the only requirement is the presence of a `:crux.db/id` key pointing to either a keyword or a map
let's say that we have a clojure map that fulfill this requirement
``` clojure 
(def data1 {:crux.db/id :data1
            :myfield "mydata"})
```
we can transact it to the database like this
``` clojure 
(crux/submit-tx node
                [[:crux.tx/put data1]])
```
the simplest way to retrieve it is to use `crux/entity`
``` clojure 
(crux/entity (crux/db node) :data1)
;;=> {:crux.db/id :data1, :myfield "mydata"}
```
the `crux/db` call is returning the current value of our database
if we are interested in retrieving its value at a given time we can feed it a second argument
``` clojure 
(crux/db node #inst "2000") ;; returns the value of the database as in the beginning of the year 2000
```
as we can check our previously trasacted `:data1` document does not yet exists in 2000
``` clojure 
(crux/entity (crux/db node #inst "2000") :data1) ;;=> nil
```
`crux/submit-tx` can take several transactions
``` clojure 
(crux/submit-tx node
                [[:crux.tx/put {:crux.db/id :data2 :foo {:arbitrary {:nested "map"}}}]
                 [:crux.tx/put {:crux.db/id :data3 :data 3}]])
```
the `:crux.tx/put` operation is letting you specify the valid time frame of the given document
``` clojure 
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
```
### deleting (invalidating) documents
``` clojure 
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
```
like `:crux.tx.put`, `:crux.tx/delete` do not have to take valid-time starts and ends
if not the data will be deleted (invalidated) from now
``` clojure 
(crux/submit-tx node
                [[:crux.tx/delete :timed1]])

;; :timed1 is no longer valid
(crux/entity (crux/db node) :timed1)
;;=> nil

;; but is still valid in 2019
(crux/entity (crux/db node #inst "2019") :timed1)
;;=> {:crux.db/id :timed1, :value 10}
```
### eviction
``` clojure 
;; remove all historical versions of a document
(crux/submit-tx node
                [[:crux.tx/evict :one]])
```
### conditional transactions
one way to issue transaction only if certain condition is met is to use the `:crux.tx/match` operation
it let you verify the value of a database document against a given value
and issue some transactions only if those are equals
``` clojure 
(crux/submit-tx node
                [[:crux.tx/match
                  :data1 ;; we will check this entity against next provided arg
                  {:crux.db/id :data1 :myfield "mydata"} ;; the value we check the corresponding document against
                  ]

                 ;; if the match expression succeed we will transact the following forms
                 [:crux.tx/put
                  {:crux.db/id :data1
                   :myfield "mydata"
                   :foo :bar}]]) ;; <- we had an entry to our document

(crux/entity (crux/db node) :data1)
;;=> {:crux.db/id :data1, :myfield "mydata", :foo :bar}
```
like previously seen operations, `crux.db/match` can take a time at which to issue the matching
``` clojure 
(crux/submit-tx node
                [[:crux.tx/match
                  :data2
                  {:crux.db/id :data3 :data 3}
                  #inst "2019"] ;; the point in time where we do the check

                 ;; since in 2019, :data2 does not still exists, the belowing transaction is not executed
                 [:crux.tx/put
                  {:crux.db/id :data3
                   :never :occurs}]])

(crux/entity (crux/db node) :data3)
;;=> {:crux.db/id :data3, :data 3}


```
### modelling a simplistic bank account
``` clojure 
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
;=> {:crux.db/id :bank-account, :dollars 39}
```
### transaction functions
Transaction functions are user-supplied functions that run on the individual Crux nodes when a transaction is being ingested.
They can take any number of parameters, and return normal transaction operations which are then indexed as above.
If they return false or throw an exception, the whole transaction will roll back.
#### exemple 1
A transaction function that add (or substract) a given amount on our fancy `:bank-account` document.
transaction functions are defined with our old friend `crux.tx/put`
the given document has to have a `:crux.db/fn` key pointing to the function code (quoted)
``` clojure 
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
```
#### exemple 2
a transaction function that can create a new document by merging existing/given ones
``` clojure 
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
```
#### exemple 3
a transaction function that let you extend your document with new key (semantically similar to clojure's `assoc`)
``` clojure 
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
```
### speculative transactions
``` clojure 
;; with the `crux/with-tx` function, we are creating an enriched database value without persisting anything to the system
(def speculative-db
  (crux/with-tx (crux/db node)
                [[:crux.tx/put {:crux.db/id :speculative-doc1 :value 42}]]))
```
we can chack that the added document does not exist in our real database
``` clojure 
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
```



## Queries 

``` clojure 
(ns crux-starter.p02_queries
  (:require [crux.api :as crux]
            [crux-starter.p00_setup :refer [node]]
            [crux-starter.sugar :refer [puts q]]))
```
### data
putting some data to play with in the database
``` clojure 
(puts

  {:id :philippe
   :name "Philippe"
   :genre :M
   :age 70}

  {:id :odette
   :name "Odette"
   :genre :F
   :age 71}

  {:id :jean-pierre
   :name "Jean-Pierre"
   :genre :M
   :age 40
   :mother :odette
   :father :philippe}

  {:id :blandine
   :name "Blandine"
   :genre :F
   :age 43}

  {:id :blandine
   :name "Valerie"
   :genre :F
   :age 35}

  {:id :pierre
   :name "Pierre"
   :genre :M
   :age 20
   :father :jean-pierre
   :mother :blandine}

  {:id :clement
   :name "Clément"
   :genre :M
   :age 19
   :father :jean-pierre
   :mother :blandine}

  {:id :mathilde
   :name "Mathilde"
   :genre :F
   :age 12
   :father :jean-pierre
   :mother :valerie}

  {:id :nicolas
   :name "Nicolas"
   :genre :M
   :age 9
   :father :jean-pierre
   :mother :valerie}

  )
```
### basics
``` clojure 
;; attribute existence
;; find every documents that have a `:father` attribute
(q '{:find [x]
     :where [[x :father]]})
;=> #{[:clement] [:jean-pierre] [:mathilde] [:nicolas] [:pierre]}

;; finds all males
(q '{:find [p]
     :where [[p :genre :M]]})
;;=> #{[:clement] [:jean-pierre] [:nicolas] [:pierre]}

;; find sons of Valerie
(q '{:find [p]
     :where [[p :genre :M]
             [p :mother :valerie]]})
;;=> #{[:nicolas]}

;; names of blandine's children
(q '{:find [n]
     :where [[p :mother :blandine]
             [p :name n]]})
;;=> #{["Pierre"] ["Clément"]}

;; retrieve full entities
(q '{:find [p]
     :where [[p :genre :M]]
     :full-results? true})
```
### predicates
``` clojure 
;; finds all adults
(q '{:find [p]
     :where [[p :age a]
             ;; predicates expressions have to be wrapped in vector literal
             [(>= a 18)]]})

;; arithmetic constraints can take logic variables as arguments
(q '{:find [p q]
     :where [[p :age pa]
             [q :age qa]
             [(> pa qa)]]})

;; regular functions
(q '{:find [p half-age]
     :where [[p :age a]
             ;; we can catch the return value into a var
             [(/ a 2) half-age]]})

;; find couples of persons such that the first is two times older than the second
(q '{:find [p q]
     :where [[p :age pa]
             [q :age qa]
             [(/ pa 2) qa]]})
```
### logic connectors
``` clojure 
;; or
(q '{:find [x]
     :where [(or [x :name "Nicolas"]
                 [x :name "Mathilde"])]})

;; in this simple case we could use set literal
(q '{:find [x]
     :where [[x :name #{"Mathilde" "Nicolas"}]]})

;; nested `or` and `and`
(q '{:find [x age]
     :where [(or (and [x :name "Nicolas"] [x :age age])
                 (and [x :father :jean-pierre]
                      [x :age age]
                      (or [(= age 12)]
                          [(> age 19)])))]})
```
### rules
rules let you abstract clauses and create a more readable language for your queries
for instance we will create a `parent` rule wich describe a parent relationship between its two arguments
`(parent a b)` means that `a` is a `parent` of `b` (either father or mother)
``` clojure 
(q '{:find [a b]
     ;; introducing the parent rule
     :rules [[(parent b a) (or [a :mother b] [a :father b])]]
     ;; using it
     :where [(parent a b)]
     })
```
rules are also a great way to express traversal relationships
here we will define a `anccestor` rule
``` clojure 
(q '{:find [x]
     ;; we define ancestor in terms of parent
     :rules [[(parent p c) (or [c :mother p] [c :father p])]
             ;; ancestors have 2 cases
             [(ancestor a b) (parent a b)] ;; direct parent
             [(ancestor a b) (parent a pa) (ancestor fa b)] ;; transitive parent
             ]

     :where [(ancestor :odette x)]})
;;=> #{[:clement] [:jean-pierre] [:mathilde] [:nicolas] [:pierre]}
```
### Ordering and pagination
``` clojure 
(q '{:find [p age]
     :where [[p :age age]]
     :order-by [[age :asc]]})

#_[[:nicolas 9]
   [:mathilde 12]
   [:clement 19]
   [:pierre 20]
   [:blandine 35]
   [:jean-pierre 40]
   [:philippe 70]
   [:odette 71]]

(q '{:find [p age]
     :where [[p :age age]]
     :order-by [[age :asc]]
     :limit 4 ;; limits the number of results to 4
     :offset 2}) ;; starting at index 2

#_[[:clement 19]
   [:pierre 20]
   [:blandine 35]
   [:jean-pierre 40]]
```
### EQL projections
Crux queries support a 'projection' syntax,
allowing you to decouple specifying which entities you want from what data you’d like about those entities in your queries.
Crux’s support is based on the excellent EDN Query Language (EQL) library.
``` clojure 
(puts
  {:crux.db/id :lawyer, :profession/name "Lawyer"}
  {:crux.db/id :doctor, :profession/name "Doctor"}
  {:crux.db/id :u1, :user/name "Ivan", :user/profession :doctor},
  {:crux.db/id :u2, :user/name "Sergei", :user/profession :lawyer}
  {:crux.db/id :u3, :user/name "Petr", :user/profession :doctor})

(q '{:find [(eql/project ?user [:user/name {:user/profession [:profession/name]}])]
     :where [[?user :user/name ?uid]]})
```

## Conclusion 

In this article we've got a brief overview of crux main ideas and API, many things remains to be seen, 
like the infrastructure and deployment parts. I hope to be able to cover this in further articles, so stay tuned! 