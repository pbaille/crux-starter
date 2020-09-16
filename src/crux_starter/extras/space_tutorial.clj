(ns crux-starter.extras.space-tutorial
  (:require [crux.api :as crux]
            [clojure.java.io :as io]))

;; step 1 --------------------------------------------------------------------------------------------------------------

(defn start-standalone-node ^crux.api.ICruxAPI [storage-dir]
  (crux/start-node {:crux.node/topology '[crux.standalone/topology]
                    :crux.kv/db-dir (str (io/file storage-dir "db"))}))

(def node (start-standalone-node "crux-store"))

(def manifest
  {:crux.db/id :manifest
   :pilot-name "Johanna"
   :id/rocket "SB002-sol"
   :id/employee "22910x2"
   :badges "SETUP"
   :cargo ["stereo" "gold fish" "slippers" "secret note"]})

;; put the manifest into crux
(crux/submit-tx node [[:crux.tx/put manifest]])

;; returns the history of the provided entity id at this point in time.
(crux/entity-history (crux/db node) :manifest :asc)

;; step 2 --------------------------------------------------------------------------------------------------------------

;; https://juxt.pro/blog/crux-tutorial-put#continue-pluto

(crux/submit-tx node
                [[:crux.tx/put
                  {:crux.db/id :commodity/Pu
                   :common-name "Plutonium"
                   :type :element/metal
                   :density 19.816
                   :radioactive true}]

                 [:crux.tx/put
                  {:crux.db/id :commodity/N
                   :common-name "Nitrogen"
                   :type :element/gas
                   :density 1.2506
                   :radioactive false}]

                 [:crux.tx/put
                  {:crux.db/id :commodity/CH4
                   :common-name "Methane"
                   :type :molecule/gas
                   :density 0.717
                   :radioactive false}]])

