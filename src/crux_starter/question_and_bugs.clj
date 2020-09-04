(ns crux-starter.question-and-bugs
  (:require [crux-starter.setup :refer [node]]
            [crux-starter.sugar :refer [puts q]]
            [crux.api :as crux]))

(comment
  :in-rule
  ;; i've got a simple nested document
  (crux/submit-tx node
                  [[:crux.tx/put
                    {:crux.db/id :nested1
                     :a {:b {:c 1}}}]])

  ;; manually retrieving the value at path [:a :b :c]
  (crux/q (crux/db node)
          '{:find [v]
            :where [[x :a xa]
                    [(get xa :b) xab]
                    [(get xab :c) v]]})

  ;; trying to generalize this with a 'in rule
  ;; but it throws:
  ;; Execution error (NullPointerException) at crux.query/build-logic-var-range-constraint-fns$iter$fn$fn (query.clj:568).
  (crux/q (crux/db node)
          '{:find [x y]
            :rules [[(in x p v) [(empty? p)] [(= x v)]]
                    [(in x p v) [(first p) fp] [(rest p) rp] [(get x fp) x'] (in x' rp v)]]
            :where [(in x [:a :b :c] y)]}))

(do :unification-question

    (crux/submit-tx node

                    [[:crux.tx/put
                      {:crux.db/id :v1
                       :val 4}]

                     [:crux.tx/put
                      {:crux.db/id :v2
                       :val 2}]])

    (comment
      ;; throws 'Circular dependency between vb and vb'
      (crux/q (crux/db node)
              '{:find [a b]
                :where [[a :val va]
                        [b :val vb]
                        [(/ va 2) vb]]}))

    ;; this works
    (crux/q (crux/db node)
            '{:find [a b]
              :where [[a :val va]
                      [b :val vb]
                      [(/ va 2) half-va]
                      [(= vb half-va)]]}))

(comment
  :and-semantics
  (crux/submit-tx node
                  [[:crux.tx/put
                    {:crux.db/id :ab
                     :a 1
                     :b 2}]])

  (crux/q (crux/db node)
          '{:find [x]
            :where [[x :a 1]
                    [x :b 2]]})

  (crux/q (crux/db node)
          '{:find [x]
            :where [(and [x :a 1]
                         [x :b 2])]}))

(do :ids
    (crux/submit-tx node [[:crux.tx/put {:crux.db/id :one :val 1}]])
    (crux/submit-tx node [[:crux.tx/put {:crux.db/id "one" :val 1}]])
    (crux/submit-tx node [[:crux.tx/put {:crux.db/id {:pouet 1} :val 1}]]))

(do :playing-with-numbers

    (crux/submit-tx node
                    (mapv (fn [n] [:crux.tx/put {:crux.db/id (keyword (str "number_" n)) :val n}])
                          (range 1 100)))

    (q '{:find [a b c]
         :where [[_ :val a]
                 [_ :val b]
                 [_ :val c]
                 [(+ a b) z]
                 [(= z c)]]})

    (q '{:find [a b]
         :where [[_ :val a]
                 [_ :val b]
                 [(not= 0 b)]
                 [(rem a b) r]
                 [(= r 0)]]}))

(comment
  :projections

  (crux/submit-tx node
                  [[:crux.tx/put
                    {:crux.db/id :bob
                     :name "Bob"
                     :genre :M
                     :age 34
                     :hobbies [:chess :programing]
                     :address {:street "12 main street"
                               :zip 75018
                               :details {:floor 2
                                         :digicode 123456}}}]

                   [:crux.tx/put
                    {:crux.db/id :mary
                     :name "Mary"
                     :genre :F
                     :age 23
                     :hobbies [:dancing :cooking]
                     :address {:street "16 false street"
                               :zip 75017
                               :details {:floor 1
                                         :digicode 654321}}}]])

  (q '{:find [(eql/project x [:name :age {:address [:street :zip]}])]
       :where [[x :genre :M]
               [x :address _]]}))

(comment
  :projections

  (crux/submit-tx node
                  [[:crux.tx/put
                    {:crux.db/id :one
                     :a true
                     :b {:c 2
                         :d 3
                         :e {:f 5
                             :g 6}}}]

                   [:crux.tx/put
                    {:crux.db/id :two
                     :a false
                     :b {:c 2
                         :d 3
                         :e {:f 5
                             :g 6}}}]])

  (q '{:find [(eql/project x [:a {:b [:c]}])]
       :where [[x :a true]]}))

(crux/submit-tx node
                [[:crux.tx/put {:crux.db/id :lawyer, :profession/name "Lawyer"}]
                 [:crux.tx/put {:crux.db/id :doctor , :profession/name "Doctor"}]
                 [:crux.tx/put {:crux.db/id :u1, :user/name "Ivan", :user/profession :doctor}],
                 [:crux.tx/put {:crux.db/id :u2, :user/name "Sergei", :user/profession :layer}]
                 [:crux.tx/put {:crux.db/id :u3, :user/name "Petr", :user/profession :doctor}]])

(crux/q (crux/db node)
        '{:find [(eql/project ?user [:user/name {:user/profession [:profession/name]}])]
          :where [[?user :user/name ?uid]]})

;; ordering

