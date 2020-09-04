``` clojure 
(ns crux-starter.setup
  (:require [crux.api :as crux]))
```
this setup is for playing only
;; it does not persist anything
``` clojure 
(def node
  (crux/start-node
    {:crux.node/topology '[crux.standalone/topology]
     :crux.kv/db-dir "data/db-dir"}))
```