(crux/submit-tx node
                [[:crux.tx/put
                  {:crux.db/id :stock/Pu
                   :commod :commodity/Pu
                   :weight-ton 21}
                  #inst "2115-02-13T18"] ;; valid-time

                 [:crux.tx/put
                  {:crux.db/id :stock/Pu
                   :commod :commodity/Pu
                   :weight-ton 23}
                  #inst "2115-02-14T18"]

                 [:crux.tx/put
                  {:crux.db/id :stock/Pu
                   :commod :commodity/Pu
                   :weight-ton 22.2}
                  #inst "2115-02-15T18"]

                 [:crux.tx/put
                  {:crux.db/id :stock/Pu
                   :commod :commodity/Pu
                   :weight-ton 24}
                  #inst "2115-02-18T18"]

                 [:crux.tx/put
                  {:crux.db/id :stock/Pu
                   :commod :commodity/Pu
                   :weight-ton 24.9}
                  #inst "2115-02-19T18"]])

(crux/submit-tx node
                [[:crux.tx/put
                  {:crux.db/id :stock/N
                   :commod :commodity/N
                   :weight-ton 3}
                  #inst "2115-02-13T18" ;; start valid-time
                  #inst "2115-02-19T18"] ;; end valid-time

                 [:crux.tx/put
                  {:crux.db/id :stock/CH4
                   :commod :commodity/CH4
                   :weight-ton 92}
                  #inst "2115-02-15T18"
                  #inst "2115-02-19T18"]])

(crux/entity (crux/db node #inst "2115-02-14") :stock/Pu)
;;=> {:crux.db/id :stock/Pu, :commod :commodity/Pu, :weight-ton 21}

(crux/entity (crux/db node #inst "2115-02-18") :stock/Pu)
;;=> {:crux.db/id :stock/Pu, :commod :commodity/Pu, :weight-ton 22.2}

(defn easy-ingest
  "Uses Crux put transaction to add a vector of documents to a specified
  node"
  [node docs]
  (crux/submit-tx node (mapv (fn [doc] [:crux.tx/put doc]) docs)))

(crux/submit-tx
  node
  [[:crux.tx/put
    (assoc manifest :badges ["SETUP" "PUT"])]])
;;=> #:crux.tx{:tx-id 3, :tx-time #inst "2020-06-18T14:20:31.602-00:00"}

(crux/entity (crux/db node) :manifest)
;;=> {:crux.db/id :manifest,
;;    :pilot-name "Johanna",
;;    :id/rocket "SB002-sol",
;;    :id/employee "22910x2",
;;    :badges ["SETUP" "PUT"],
;;    :cargo ["stereo" "gold fish" "slippers" "secret note"]}

;; step 3 --------------------------------------------------------------------------------------------------------------

(easy-ingest node
             [{:crux.db/id :commodity/Pu
               :common-name "Plutonium"
               :type :element/metal
               :density 19.816
               :radioactive true}

              {:crux.db/id :commodity/N
               :common-name "Nitrogen"
               :type :element/gas
               :density 1.2506
               :radioactive false}

              {:crux.db/id :commodity/CH4
               :common-name "Methane"
               :type :molecule/gas
               :density 0.717
               :radioactive false}

              {:crux.db/id :commodity/Au
               :common-name "Gold"
               :type :element/metal
               :density 19.300
               :radioactive false}

              {:crux.db/id :commodity/C
               :common-name "Carbon"
               :type :element/non-metal
               :density 2.267
               :radioactive false}

              {:crux.db/id :commodity/borax
               :common-name "Borax"
               :IUPAC-name "Sodium tetraborate decahydrate"
               :other-names ["Borax decahydrate" "sodium borate"
                             "sodium tetraborate" "disodium tetraborate"]
               :type :mineral/solid
               :appearance "white solid"
               :density 1.73
               :radioactive false}])

;; basic

(crux/q (crux/db node)
        '{:find [element]
          :where [[element :type :element/metal]]})

;; quoting

(=
  (crux/q (crux/db node)
          '{:find [element]
            :where [[element :type :element/metal]]})

  (crux/q (crux/db node)
          {:find '[element]
           :where '[[element :type :element/metal]]})

  (crux/q (crux/db node)
          (quote
            {:find [element]
             :where [[element :type :element/metal]]})))

;; more infos

(crux/q (crux/db node)
        '{:find [name]
          :where [[e :type :element/metal]
                  [e :common-name name]]})

(crux/q (crux/db node)
        '{:find [name rho]
          :where [[e :density rho]
                  [e :common-name name]]})

;; arguments

(crux/q (crux/db node)
        {:find '[name]
         :where '[[e :type t]
                  [e :common-name name]]
         :args [{'t :element/metal}]}) ;; <<- here

(defn add-badges [node bs]
  (crux/submit-tx node
                  [[:crux.tx/put
                    (apply update (crux/entity (crux/db node) :manifest)
                           :badges conj bs)]]))

; (add-badges node ["DATALOG-QUERIES"])

(crux/entity (crux/db node) :manifest)

;; step 4 --------------------------------------------------------------------------------------------------------------

(crux/submit-tx
  node
  [[:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Full}
    #inst "2114-12-03"]])

(crux/submit-tx
  node
  [[:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Full}
    #inst "2113-12-03" ;; Valid time start
    #inst "2114-12-03"] ;; Valid time end

   [:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Full}
    #inst "2112-12-03"
    #inst "2113-12-03"]

   [:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? false}
    #inst "2112-06-03"
    #inst "2112-12-02"]

   [:crux.tx/put
    {:crux.db/id :consumer/RJ29sUU
     :consumer-id :RJ29sUU
     :first-name "Jay"
     :last-name "Rose"
     :cover? true
     :cover-type :Promotional}
    #inst "2111-06-03"
    #inst "2112-06-03"]])

;; querying through time

(crux/q (crux/db node #inst "2115-07-03")
        '{:find [cover type]
          :where [[e :consumer-id :RJ29sUU]
                  [e :cover? cover]
                  [e :cover-type type]]})

(crux/q (crux/db node #inst "2111-07-03")
        '{:find [cover type]
          :where [[e :consumer-id :RJ29sUU]
                  [e :cover? cover]
                  [e :cover-type type]]})

(crux/q (crux/db node #inst "2112-07-03")
        '{:find [cover type]
          :where [[e :consumer-id :RJ29sUU]
                  [e :cover? cover]
                  [e :cover-type type]]})

(crux/submit-tx
  node [[:crux.tx/put
         (assoc manifest
           :badges ["SETUP" "PUT" "DATALOG-QUERIES" "BITEMP"])]])

;; step 5 --------------------------------------------------------------------------------------------------------------

(easy-ingest
  node
  [{:crux.db/id :gold-harmony
    :company-name "Gold Harmony"
    :seller? true
    :buyer? false
    :units/Au 10211
    :credits 51}

   {:crux.db/id :tombaugh-resources
    :company-name "Tombaugh Resources Ltd."
    :seller? true
    :buyer? false
    :units/Pu 50
    :units/N 3
    :units/CH4 92
    :credits 51}

   {:crux.db/id :encompass-trade
    :company-name "Encompass Trade"
    :seller? true
    :buyer? true
    :units/Au 10
    :units/Pu 5
    :units/CH4 211
    :credits 1002}

   {:crux.db/id :blue-energy
    :seller? false
    :buyer? true
    :company-name "Blue Energy"
    :credits 1000}])

(defn stock-check
  [company-id item]
  {:result (crux/q (crux/db node)
                   {:find '[name funds stock]
                    :where ['[e :company-name name]
                            '[e :credits funds]
                            ['e item 'stock]]
                    :args [{'e company-id}]})
   :item item})

(defn format-stock-check
  [{:keys [result item] :as stock-check}]
  (for [[name funds commod] result]
    (str "Name: " name ", Funds: " funds ", " item " " commod)))

(crux/submit-tx node
                [;; putting a new document in
                 [:crux.tx/put {:crux.db/id :moi :up true}]
                 ;; if this line succeed we continue
                 [:crux.tx/match :moi {:crux.db/id :moi :up true}]
                 ;; doing this
                 [:crux.tx/put {:crux.db/id :moi :up true :yo 42}]])

(crux/entity (crux/db node) :moi)

(crux/submit-tx
  node [[:crux.tx/put
         (assoc manifest
           :badges ["SETUP" "PUT" "DATALOG-QUERIES" "BITEMP" "MATCH"])]])

;; step 6 --------------------------------------------------------------------------------------------------------------

(crux/submit-tx
  node [[:crux.tx/put {:crux.db/id :kaarlang/clients
                       :clients [:encompass-trade]}
         #inst "2110-01-01T09"
         #inst "2111-01-01T09"]

        [:crux.tx/put {:crux.db/id :kaarlang/clients
                       :clients [:encompass-trade :blue-energy]}
         #inst "2111-01-01T09"
         #inst "2113-01-01T09"]

        [:crux.tx/put {:crux.db/id :kaarlang/clients
                       :clients [:blue-energy]}
         #inst "2113-01-01T09"
         #inst "2114-01-01T09"]

        [:crux.tx/put {:crux.db/id :kaarlang/clients
                       :clients [:blue-energy :gold-harmony :tombaugh-resources]}
         #inst "2114-01-01T09"
         #inst "2115-01-01T09"]])
;;=> #:crux.tx{:tx-id 0, :tx-time #inst "2020-06-18T15:55:00.894-00:00"}

(crux/entity-history
  (crux/db node #inst "2116-01-01T09")
  :kaarlang/clients
  :desc
  {:with-docs? true})

;; deleting
(crux/submit-tx
  node [[:crux.tx/delete :kaarlang/clients #inst "2110-01-01" #inst "2116-01-01"]])
;;=> #:crux.tx{:tx-id 1, :tx-time #inst "2020-06-18T15:59:38.323-00:00"}

(crux/entity-history
  (crux/db node #inst "2116-01-01T09")
  :kaarlang/clients
  :desc
  {:with-docs? true})

;; step 7 --------------------------------------------------------------------------------------------------------------

(crux/submit-tx node
                [[:crux.tx/put
                  {:crux.db/id :person/kaarlang
                   :full-name "Kaarlang"
                   :origin-planet "Mars"
                   :identity-tag :KA01299242093
                   :DOB #inst "2040-11-23"}]

                 [:crux.tx/put
                  {:crux.db/id :person/ilex
                   :full-name "Ilex Jefferson"
                   :origin-planet "Venus"
                   :identity-tag :IJ01222212454
                   :DOB #inst "2061-02-17"}]

                 [:crux.tx/put
                  {:crux.db/id :person/thadd
                   :full-name "Thad Christover"
                   :origin-moon "Titan"
                   :identity-tag :IJ01222212454
                   :DOB #inst "2101-01-01"}]

                 [:crux.tx/put
                  {:crux.db/id :person/johanna
                   :full-name "Johanna"
                   :origin-planet "Earth"
                   :identity-tag :JA012992129120
                   :DOB #inst "2090-12-07"}]])

(defn full-query
  [node]
  (crux/q
    (crux/db node)
    '{:find [id]
      :where [[e :crux.db/id id]]
      :full-results? true}))

(full-query node)

;; evict

(crux/submit-tx node [[:crux.tx/evict :person/kaarlang]])

(crux/entity-history
  (crux/db node)
  :person/kaarlang
  :desc
  {:with-docs? true})
#_#_
    => [{:crux.tx/tx-time #inst "2020-06-18T16:11:03.410-00:00",
         :crux.tx/tx-id 2,
         :crux.db/valid-time #inst "2020-06-18T16:11:03.410-00:00",
         :crux.db/content-hash #crux/id "c3ad3191fff06083fedf3640b625566c02033a6b",
         :crux.db/doc
         #:crux.db{:id #crux/id "efe634523d6867a3c6e4089074adf29b07b45f43",
                   :evicted? true}}]