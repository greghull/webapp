(ns webapp.helpers.titles 
  (:require
    [clojure.string :as str]))

(defmulti title-for #(keyword (:meta/type %)))

(defmethod title-for :default [doc]
  (str (str/capitalize (name (or (:meta/type doc) "Untyped")))
       " Document"))

(defmethod title-for :user [doc]
  (str (:user/first-name doc) " " (:user/last-name doc)))
