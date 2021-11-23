(ns webapp.views.helpers)

(defmulti title-for #(:meta/type %))

(defmethod title-for :default [doc]
  (str (clojure.string/capitalize (name (or (:meta/type doc) "Untyped")))
       " Document"))