(ns crux-starter.sugar
  (:require [crux.api :as crux]
            [crux-starter.p00_setup :refer [node]]))

(defn puts
  "transact several puts on node"
  [& xs]
  (crux/submit-tx node
                  (mapv (fn [x]
                          (let [id (or (:crux.db/id x) (:id x) (throw (Exception. "no id in " x)))]
                            [:crux.tx/put (assoc (dissoc x :id) :crux.db/id id)]))
                        xs)))

(defn q
  "query node shorthand"
  ([x] (crux/q (crux/db node) x))
  ([at x] (crux/q (crux/db node at) x)))