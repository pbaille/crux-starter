(ns crux-starter.queries
  (:require [crux.api :as crux]
            [crux-starter.setup :refer [node]]
            [crux-starter.sugar :refer [puts q]]))

;; data
;; ---------------------------------------------------------------------------------------------------------------------

;; putting some data to play with in the database

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

;; basics
;; ---------------------------------------------------------------------------------------------------------------------

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

;; predicates
;; ---------------------------------------------------------------------------------------------------------------------

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

;; logic connectors
;; ---------------------------------------------------------------------------------------------------------------------

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

;; rules
;; ---------------------------------------------------------------------------------------------------------------------

;; rules let you abstract clauses and create a more readable language for your queries
;; for instance we will create a 'parent rule wich describe a parent relationship between its two arguments
;; (parent a b) means that a is a parent of b (either father or mother)

(q '{:find [a b]
     ;; introducing the parent rule
     :rules [[(parent b a) (or [a :mother b] [a :father b])]]
     ;; using it
     :where [(parent a b)]
     })

;; rules are also a great way to express traversal relationships
;; here we will define a 'anccestor rule

(q '{:find [x]
     ;; we define ancestor in terms of parent
     :rules [[(parent p c) (or [c :mother p] [c :father p])]
             ;; ancestors have 2 cases
             [(ancestor a b) (parent a b)] ;; direct parent
             [(ancestor a b) (parent a pa) (ancestor fa b)] ;; transitive parent
             ]

     :where [(ancestor :odette x)]})
;;=> #{[:clement] [:jean-pierre] [:mathilde] [:nicolas] [:pierre]}

;; Ordering and pagination
;; ---------------------------------------------------------------------------------------------------------------------

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

;; EQL projections
;; ---------------------------------------------------------------------------------------------------------------------

;; Crux queries support a 'projection' syntax,
;; allowing you to decouple specifying which entities you want from what data you’d like about those entities in your queries.
;; Crux’s support is based on the excellent EDN Query Language (EQL) library.

(puts
  {:crux.db/id :lawyer, :profession/name "Lawyer"}
  {:crux.db/id :doctor, :profession/name "Doctor"}
  {:crux.db/id :u1, :user/name "Ivan", :user/profession :doctor},
  {:crux.db/id :u2, :user/name "Sergei", :user/profession :lawyer}
  {:crux.db/id :u3, :user/name "Petr", :user/profession :doctor})

(q '{:find [(eql/project ?user [:user/name {:user/profession [:profession/name]}])]
     :where [[?user :user/name ?uid]]})