(ns crux-starter.p00_setup
  (:require [crux.api :as crux]))

;; this setup is for playing only
;; it does not persist anything

(def node
  (crux/start-node
    {:crux.node/topology '[crux.standalone/topology]
     :crux.kv/db-dir "data/db-dir"}))



