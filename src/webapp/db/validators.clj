(ns webapp.db.validators
  (:require [webapp.db.core :as db]))

(def does-not-exist
  {:message "already exists"
   :optional true
   :validate #(not (db/exists? %2 %1))})

(def unique-to
  {:message "is not unique"
   :optional true
   :state true
   :validate (fn [state val id field]
               (let [doc (first (db/query [:= field val]))
                     id (get state id)]
                 (if (nil? doc)
                   true
                   (= id (:meta/id doc)))))})